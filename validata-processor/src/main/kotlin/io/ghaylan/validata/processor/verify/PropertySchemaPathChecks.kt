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
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScalarCompatibility
import io.ghaylan.validata.schema.shape.ScalarKind

/**
 * Schema sibling-path walk and subject scalar-kind compatibility checks.
 *
 * @property error emits a KSP diagnostic for the current verification site*
 * 
 * @author Ghaylan Saada
 */
internal class PropertySchemaPathChecks(
	private val error: (String) -> Unit,
) {

	/**
	 * Property-name indexes keyed by schema FQCN — built once per schema for this instance.
	 */
	private val propertyIndexes = HashMap<String, Map<String, PropertyModel>>()

	/**
	 * When [compatibilityKind] is not [PropertyRefCompatibilityKind.NONE], ensures the annotated
	 * subject’s scalar kind may carry that rule (open `validatedBy` → `@PropertyRef.compatibility`
	 * fallback from Phase 0 §4.4).
	 *
	 * Side effects: may emit a KSP error via [error].
	 *
	 * @param ownerSchema Schema that owns [subjectProperty].
	 * @param subjectProperty Annotated property.
	 * @param subjectShape Shape node carrying the constraint (may differ from [PropertyModel.shape]).
	 * @param compatibilityKind Scalar rule from sibling `@PropertyRef.compatibility`.
	 * @param annotationSimpleName Short annotation name for diagnostics.
	 */
	fun verifySubjectCompatibility(
		ownerSchema: SchemaModel,
		subjectProperty: PropertyModel,
		subjectShape: ShapeModel,
		compatibilityKind: PropertyRefCompatibilityKind,
		annotationSimpleName: String,
	) {
		if (compatibilityKind == PropertyRefCompatibilityKind.NONE) return
		val subjectKind = (subjectShape as? ScalarShapeModel)?.kind
			?: (subjectProperty.shape as? ScalarShapeModel)?.kind
			?: return
		if (PropertyRefScalarCompatibility.isSubjectCompatible(compatibilityKind, subjectKind)) {
			return
		}
		error(
			"Constraint @$annotationSimpleName on '${ownerSchema.qualifiedName}.${subjectProperty.declaredName}': " +
				PropertyRefScalarCompatibility.subjectMismatchMessage(
					annotationSimpleName,
					compatibilityKind,
					subjectKind),
		)
	}

	/**
	 * Resolves [path] from [ownerSchema], then — when [compatibilityKind] is not
	 * [PropertyRefCompatibilityKind.NONE] — compares the annotated property's scalar kind to the
	 * leaf's via [PropertyRefScalarCompatibility].
	 *
	 * Side effects: may emit KSP errors via [error] for depth, self-reference, existence, or type
	 * mismatch.
	 *
	 * @param ownerSchema Schema that owns [subjectProperty].
	 * @param subjectProperty Annotated property.
	 * @param path Single-segment sibling path from a constraint argument.
	 * @param compatibilityKind Scalar rule from sibling `@PropertyRef.compatibility`.
	 * @param annotationSimpleName Short annotation name for diagnostics.
	 * @param schemasByQualifiedName All schemas built in this round for nested object walks.
	 */
	fun verifySiblingPath(
		ownerSchema: SchemaModel,
		subjectProperty: PropertyModel,
		path: String,
		compatibilityKind: PropertyRefCompatibilityKind,
		annotationSimpleName: String,
		schemasByQualifiedName: Map<String, SchemaModel>,
	) {
		val segments = PropertyPath.split(path)
		if (segments.isEmpty()) return

		if (segments.size > 1) {
			error(
				"Constraint on '${ownerSchema.qualifiedName}.${subjectProperty.declaredName}' " +
					"references nested path '$path'. Only same-object field names are supported " +
					"(no dotted nested paths). Use List<@Distinct(by = […]) ElementDto> for fields of collection elements.")
			return
		}

		if (isSelfReference(subjectProperty, segments)) {
			error(
				"Constraint on '${ownerSchema.qualifiedName}.${subjectProperty.declaredName}' " +
					"cannot reference itself via '$path'. Cross-field constraints must name a " +
					"different sibling property.")
			return
		}

		val resolved = resolvePath(
			startSchema = ownerSchema,
			segments = segments,
			schemasByQualifiedName = schemasByQualifiedName,
			ownerLabel = "${ownerSchema.qualifiedName}.${subjectProperty.declaredName}",
			fullPath = path,
		) ?: return

		if (compatibilityKind == PropertyRefCompatibilityKind.NONE) return

		val annotatedKind = scalarKind(subjectProperty.shape)
		val referencedKind = scalarKind(resolved.leafShape)
		if (annotatedKind == null || referencedKind == null) {
			// Non-scalar annotated or referenced leaf — comparison constraints typically require scalars;
			// skip rather than invent a kind (runtime still guards with Class checks).
			return
		}
		if (!PropertyRefScalarCompatibility.isCompatible(compatibilityKind, annotatedKind, referencedKind)) {
			error(
				"Constraint @$annotationSimpleName on '${ownerSchema.qualifiedName}.${subjectProperty.declaredName}' " +
					"references '$path' with incompatible types: " +
					PropertyRefScalarCompatibility.mismatchMessage(
						compatibilityKind,
						annotatedKind,
						referencedKind,
					)
			)
		}
	}

	/**
	 * Walks [segments] starting at [startSchema], reporting existence / readability errors.
	 *
	 * Side effects: may emit KSP errors via [error].
	 *
	 * @param startSchema Schema where the first segment is resolved.
	 * @param segments Path segments from [PropertyPath.split].
	 * @param schemasByQualifiedName All schemas built in this round for nested object walks.
	 * @param ownerLabel Human label for diagnostics (e.g. `Owner.field`).
	 * @param fullPath Original path string for error messages.
	 * @return Leaf property and shape when the path fully resolves, or `null` after logging an error.
	 */
	fun resolvePath(
		startSchema: SchemaModel,
		segments: List<String>,
		schemasByQualifiedName: Map<String, SchemaModel>,
		ownerLabel: String,
		fullPath: String,
	): ResolvedLeaf? {
		var currentSchema = startSchema

		for ((index, segment) in segments.withIndex()) {
			val isLast = index == segments.lastIndex
			val property = findProperty(currentSchema, segment)
			if (property == null) {
				if (currentSchema.isPolymorphicRoot) {
					error(
						"Constraint on '$ownerLabel' references '$fullPath': segment '$segment' " +
							"is not a shared property of polymorphic type '${currentSchema.qualifiedName}'. " +
							"Subtype-only properties are unsupported in cross-field paths for this phase.",
					)
				} else {
					val known = currentSchema.properties
						.flatMap { listOf(it.declaredName, it.externalName) }
						.distinct()
						.sorted()
					error(
						"Constraint on '$ownerLabel' references unknown property '$segment' " +
							"in path '$fullPath' on '${currentSchema.qualifiedName}'. " +
							"Known properties: $known. Fix the path or rename the property.",
					)
				}
				return null
			}

			if (isLast) {
				return ResolvedLeaf(property, property.shape)
			}

			when (val shape = property.shape) {
				is ObjectRefShapeModel -> {
					val nested = schemasByQualifiedName[shape.typeQualifiedName]
					if (nested == null) {
						error(
							"Constraint on '$ownerLabel' cannot verify path '$fullPath': " +
								"nested type '${shape.typeQualifiedName}' has no schema in this compilation " +
								"(annotate it with @Validatable or ensure it is processed in this module).",
						)
						return null
					}
					currentSchema = nested
				}
				is DynamicShapeModel -> {
					if (property.noCascade) {
						error(
							"Constraint on '$ownerLabel' cannot reference through '${property.declaredName}' " +
								"in path '$fullPath' because it is @NoCascade and has no generated schema to check against.",
						)
					} else {
						error(
							"Constraint on '$ownerLabel' cannot step into '${property.declaredName}' " +
								"in path '$fullPath' — its shape is opaque/dynamic, not an object.",
						)
					}
					return null
				}
				is ScalarShapeModel -> {
					if (property.noCascade) {
						error(
							"Constraint on '$ownerLabel' cannot reference through '${property.declaredName}' " +
								"in path '$fullPath' because it is @NoCascade and has no generated schema to check against.",
						)
					} else {
						error(
							"Constraint on '$ownerLabel' cannot step into '${property.declaredName}' " +
								"in path '$fullPath' — '${property.declaredName}' is a ${shape.kind.name}, " +
								"it has no '${segments.getOrNull(index + 1)}' property.",
						)
					}
					return null
				}
				else -> {
					error(
						"Constraint on '$ownerLabel' cannot step into '${property.declaredName}' " +
							"in path '$fullPath' — shape is ${shape::class.simpleName}.",
					)
					return null
				}
			}
		}
		return null
	}

	/**
	 * Looks up a property by **declared or external** name so `@Compare("userName")`
	 * (when that is the `@JsonProperty`) both resolve.
	 *
	 * Side effects: may populate [propertyIndexes] for [schema].
	 *
	 * @param schema Schema to search.
	 * @param name Declared or wire property name.
	 * @return Matching [PropertyModel], or `null` when absent.
	 */
	fun findProperty(schema: SchemaModel, name: String): PropertyModel? {
		val map = propertyIndexes.getOrPut(schema.qualifiedName) { indexProperties(schema) }
		return map[name]
	}

	/**
	 * Builds declared-name / external-name → [PropertyModel] for [schema].
	 *
	 * Side effects: none.
	 *
	 * @param schema Schema whose properties are indexed.
	 * @return Map accepting both [PropertyModel.declaredName] and [PropertyModel.externalName].
	 */
	fun indexProperties(schema: SchemaModel): Map<String, PropertyModel> {
		val map = HashMap<String, PropertyModel>(schema.properties.size * 2)
		for (property in schema.properties) {
			map[property.declaredName] = property
			map[property.externalName] = property
		}
		return map
	}

	/**
	 * `true` when a single-segment path names the annotated property itself (declared or wire name).
	 *
	 * Side effects: none.
	 *
	 * @param subject Annotated property.
	 * @param segments Path segments from [PropertyPath.split].
	 * @return `true` when the path is a self-reference.
	 */
	fun isSelfReference(subject: PropertyModel, segments: List<String>): Boolean =
		segments.size == 1 &&
			(segments[0] == subject.declaredName || segments[0] == subject.externalName)

	/**
	 * Scalar kind for [PropertyRefScalarCompatibility] typed overloads.
	 *
	 * Side effects: none.
	 *
	 * @param shape Shape node to classify.
	 * @return [ScalarKind] when [shape] is [ScalarShapeModel]; otherwise `null`.
	 */
	fun scalarKind(shape: ShapeModel): ScalarKind? =
		(shape as? ScalarShapeModel)?.kind

	/**
	 * Scalar kind name for diagnostics that still need a string.
	 *
	 * Side effects: none.
	 *
	 * @param shape Shape node to classify.
	 * @return [ScalarKind.name] when [shape] is scalar; otherwise `null`.
	 */
	fun scalarKindName(shape: ShapeModel): String? = scalarKind(shape)?.name

	/**
	 * Successful path walk: the leaf [PropertyModel] and its [ShapeModel] (for scalar-kind checks).
	 *
	 * @property property Resolved leaf property.
	 * @property leafShape Shape of the leaf property.

	 */
	internal data class ResolvedLeaf(
		val property: PropertyModel,
		val leafShape: ShapeModel,
	)
}
