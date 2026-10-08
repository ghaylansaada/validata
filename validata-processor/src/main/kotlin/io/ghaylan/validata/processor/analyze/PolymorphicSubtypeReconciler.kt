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
import com.google.devtools.ksp.symbol.*
import io.ghaylan.validata.processor.compat.has
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.verify.ConstraintLiteralVerifier

/**
 * Unions sealed-subclass and `@Validatable(subtypes = …)` sources for a polymorphic root and
 * fails the build when the two disagree.
 *
 * Also verifies authoring rules on every [resolve] call (including concrete `@Validatable` types,
 * so callers should invoke [resolve] for all annotated classes and ignore the list when the
 * type is not a polymorphic root):
 * - Non-blank `discriminator` must name a **scalar** Kotlin property on the annotated type
 *   (string / number / temporal / enum — not a collection or nested object)
 * - Each `@Validatable.Subtype(type = …)` must extend or implement the annotated parent
 * - `@Validatable.Subtype(name = …)` must be a valid typed literal for the discriminator
 *   property’s type (enum constant, number string, temporal, or free string — same rules as
 *   `@ConstraintArg(TYPED_LITERAL)`)
 *
 * The framework deliberately does **not** read host-serializer subtype annotations.
 * Authors declare subtypes on `Validatable` itself (validata-core library — not on this
 * processor’s compile classpath).
 *
 * @property logger emits KSP errors when sealed leaves and declared subtypes disagree, when a
 *   polymorphic root has no concrete children, when the discriminator is missing / non-scalar,
 *   or when a declared subtype is not assignable to the parent*
 * 
 * @author Ghaylan Saada
 */
internal class PolymorphicSubtypeReconciler(
	private val logger: KSPLogger,
) {

	private val discriminatorRules = DiscriminatorRules(logger)

	/**
	 * Collects concrete subtype FQCNs for [clazz] and validates polymorphism metadata.
	 *
	 * Always runs discriminator + declared-subtype checks. Preference when both sealed
	 * discovery and `@Validatable(subtypes=…)` are present: sealed discovery order, after
	 * [reconcile] proves the two sets match.
	 *
	 * @param clazz `@Validatable` type (polymorphic root or concrete DTO)
	 * @param validatableAnn the `@Validatable` usage on [clazz], or `null` if absent
	 * @return deduplicated concrete subtype qualified names in discovery order (empty for
	 *   ordinary concrete DTOs with no `subtypes` / sealed children)
	 */
	fun resolve(
		clazz: KSClassDeclaration,
		validatableAnn: KSAnnotation?,
	): List<String> {
		val propertyIndex = DiscriminatorRules.propertyIndex(clazz)
		discriminatorRules.verifyDiscriminator(clazz, validatableAnn, propertyIndex)

		val sealedConcrete = LinkedHashSet<String>()
		if (clazz.modifiers.contains(Modifier.SEALED)) {
			collectSealedLeaves(clazz, sealedConcrete)
		}

		val declaredConcrete = LinkedHashSet<String>()
		val declaredNames = LinkedHashMap<String, String>() // typeFqcn → discriminator name
		readDeclaredSubtypes(clazz, validatableAnn, declaredConcrete, declaredNames, propertyIndex)
		when {
			sealedConcrete.isEmpty() && declaredConcrete.isEmpty() -> {
				if (isPolymorphicRoot(clazz)) {
					logger.error(
						"Polymorphic @Validatable '${clazz.qualifiedName?.asString()}' has no concrete subtypes. " +
							"Use a sealed hierarchy or @Validatable(subtypes = [Subtype(...)])).",
						clazz,
					)
				}
				return emptyList()
			}
			sealedConcrete.isEmpty() -> return declaredConcrete.toList()
			declaredConcrete.isEmpty() -> return sealedConcrete.toList()
			else -> {
				reconcile(clazz, sealedConcrete, declaredConcrete)
				// Prefer sealed discovery order, then any extras already rejected by reconcile.
				return sealedConcrete.toList()
			}
		}
	}

	/**
	 * Expands sealed children recursively until only concrete classes remain (skips abstract /
	 * interface sealed nodes by descending further).
	 */
	private fun collectSealedLeaves(clazz: KSClassDeclaration, into: MutableSet<String>) {
		clazz.getSealedSubclasses().forEach { sub ->
			if (isConcrete(sub)) {
				val fqcn = sub.qualifiedName?.asString()
				if (fqcn == null) {
					logger.error(
						"Sealed subclass of '${clazz.qualifiedName?.asString() ?: clazz.simpleName.asString()}' " +
							"has no qualified name and cannot be registered as a polymorphic subtype.",
						sub,
					)
				} else {
					into += fqcn
				}
			} else {
				collectSealedLeaves(sub, into)
			}
		}
	}

	/**
	 * Reads `@Validatable(subtypes = [Subtype(name=…, type=…), …])`.
	 *
	 * Each subtype type must itself be `@Validatable` **and** be assignable to [owner]
	 * (extend / implement the annotated parent). When a scalar discriminator property type is
	 * known, `name` is checked with the same typed-literal rules as
	 * `@ConstraintArg(TYPED_LITERAL)` ([ConstraintLiteralVerifier.typedLiteralError]).
	 *
	 * Concrete classes are added to [into]; sealed types are expanded via [collectSealedLeaves].
	 * Discriminator [names] are recorded for future emission but are not returned by [resolve]
	 * today (runtime uses type identity).
	 *
	 * @param names filled with `typeFqcn → discriminator name` when `name` is non-blank
	 */
	private fun readDeclaredSubtypes(
		owner: KSClassDeclaration,
		validatableAnn: KSAnnotation?,
		into: MutableSet<String>,
		names: MutableMap<String, String>,
		propertyIndex: Map<String, KSPropertyDeclaration>,
	) {
		if (validatableAnn == null) return
		val subtypesArg = validatableAnn.arguments.firstOrNull { it.name?.asString() == "subtypes" }
			?: return
		@Suppress("UNCHECKED_CAST")
		val nested = subtypesArg.value as? List<*> ?: return
		val ownerType = owner.asStarProjectedType()
		val discType = discriminatorRules.discriminatorPropertyType(owner, validatableAnn, propertyIndex)
		for (item in nested) {
			val ann = item as? KSAnnotation ?: continue
			val nameValue = ann.arguments.firstOrNull { it.name?.asString() == "name" }?.value as? String
			val typeValue = ann.arguments.firstOrNull { it.name?.asString() == "type" }?.value as? KSType
			if (typeValue == null) {
				logger.error(
					"@Validatable.Subtype on '${owner.qualifiedName?.asString()}' has an unresolvable type.",
					owner,
				)
				continue
			}
			if (discType != null && nameValue != null) {
				val err = ConstraintLiteralVerifier.typedLiteralError(nameValue, discType)
				if (err != null) {
					logger.error(
						"@Validatable.Subtype name on '${owner.qualifiedName?.asString()}' $err",
						ann,
					)
				}
			}
			acceptSubtypeType(owner, ownerType, ann, typeValue, nameValue, into, names)
		}
	}

	/**
	 * Validates and registers one `@Validatable.Subtype` target type.
	 *
	 * May emit KSP errors via [logger]; mutates [into] and [names] on success.
	 *
	 * @param owner annotated polymorphic parent
	 * @param ownerType star-projected type of [owner] for assignability checks
	 * @param ann `@Validatable.Subtype` nested annotation
	 * @param typeValue resolved subtype class type
	 * @param nameValue optional discriminator name literal
	 * @param into accumulator for concrete subtype FQCNs
	 * @param names accumulator mapping subtype FQCN to discriminator name
	 */
	private fun acceptSubtypeType(
		owner: KSClassDeclaration,
		ownerType: KSType,
		ann: KSAnnotation,
		typeValue: KSType,
		nameValue: String?,
		into: MutableSet<String>,
		names: MutableMap<String, String>,
	) {
		val decl = typeValue.declaration as? KSClassDeclaration
		if (decl == null) {
			logger.error(
				"@Validatable.Subtype on '${owner.qualifiedName?.asString()}' has an unresolvable type.",
				owner,
			)
			return
		}
		val fqcn = decl.qualifiedName?.asString() ?: return
		if (decl == owner || fqcn == owner.qualifiedName?.asString()) {
			logger.error(
				"@Validatable.Subtype type '$fqcn' must not be the same type as the " +
					"@Validatable parent '${owner.qualifiedName?.asString()}'.",
				ann,
			)
			return
		}
		if (!ownerType.isAssignableFrom(decl.asStarProjectedType())) {
			logger.error(
				"@Validatable.Subtype type '$fqcn' must extend or implement " +
					"'${owner.qualifiedName?.asString()}' (the @Validatable parent).",
				ann,
			)
			return
		}
		if (!hasValidatable(decl)) {
			logger.error(
				"@Validatable.Subtype type '$fqcn' must itself be annotated @Validatable.",
				owner,
			)
			return
		}
		if (isConcrete(decl)) {
			into += fqcn
		} else if (decl.modifiers.contains(Modifier.SEALED)) {
			collectSealedLeaves(decl, into)
		} else {
			logger.error(
				"@Validatable.Subtype type '$fqcn' is not a concrete class (and not a sealed type to expand).",
				owner,
			)
			return
		}
		if (!nameValue.isNullOrBlank()) {
			names[fqcn] = nameValue
		}
	}

	/**
	 * Compile-error when sealed leaves and declared `Subtype` sets disagree in either direction.
	 *
	 * Missing sealed members in `subtypes=[…]` or declared types that are not sealed subclasses
	 * both fail the build — silent union would hide author mistakes.
	 */
	private fun reconcile(
		owner: KSClassDeclaration,
		sealedConcrete: Set<String>,
		declaredConcrete: Set<String>,
	) {
		val onlySealed = sealedConcrete - declaredConcrete
		val onlyDeclared = declaredConcrete - sealedConcrete
		if (onlySealed.isEmpty() && onlyDeclared.isEmpty()) return

		val ownerName = owner.qualifiedName?.asString()
		if (onlySealed.isNotEmpty()) {
			logger.error(
				"Polymorphic @Validatable '$ownerName': sealed subclass(es) missing from " +
					"@Validatable(subtypes=[…]): ${onlySealed.joinToString()}. " +
					"Add matching Validatable.Subtype entries (or remove the subtypes array to rely on sealed discovery alone).",
				owner,
			)
		}
		if (onlyDeclared.isNotEmpty()) {
			logger.error(
				"Polymorphic @Validatable '$ownerName': @Validatable.Subtype type(s) that are not " +
					"sealed subclasses: ${onlyDeclared.joinToString()}.",
				owner,
			)
		}
	}

	/**
	 * Whether [clazz] should error when no concrete subtypes were found.
	 *
	 * True for interfaces, abstract types, and sealed non-class kinds (e.g. sealed interfaces).
	 * A sealed class with subclasses is still expanded via [collectSealedLeaves]; an empty result
	 * for a sealed class that is itself concrete does not use this gate.
	 */
	private fun isPolymorphicRoot(clazz: KSClassDeclaration): Boolean =
		clazz.classKind == ClassKind.INTERFACE ||
			clazz.isAbstract() ||
			(clazz.modifiers.contains(Modifier.SEALED) && clazz.classKind != ClassKind.CLASS)

	/**
	 * Instantiable class leaf: `class` kind and not abstract (sealed intermediate nodes are not).
	 */
	private fun isConcrete(clazz: KSClassDeclaration): Boolean =
		clazz.classKind == ClassKind.CLASS && !clazz.isAbstract()

	/** `true` when [decl] carries `@Validatable` (required for every declared subtype target).
	 *
	 * No side effects.
	 *
	 * @param decl candidate subtype declaration
	 * @return `true` when `@Validatable` is present on [decl]
	 */
	private fun hasValidatable(decl: KSClassDeclaration): Boolean =
		decl.has(ProcessorFqns.VALIDATABLE)
}
