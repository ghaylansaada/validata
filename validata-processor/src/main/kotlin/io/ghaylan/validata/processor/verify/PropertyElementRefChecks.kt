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
package io.ghaylan.validata.processor.verify

import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.schema.PropertyPath

/**
 * Element-path (`by = […]`) and placement checks for `@Distinct`.
 *
 * `@Distinct` must be **type-use on the collection element** (`List<@Distinct T>`). Property-level
 * or list-type placement (`@Distinct val users: List<…>` / `@Distinct List<…>`) is rejected.
 * When `by` is non-empty, names resolve against the element DTO ([ObjectRefShapeModel]).
 *
 * @property error emits a KSP diagnostic for the current verification site
 * @property maxShapeNestingDepth cap for [describeShape] nesting
 * @property pathChecks resolves element field names on element schemas
 *
 * @author Ghaylan Saada
 */
internal class PropertyElementRefChecks(
	private val error: (String) -> Unit,
	private val maxShapeNestingDepth: Int,
	private val pathChecks: PropertySchemaPathChecks,
) {
	
	/**
	 * Rejects `@Distinct` on a collection / map container shape (must be element type-use).
	 *
	 * Side effects: may emit KSP errors via [error].
	 *
	 * @param ownerLabel Human label for diagnostics (schema or endpoint coordinate).
	 * @param subjectName Declared property or parameter name.
	 * @param subjectShape Shape carrying the constraint.
	 * @param annotationSimpleName Short annotation name; only `"Distinct"` is checked.
	 */
	fun reportDistinctContainerPlacement(
		ownerLabel: String,
		subjectName: String,
		subjectShape: ShapeModel,
		annotationSimpleName: String,
	) {
		if (annotationSimpleName != "Distinct") return
		if (subjectShape !is IterableShapeModel && subjectShape !is MapShapeModel) return
		val site = "$ownerLabel.$subjectName"
		error(
			"@Distinct on '$site' must be type-use on the collection element " +
				"(e.g. List<@Distinct String> or List<@Distinct(by = [\"id\"]) User>), " +
				"not on the List/Set/array property or type itself.",
		)
	}
	
	/**
	 * Logs an error when [elementRefs] cannot be resolved as properties of each collection element.
	 *
	 * Element paths (`@Distinct(by = ["email"])`) must be **single-segment** names of fields on the
	 * element DTO — nested paths inside the element are not supported.
	 *
	 * Supported annotated shape for non-empty [elementRefs]: [ObjectRefShapeModel] (element type-use
	 * on a `@Validatable` DTO). Scalars, maps, and collections get a precise diagnostic.
	 *
	 * Side effects: may emit KSP errors via [error].
	 *
	 * @param ownerLabel Human label for diagnostics (schema or endpoint coordinate).
	 * @param subjectName Declared property or parameter name.
	 * @param subjectShape Shape carrying the constraint.
	 * @param elementRefs Element paths from `@PropertyRef(scope = ELEMENT)`.
	 * @param annotationSimpleName Short annotation name for diagnostics.
	 * @param schemasByQualifiedName All schemas built in this round for element-type lookup.
	 */
	fun reportUnknownElementRefs(
		ownerLabel: String,
		subjectName: String,
		subjectShape: ShapeModel,
		elementRefs: List<String>,
		annotationSimpleName: String,
		schemasByQualifiedName: Map<String, SchemaModel>,
	) {
		if (elementRefs.isEmpty()) return
		val site = "$ownerLabel.$subjectName"
		val paths = elementRefs.joinToString(", ") { "'$it'" }
		val ann = if (annotationSimpleName.isNotBlank()) "@$annotationSimpleName" else "Constraint"
		
		when (subjectShape) {
			is ObjectRefShapeModel -> resolveObjectElementRefs(
				ann = ann,
				site = site,
				paths = paths,
				objectRef = subjectShape,
				elementRefs = elementRefs,
				schemasByQualifiedName = schemasByQualifiedName,
			)
			
			is IterableShapeModel -> error(
				"$ann on '$site' references element path(s) $paths, but is placed on the " +
					"collection itself. Use type-use on the element DTO " +
					"(e.g. List<@Distinct(by = [\"id\"]) User>).",
			)
			
			else -> error(
				"$ann on '$site' references element path(s) $paths, but the annotated type is " +
					"${describeShape(subjectShape)} — not an object element type-use. Place " +
					"@Distinct(by = […]) as type-use on a @Validatable DTO element " +
					"(e.g. List<@Distinct(by = [\"id\"]) User>); not on a scalar type-use " +
					"such as List<@Distinct String>.",
			)
		}
	}
	
	/**
	 * Resolves [elementRefs] against the `@Validatable` schema for [objectRef].
	 *
	 * Side effects: may emit KSP errors via [error] / [pathChecks].
	 *
	 * @param ann Annotation label for diagnostics (e.g. `@Distinct`).
	 * @param site Owner.property coordinate for diagnostics.
	 * @param paths Comma-joined quoted [elementRefs] for missing-schema messages.
	 * @param objectRef Element DTO shape whose schema owns the `by` fields.
	 * @param elementRefs Single-segment field names to resolve.
	 * @param schemasByQualifiedName Schemas built in this round.
	 */
	private fun resolveObjectElementRefs(
		ann: String,
		site: String,
		paths: String,
		objectRef: ObjectRefShapeModel,
		elementRefs: List<String>,
		schemasByQualifiedName: Map<String, SchemaModel>,
	) {
		val elementSchema = schemasByQualifiedName[objectRef.typeQualifiedName]
		if (elementSchema == null) {
			error(
				"$ann on '$site' references element path(s) $paths, but no " +
					"@Validatable schema was generated for element type " +
					"'${objectRef.typeQualifiedName}'. Annotate that type with @Validatable.",
			)
			return
		}
		for (ref in elementRefs) {
			val segments = PropertyPath.split(ref)
			if (segments.isEmpty()) continue
			if (segments.size > 1) {
				error(
					"$ann on '$site' element path '$ref' is nested. Only single-segment field " +
						"names of the collection element type are supported.",
				)
				continue
			}
			pathChecks.resolvePath(
				startSchema = elementSchema,
				segments = segments,
				schemasByQualifiedName = schemasByQualifiedName,
				ownerLabel = site,
				fullPath = ref,
			)
		}
	}
	
	/**
	 * Short shape label for diagnostics (no FQCN noise for scalars).
	 *
	 * Side effects: none.
	 *
	 * @param shape Shape to summarize.
	 * @param depth Nesting depth for cycle/depth safety.
	 * @return Human-readable shape kind for error text.
	 */
	fun describeShape(
		shape: ShapeModel,
		depth: Int = 0
	): String {
		if (depth > maxShapeNestingDepth) return "…"
		return when (shape) {
			is ScalarShapeModel -> "scalar (${shape.kind.name})"
			is IterableShapeModel -> "collection of ${describeShape(shape.element, depth + 1)}"
			is MapShapeModel -> "map"
			is ObjectRefShapeModel -> "object '${shape.typeQualifiedName}'"
			is DynamicShapeModel -> "opaque/dynamic type"
		}
	}
}
