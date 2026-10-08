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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.compat.KotlinStringLiteral
import io.ghaylan.validata.processor.fqns.CodegenFqns
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.ObjectRefShapeModel
import io.ghaylan.validata.processor.model.PropertyModel
import io.ghaylan.validata.processor.model.SchemaModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.processor.naming.GeneratedPackageNamer
import io.ghaylan.validata.schema.PropertyPath
import java.util.*

/**
 * Emits a generated `<Type>_` constants object for wire / error-path strings.
 *
 * **Purpose.** Client-visible path strings for hand-built errors and messages
 * (`CreateUserRequest_.FIRST_NAME` → `"first_name"`). Values use [PropertyModel.externalName]
 * (`@JsonProperty` or KSP `validata.jackson.naming`). Identifiers come from
 * [PropertyModel.declaredName] (`firstName` → `FIRST_NAME`).
 *
 * **Annotations.** `@PropertyRef` / `@Compare` / `@RequiredWhen` may use either:
 * - Kotlin **declared** names as string literals (`ref = "firstName"`) — preferred for authorship, or
 * - these wire constants (`ref = CreateUserRequest_.FIRST_NAME`) — also resolve at runtime / KSP
 *   because [PropertyPath] accepts both declared and external spellings.
 *
 * No second generated class is required for declared names.
 *
 * Nested object refs become nested `object` blocks up to [PropertyPath.MAX_REFERENCE_PATH_DEPTH];
 * types already on the current nesting path are omitted so cyclic graphs terminate.
 *
 * Related: `@PropertyRef` ([ProcessorFqns.PROPERTY_REF]). Package: `<typePkg>.ghaylan.validata`
 * via [GeneratedPackageNamer.typePackage].
 */
internal object FieldsObjectCodeWriter {
	
	private val CAMEL_BOUNDARY = Regex("([a-z0-9])([A-Z])")
	
	/**
	 * Package for the Fields file of [model].
	 *
	 * Side effects: none.
	 *
	 * @param model Root `@Validatable` schema.
	 * @param moduleFallback Fallback when [model]'s package is empty.
	 * @return Generated package from [GeneratedPackageNamer.typePackage].
	 */
	fun packageNameFor(
		model: SchemaModel,
		moduleFallback: String
	): String =
		GeneratedPackageNamer.typePackage(model.packageName, moduleFallback)
	
	/** File / object simple name: `UserRequest_` or `Outer_Inner_`. */
	fun fileName(model: SchemaModel): String =
		GeneratedNames.fieldsFileName(model.packageName, model.qualifiedName)
	
	/** Generated top-level object simple name for [model]'s Fields constants (matches [fileName]). */
	fun objectName(model: SchemaModel): String =
		GeneratedNames.fieldsObjectName(model.packageName, model.qualifiedName)
	
	/**
	 * Renders one Fields constants file for [model].
	 *
	 * @param packageName package of the generated file
	 * @param model root `@Validatable` schema
	 * @param schemasByQualifiedName all schemas in this round (for nested object walks)
	 * @return complete Kotlin source
	 */
	fun write(
		packageName: String,
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
	): String {
		val b = KotlinSourceBuilder(packageName)
		val name = objectName(model)
		b.addImport(model.qualifiedName)
		b.addImport(CodegenFqns.PROPERTY_SPEC)
		b.line("/**")
		b.line(" * Generated wire-path constants for [${model.simpleName}].")
		b.line(" *")
		b.line(" * Values are [PropertySpec.externalName] segments (`@JsonProperty` / Jackson naming) —")
		b.line(" * use for client-visible error paths. For `@PropertyRef` / `@Compare`, declared Kotlin")
		b.line(" * names as string literals also work; these constants resolve too.")
		b.line(" * Regenerated whenever [${model.simpleName}] changes.")
		b.line(" */")
		b.block("object $name") {
			if (model.isPolymorphicRoot && model.properties.isEmpty()) {
				line("// Polymorphic root with no shared properties — no path constants.")
			}
			else {
				emitProperties(
					properties = model.properties.sortedBy { it.declaredName },
					pathPrefix = "",
					schemasByQualifiedName = schemasByQualifiedName,
					pathTypes = mutableSetOf(model.qualifiedName),
					depth = 1,
					usedConstNames = mutableSetOf(),
				)
			}
		}
		return b.build(model.qualifiedName)
	}
	
	private fun KotlinSourceBuilder.emitProperties(
		properties: List<PropertyModel>,
		pathPrefix: String,
		schemasByQualifiedName: Map<String, SchemaModel>,
		pathTypes: MutableSet<String>,
		depth: Int,
		usedConstNames: MutableSet<String>,
	) {
		for (property in properties) {
			val segment = property.externalName
			val fullPath = if (pathPrefix.isEmpty()) segment else "$pathPrefix.$segment"
			val constName = uniqueConstName(property.declaredName, usedConstNames)
			line("/** Wire path to `$fullPath` (declared `${property.declaredName}`). */")
			line("const val $constName: String = \"${escape(fullPath)}\"")
			line()
			val nestedRef = property.shape as? ObjectRefShapeModel
				?: continue
			if (depth >= PropertyPath.MAX_REFERENCE_PATH_DEPTH) {
				line("// Nested object '${property.declaredName}' omitted — max reference depth ${PropertyPath.MAX_REFERENCE_PATH_DEPTH} reached.")
				line()
				continue
			}
			if (nestedRef.typeQualifiedName in pathTypes) {
				line("// Nested object '${property.declaredName}' (${nestedRef.typeQualifiedName}) omitted — cycle on the current path.")
				line()
				continue
			}
			val nestedSchema = schemasByQualifiedName[nestedRef.typeQualifiedName]
				?: continue
			if (nestedSchema.properties.isEmpty()) continue
			val nestedObjectName = nestedObjectName(property.declaredName)
			addImport(nestedSchema.qualifiedName)
			line("/** Nested wire paths into `$fullPath` ([${nestedSchema.simpleName}]). */")
			block("object $nestedObjectName") {
				pathTypes += nestedRef.typeQualifiedName
				emitProperties(
					properties = nestedSchema.properties.sortedBy { it.declaredName },
					pathPrefix = fullPath,
					schemasByQualifiedName = schemasByQualifiedName,
					pathTypes = pathTypes,
					depth = depth + 1,
					usedConstNames = mutableSetOf(),
				)
				pathTypes -= nestedRef.typeQualifiedName
			}
			line()
		}
	}
	
	/**
	 * `minAge` → `MIN_AGE`, `firstName` → `FIRST_NAME`, `URL` → `URL`.
	 */
	internal fun constName(declaredName: String): String {
		val withUnderscores = declaredName.replace(CAMEL_BOUNDARY, "$1_$2")
		return withUnderscores.uppercase(Locale.ROOT)
	}
	
	/**
	 * [constName] plus `_2`, `_3`, … when [used] already contains that SCREAMING_SNAKE token.
	 *
	 * Side effects: inserts the chosen name into [used].
	 */
	internal fun uniqueConstName(
		declaredName: String,
		used: MutableSet<String>
	): String {
		val base = constName(declaredName)
		var candidate = base
		var n = 2
		while (!used.add(candidate)) {
			candidate = "${base}_$n"
			n++
		}
		return candidate
	}
	
	/**
	 * Nested companion object name for a property.
	 *
	 * Prefer Title case (`address` → `Address`) so usage reads `UserRequest_.Address.CITY`.
	 * When that would collide with the property's own const (`b` → const `B` and object `B`),
	 * fall back to the declared camelCase name so the generated file still compiles.
	 */
	internal fun nestedObjectName(declaredName: String): String {
		val titled = declaredName.replaceFirstChar { it.uppercaseChar() }
		return if (titled == constName(declaredName)) declaredName else titled
	}
	
	private fun escape(s: String): String =
		KotlinStringLiteral.escape(s)
}
