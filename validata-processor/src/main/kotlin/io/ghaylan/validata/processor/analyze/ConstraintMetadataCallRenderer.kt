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

import com.google.devtools.ksp.symbol.*
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.KotlinStringLiteral
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Renders constraint metadata constructor calls as Kotlin source literals for codegen.
 *
 * Annotation argument names become named parameters so the call stays readable and order-independent.
 * Collection args choose `setOf` / `listOf` from the metadata parameter type (or annotation heuristics
 * when the metadata class is not yet visible in the current KSP round).*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintMetadataCallRenderer {

	/**
	 * Builds the Kotlin source for a metadata constructor, e.g.
	 * `io.…RequiredConstraint(mode = Required.Mode.STRICT, …)`.
	 *
	 * @param metadataQ Fully qualified metadata class.
	 * @param ann The usage-site annotation supplying argument values.
	 * @param metadataDecl Already-resolved metadata class (avoids a second lookup).
	 * @param metadataParamSetCache Round cache: metadata FQCN → (param name → is Set-like).
	 * @return Kotlin metadata constructor call source text
	 */
	fun buildMetadataCall(
		metadataQ: String,
		ann: KSAnnotation,
		metadataDecl: KSClassDeclaration?,
		metadataParamSetCache: MutableMap<String, Map<String, Boolean>>,
	): String {
		val args = ann.arguments.mapNotNull { arg ->
			val name = arg.name?.asString() ?: return@mapNotNull null
			"$name = ${renderArg(arg, ann, metadataDecl, metadataParamSetCache)}"
		}
		if (args.isEmpty()) return "$metadataQ()"
		return buildString {
			append(metadataQ)
			append("(\n")
			args.forEachIndexed { index, arg ->
				append("    ")
				append(arg)
				if (index < args.lastIndex) append(',')
				append('\n')
			}
			append(')')
		}
	}

	/**
	 * Renders one annotation argument as Kotlin source text for the generated file.
	 *
	 * Handles primitives, strings, `KClass`/`::class`, lists/sets, and enums.
	 * Collection args use [collectionLiteral] so `Set` metadata params get `setOf` / `emptySet`
	 * (annotation `Array` arguments become Kotlin set/list literals in generated source).
	 *
	 * Enums need special care: KSP’s `toString()` is often short (`RequirementCondition.ALWAYS`);
	 * [renderEnumLike] expands that to a fully qualified constant.
	 *
	 * @param arg Annotation argument from KSP.
	 * @param ann Owning annotation (needed to resolve enum property types).
	 * @param metadataDecl Generated `{Name}Constraint` metadata class, if resolvable this round.
	 * @param metadataParamSetCache Round cache for Set-like metadata params.
	 * @return Kotlin expression source for [arg]
	 */
	fun renderArg(
		arg: KSValueArgument,
		ann: KSAnnotation,
		metadataDecl: KSClassDeclaration?,
		metadataParamSetCache: MutableMap<String, Map<String, Boolean>>,
	): String {
		val paramName = arg.name?.asString()
		return when (val v = arg.value) {
			// KSP represents an enum constant literally named `NULL` as a Kotlin null value.
			null -> renderNullEnumArg(arg, ann, metadataDecl, paramName)
			is String -> "\"${v.escape()}\""
			is Char -> "'$v'"
			is Boolean, is Byte, is Short, is Int, is Long, is Float, is Double -> v.toString()
			is KSType -> {
				val q = ksTypeFqcn(v) ?: return "null"
				"$q::class"
			}
			is List<*> -> {
				if (v.isEmpty()) {
					collectionLiteral(ann, metadataDecl, paramName, elements = "", empty = true, metadataParamSetCache)
				} else if (v.first() is KSType) {
					val classes = v.filterIsInstance<KSType>()
						.mapNotNull { ksTypeFqcn(it)?.let { fqcn -> "$fqcn::class" } }
						.joinToString(", ")
					collectionLiteral(ann, metadataDecl, paramName, elements = classes, empty = false, metadataParamSetCache)
				} else if (v.first() is String) {
					val strings = v.joinToString(", ") { "\"${(it as String).escape()}\"" }
					collectionLiteral(ann, metadataDecl, paramName, elements = strings, empty = false, metadataParamSetCache)
				} else {
					val rendered = v.joinToString(", ") { item ->
						when (item) {
							is KSType -> {
								val q = ksTypeFqcn(item) ?: return@joinToString "null"
								"$q::class"
							}
							null -> renderNullEnumArg(arg, ann, metadataDecl, paramName)
							else -> renderEnumLike(item.toString(), arg, ann) ?: item.toString()
						}
					}
					collectionLiteral(ann, metadataDecl, paramName, elements = rendered, empty = false, metadataParamSetCache)
				}
			}
			else -> renderEnumLike(v.toString(), arg, ann)
				?: run {
					val text = v.toString()
					if (text.contains('.')) text else "\"${text.escape()}\""
				}
		}
	}

	/**
	 * Chooses `setOf`/`emptySet` vs `listOf`/`emptyList` from the metadata parameter type.
	 *
	 * Annotation params are usually `Array<…>`; many metadata classes store them as `Set<…>`
	 * (`UrlConstraint`, `PhoneConstraint`, `groups`, …). Emitting the wrong collection type
	 * makes generated sources fail to compile.
	 *
	 * Falls back to `Set` when the parameter is named `groups` and metadata cannot be resolved,
	 * or when the annotation parameter carries `@ConstraintGroups`.
	 *
	 * No side effects beyond [metadataParamSetCache] population.
	 *
	 * @param ann usage-site constraint annotation
	 * @param metadataDecl resolved metadata class, if any
	 * @param paramName annotation argument name
	 * @param elements comma-separated rendered collection elements
	 * @param empty when `true`, emit an empty collection literal
	 * @param metadataParamSetCache round cache for Set-like metadata params
	 * @return Kotlin collection literal source text
	 */
	private fun collectionLiteral(
		ann: KSAnnotation,
		metadataDecl: KSClassDeclaration?,
		paramName: String?,
		elements: String,
		empty: Boolean,
		metadataParamSetCache: MutableMap<String, Map<String, Boolean>>,
	): String {
		val useSet = metadataParamIsSet(metadataDecl, paramName, metadataParamSetCache) ||
			(metadataDecl == null && paramName == AnnotationAttrs.ConstraintPayload.GROUPS) ||
			isConstraintGroupsParam(ann, paramName) ||
			// Same-round custom constraints: metadata class is not resolvable yet, but
			// ConstraintMetadataModelBuilder always maps annotation Array → Set.
			(metadataDecl == null && annotationParamIsArray(ann, paramName))
		return when {
			empty && useSet -> MetadataCollectionLiteral.emptySet()
			empty -> MetadataCollectionLiteral.emptyList()
			useSet -> MetadataCollectionLiteral.setOf(elements)
			else -> MetadataCollectionLiteral.listOf(elements)
		}
	}

	/**
	 * Whether [paramName] on [ann]'s annotation type carries `@ConstraintGroups`.
	 *
	 * No side effects.
	 *
	 * @param ann constraint annotation usage
	 * @param paramName annotation argument name to inspect
	 * @return `true` when the declared parameter has `@ConstraintGroups`
	 */
	private fun isConstraintGroupsParam(ann: KSAnnotation, paramName: String?): Boolean {
		if (paramName == null) return false
		val annotationDecl =
			ann.annotationType.resolve().declaration as? KSClassDeclaration ?: return false
		val param = annotationDecl.primaryConstructor?.parameters
			?.firstOrNull { it.name?.asString() == paramName }
			?: return false
		return param.annotations.any {
			AnnotationFqcn.isA(it, ProcessorFqns.CONSTRAINT_GROUPS)
		}
	}

	/**
	 * Whether [paramName] on [ann]'s annotation type is declared as `Array<…>`.
	 *
	 * No side effects.
	 *
	 * @param ann constraint annotation usage
	 * @param paramName annotation argument name to inspect
	 * @return `true` for Kotlin or Java array parameter types
	 */
	private fun annotationParamIsArray(ann: KSAnnotation, paramName: String?): Boolean {
		if (paramName == null) return false
		val annotationDecl =
			ann.annotationType.resolve().declaration as? KSClassDeclaration ?: return false
		val param = annotationDecl.primaryConstructor?.parameters
			?.firstOrNull { it.name?.asString() == paramName }
			?: return false
		val type = param.type.resolve()
		val q = type.declaration.qualifiedName?.asString()
		val simple = type.declaration.simpleName.asString()
		return q == TypeNames.ARRAY_KOTLIN ||
			simple == "Array" ||
			(q != null && KnownTypes.primitiveArrayElementFqcn(q) != null)
	}

	/**
	 * `true` when [metadataDecl] declares [paramName] as a `Set` / `MutableSet` (Kotlin or Java).
	 *
	 * Mutates [metadataParamSetCache] on first lookup per metadata FQCN.
	 *
	 * @param metadataDecl resolved metadata class, if any
	 * @param paramName metadata constructor parameter name
	 * @param metadataParamSetCache round cache for Set-like metadata params
	 * @return `true` when the metadata property type is a set interface
	 */
	private fun metadataParamIsSet(
		metadataDecl: KSClassDeclaration?,
		paramName: String?,
		metadataParamSetCache: MutableMap<String, Map<String, Boolean>>,
	): Boolean {
		if (metadataDecl == null || paramName == null) return false
		val metaFqcn = metadataDecl.qualifiedName?.asString() ?: return false
		val paramMap = metadataParamSetCache.getOrPut(metaFqcn) {
			val map = HashMap<String, Boolean>()
			for (prop in metadataDecl.getAllProperties()) {
				val name = prop.simpleName.asString()
				val typeName = prop.type.resolve().declaration.qualifiedName?.asString()
				map[name] = typeName == TypeNames.SET_KOTLIN ||
					typeName == TypeNames.MUTABLE_SET_KOTLIN ||
					typeName == TypeNames.SET_JAVA
			}
			map
		}
		return paramMap[paramName] == true
	}

	/**
	 * Renders a Kotlin-null annotation argument that is actually an enum entry named `NULL`.
	 *
	 * Side effects: none.
	 *
	 * @param arg Annotation argument whose [KSValueArgument.value] is null.
	 * @param ann Owning annotation usage.
	 * @param metadataDecl Generated metadata class (preferred type source).
	 * @param paramName Argument / metadata parameter name; may be null.
	 * @return `EnumType.`NULL`` source, or the literal `null` when the parameter is not an enum.
	 */
	private fun renderNullEnumArg(
		arg: KSValueArgument,
		ann: KSAnnotation,
		metadataDecl: KSClassDeclaration?,
		paramName: String?,
	): String {
		enumTypeFqcnFromMetadata(metadataDecl, paramName)?.let { return "$it.`NULL`" }
		renderEnumLike("NULL", arg, ann)?.let { fqcn ->
			val type = fqcn.substringBeforeLast('.')
			return "$type.`NULL`"
		}
		return "null"
	}

	/**
	 * Fully qualified enum type of a metadata constructor parameter, or `null` when not an enum.
	 *
	 * Side effects: none.
	 */
	private fun enumTypeFqcnFromMetadata(
		metadataDecl: KSClassDeclaration?,
		paramName: String?,
	): String? {
		if (metadataDecl == null || paramName == null) return null
		val param = metadataDecl.primaryConstructor?.parameters
			?.firstOrNull { it.name?.asString() == paramName }
			?: return null
		val type = param.type.resolve()
		val decl = type.declaration as? KSClassDeclaration ?: return null
		val enumDecl = when {
			decl.classKind == ClassKind.ENUM_CLASS -> decl
			else -> type.arguments.firstOrNull()
				?.type
				?.resolve()
				?.declaration as? KSClassDeclaration
		} ?: return null
		if (enumDecl.classKind != ClassKind.ENUM_CLASS) return null
		return enumDecl.qualifiedName?.asString()
	}

	/**
	 * Expands KSP’s short enum text to a FQCN using the annotation property’s declared type.
	 *
	 * Example: `"RequirementCondition.ALWAYS"` on `@Required.condition` becomes the fully
	 * qualified `Required.RequirementCondition.ALWAYS` so generated code compiles without
	 * extra imports.
	 *
	 * For array/list annotation parameters (`days: Array<DayOfWeek>`), the enum type is the
	 * element type of the collection, not the array declaration itself.
	 *
	 * @param shortText KSP `toString()` of the enum value.
	 * @param arg The annotation argument (name looks up the property).
	 * @param ann The constraint annotation usage.
	 * @return Fully qualified `EnumType.ENTRY`, or `null` if this is not an enum property.
	 */
	private fun renderEnumLike(shortText: String, arg: KSValueArgument, ann: KSAnnotation): String? {
		val argName = arg.name?.asString() ?: return null
		val annDecl = ann.annotationType.resolve().declaration as? KSClassDeclaration ?: return null
		val prop = annDecl.getAllProperties().firstOrNull { it.simpleName.asString() == argName }
			?: return null
		val propType = prop.type.resolve()
		val propDecl = propType.declaration as? KSClassDeclaration ?: return null
		val enumDecl = when {
			propDecl.classKind == ClassKind.ENUM_CLASS -> propDecl
			else -> propType.arguments.firstOrNull()
				?.type
				?.resolve()
				?.declaration as? KSClassDeclaration
		} ?: return null
		if (enumDecl.classKind != ClassKind.ENUM_CLASS) return null
		val enumTypeQ = enumDecl.qualifiedName?.asString() ?: return null
		val entry = shortText.substringAfterLast('.')
		if (entry.isBlank()) return null
		val looksLikeEnum = shortText == entry || shortText.contains('.')
		if (!looksLikeEnum) return null
		// Enum entry named NULL must be backtick-escaped in Kotlin source.
		val entrySrc = if (entry == "NULL") "`NULL`" else entry
		return "$enumTypeQ.$entrySrc"
	}

	/** Safe FQCN for a [KSType]; avoids `!!` on missing qualified names.
	 *
	 * No side effects.
	 *
	 * @param type resolved KSP type
	 * @return declaration qualified name, or `null` when absent
	 */
	internal fun ksTypeFqcn(type: KSType): String? =
		type.declaration.qualifiedName?.asString()

	/** Escapes a string so it is safe inside a generated Kotlin `"…"` literal.
	 *
	 * No side effects.
	 *
	 * @return escaped string body without surrounding quotes
	 */
	private fun String.escape(): String = KotlinStringLiteral.escape(this)
}
