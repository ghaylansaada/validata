/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.has
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.PropertyModel
import io.ghaylan.validata.processor.model.SchemaModel
import io.ghaylan.validata.processor.model.ShapeModel
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming

/**
 * Builds a [SchemaModel] for one `@Validatable` [KSClassDeclaration].
 *
 * ## Concrete vs polymorphic
 * | Kind | Result |
 * |---|---|
 * | Ordinary data/class | [SchemaModel.properties] filled; [SchemaModel.subtypeQualifiedNames] empty |
 * | Interface / abstract / sealed root | properties empty; subtypes listed for runtime dispatch |
 *
 * ## Naming & readers
 * - [PropertyModel.declaredName] — Kotlin property name (cross-field refs)
 * - [PropertyModel.externalName] — `@JsonProperty` value, else [jacksonNaming] on the declared name
 * - [PropertyModel.readerExpr] — `{ (it as Owner).prop }` source text for codegen
 *
 * ## Cycles
 * `visiting` tracks FQCNs currently under construction. Re-entering a type returns a **shell**
 * schema (empty properties). Real object refs use `lazy { schema_…() }` in generated code, so
 * recursion terminates at runtime without truncating the graph.
 *
 * @property logger empty-schema warnings and related diagnostics
 * @property constraints property-level constraint extraction
 * @property shapes type → [ShapeModel] conversion
 * @property subtypeReconciler unions sealed leaves with `@Validatable(subtypes=…)` for polymorphic roots
 * @property errorDocs parses `@ApiError` markers on schema properties
 * @property jacksonNaming wire naming when `@JsonProperty` is absent
 *
 * @author Ghaylan Saada
 */
internal class SchemaModelBuilder(
	private val logger: KSPLogger,
	private val constraints: ConstraintModelBuilder,
	private val shapes: ShapeModelBuilder,
	private val subtypeReconciler: PolymorphicSubtypeReconciler = PolymorphicSubtypeReconciler(logger),
	private val errorDocs: ApiErrorAnnotationParser,
	private val jacksonNaming: JacksonPropertyNaming = JacksonPropertyNaming.IDENTITY,
) {

	/**
	 * Analyses [clazz] into an intermediate schema, or skips it.
	 *
	 * Mutates [visiting] for cycle detection; may emit KSP warnings via [logger].
	 *
	 * @param clazz a class annotated with `@Validatable`
	 * @param resolver unused today (kept for call-site stability / future nested resolution)
	 * @param visiting mutable set of FQCNs currently being built (cycle detection)
	 * @return the model, a cycle shell, or `null` when a concrete type has no readable properties
	 */
	fun buildSchema(
		clazz: KSClassDeclaration,
		resolver: Resolver,
		visiting: MutableSet<String>,
	): SchemaModel? {
		val qName = clazz.qualifiedName?.asString() ?: return null

		// Already visiting this type → cycle. Emit a shell; object-ref laziness breaks the loop.
		if (!visiting.add(qName)) {
			return SchemaModel(
				packageName = clazz.packageName.asString(),
				simpleName = clazz.simpleName.asString(),
				qualifiedName = qName,
				properties = emptyList(),
				isPolymorphicRoot = isPolymorphic(clazz),
				subtypeQualifiedNames = emptyList())
		}

		try {
			val polymorphic = isPolymorphic(clazz)
			// Always reconcile / validate discriminator + Subtype entries (even on concrete DTOs);
			// only polymorphic roots keep the collected subtype list on the schema model.
			val resolvedSubtypes = resolveSubtypes(clazz)
			val subtypes = if (polymorphic) resolvedSubtypes else emptyList()
			val properties = if (polymorphic) {
				// Roots only dispatch; concrete subtypes own the properties.
				emptyList()
			} else {
				clazz.getAllProperties()
					.mapNotNull { prop -> buildProperty(clazz, prop, visiting) }
					.toList()
			}

			if (!polymorphic && properties.isEmpty()) {
				logger.warn(
					"@Validatable type '$qName' has no readable properties; skipping schema emission.",
					clazz)
				return null
			}

			return SchemaModel(
				packageName = clazz.packageName.asString(),
				simpleName = clazz.simpleName.asString(),
				qualifiedName = qName,
				properties = properties,
				isPolymorphicRoot = polymorphic,
				subtypeQualifiedNames = subtypes)
		} finally {
			visiting.remove(qName)
		}
	}

	/**
	 * Builds one [PropertyModel], or returns `null` when the property is `@JsonIgnore`.
	 *
	 * Combines property-level constraints, shape (including type-use constraints on the property
	 * type reference), Kotlin vs Jackson naming, and a cast reader lambda for codegen.
	 *
	 * `@NoCascade` on the property (or its overridee) forces opaque treatment of nested objects.
	 *
	 * May emit KSP errors via [logger] when the owner lacks a qualified name.
	 *
	 * @param owner declaring `@Validatable` class
	 * @param property property under [owner]
	 * @param visiting cycle set shared with schema construction
	 * @return [PropertyModel], or `null` when the property is `@JsonIgnore`
	 */
	private fun buildProperty(
		owner: KSClassDeclaration,
		property: KSPropertyDeclaration,
		visiting: MutableSet<String>,
	): PropertyModel? {
		val name = property.simpleName.asString()

		// Jackson: omit from schema entirely (binding + validation).
		if (property.annotations.any {
				it.shortName.asString() == "JsonIgnore" &&
					AnnotationFqcn.isA(it, ProcessorFqns.JSON_IGNORE)
			}) {
			return null
		}

		val noCascade = property.has(ProcessorFqns.NO_CASCADE) ||
			property.findOverridee()?.has(ProcessorFqns.NO_CASCADE) == true
		val propertyType = property.type.resolve()
		val propConstraints = constraints.resolveConstraints(owner, property, propertyType)
		val shape = shapes.buildShape(
			type = propertyType,
			typeUseAnnotations = property.type.annotations.toList(),
			owner = owner,
			property = property,
			visiting = visiting,
			cascadePolicy = if (noCascade) CascadePolicy.NO_CASCADE else CascadePolicy.CASCADE,
		)

		val ownerQ = owner.qualifiedName?.asString()
		if (ownerQ == null) {
			logger.error(
				"Cannot build property '$name': declaring type has no qualified name.",
				owner,
			)
			return null
		}
		return PropertyModel(
			declaredName = name,
			externalName = ExternalPropertyNames.resolve(property, jacksonNaming),
			// Cast in the lambda: generated ValueReader receives Any? at runtime.
			readerExpr = "{ (it as $ownerQ).$name }",
			shape = shape,
			constraints = propConstraints,
			noCascade = noCascade,
			errorDocs = errorDocs.parseMemberErrorDocs(property),
		)
	}

	/**
	 * Collects concrete subtype FQCNs and validates `@Validatable` polymorphism metadata via
	 * [PolymorphicSubtypeReconciler].
	 *
	 * Sources (framework-owned only — no Jackson):
	 * 1. Sealed hierarchy (`getSealedSubclasses`)
	 * 2. Explicit `@Validatable(subtypes = [Subtype(...)])` entries
	 *
	 * Also enforces non-blank `discriminator` naming a property on [clazz], and each declared
	 * subtype extending / implementing [clazz].
	 *
	 * No side effects beyond [subtypeReconciler] diagnostics.
	 *
	 * @param clazz `@Validatable` class being analyzed
	 * @return concrete subtype FQCNs for polymorphic roots
	 */
	private fun resolveSubtypes(clazz: KSClassDeclaration): List<String> {
		val validatable = clazz.annotations.firstOrNull {
			AnnotationFqcn.isA(it, ProcessorFqns.VALIDATABLE)
		}
		return subtypeReconciler.resolve(clazz, validatable)
	}

	/**
	 * Whether [clazz] is a polymorphic schema root (no own properties; subtypes only).
	 *
	 * Sealed **classes** that are concrete keep properties; sealed interfaces / abstracts do not.
	 *
	 * No side effects.
	 *
	 * @param clazz `@Validatable` class candidate
	 * @return `true` for interface, abstract, or non-class sealed roots
	 */
	private fun isPolymorphic(clazz: KSClassDeclaration): Boolean =
		clazz.classKind == ClassKind.INTERFACE ||
			clazz.isAbstract() ||
			(clazz.modifiers.contains(Modifier.SEALED) && clazz.classKind != ClassKind.CLASS)

}
