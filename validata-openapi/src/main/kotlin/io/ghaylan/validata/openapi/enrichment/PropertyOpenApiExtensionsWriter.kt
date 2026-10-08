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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.*
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KClass

/**
 * Writes [ConstraintExtensionKeys.CONSTRAINTS] and [ConstraintExtensionKeys.ERRORS]
 * onto a property / parameter OpenAPI [Schema] and nested item / map-value schemas.
 *
 * `x-validata-constraints` is an **array** of Validata constraints at **that** schema level
 * (property-level on the property schema; type-use / element constraints on `items` /
 * `additionalProperties`). Each entry is `{ "_constraint": "<Name>", …non-empty args }`.
 * Inactive validation groups are omitted when `activeGroups` is non-empty.
 *
 * `x-validata-errors` is written per schema level. The property root always includes
 * `VALUE_TYPE_MISMATCH` and `@ApiError` docs; nested levels only list constraint codes for that level.
 *
 * Does not mutate [Schema.description]. Native JSON Schema facets are applied separately by
 * [ConstraintSchemaApplicator] / [JsonSchemaFacetApplicator].*
 * 
 * @author Ghaylan Saada
 */
object PropertyOpenApiExtensionsWriter {
	
	/**
	 * Publishes constraint entries and per-level error docs for [prop] onto [schema] and nested schemas.
	 *
	 * Mutates [schema] (and nested item / additionalProperties schemas) when constraints or errors
	 * are present.
	 *
	 * @param schema OpenAPI schema for this property or parameter
	 * @param prop Validata IR
	 * @param activeGroups endpoint groups for filtering (empty = all active)
	 * @param ownerSchema enclosing object schema for sibling `@PropertyRef` rewrite; may be `null`	 
	 */
	fun write(
		schema: Schema<*>,
		prop: PropertySpec,
		activeGroups: Set<KClass<*>> = emptySet(),
		ownerSchema: ObjectSchema? = null,
	) {
		val elementSchema = elementObjectSchema(prop.shape)
		writeLevel(
			schema = schema,
			constraints = prop.constraints,
			activeGroups = activeGroups,
			siblingSchema = ownerSchema,
			elementSchema = elementSchema,
			includeTypeMismatch = true,
			errorDocs = prop.errorDocs,
			type = TypeShapeJavaTypes.resolve(prop.shape))
		writeShapeExtensions(
			shape = prop.shape,
			schema = schema,
			activeGroups = activeGroups,
			siblingSchema = ownerSchema)
	}
	
	/**
	 * Writes type-use constraints for [shape] onto [schema], then recurses into list/map children.
	 */
	private fun writeShapeExtensions(
		shape: TypeShape,
		schema: Schema<*>,
		activeGroups: Set<KClass<*>>,
		siblingSchema: ObjectSchema?,
	) {
		writeLevel(
			schema = schema,
			constraints = shape.constraints,
			activeGroups = activeGroups,
			siblingSchema = siblingSchema,
			elementSchema = elementObjectSchema(shape),
			includeTypeMismatch = false,
			errorDocs = emptyList(),
			mergeIntoExisting = true,
			type = TypeShapeJavaTypes.resolve(shape))
		when (shape) {
			is ScalarShape,
			is DynamicShape,
			is ObjectRefShape -> Unit
			is IterableShape -> {
				val items = schema.items ?: Schema<Any>().also { schema.items = it }
				writeShapeExtensions(
					shape = shape.element,
					schema = items,
					activeGroups = activeGroups,
					siblingSchema = elementObjectSchema(shape))
			}
			is MapShape -> {
				writeShapeExtensions(shape.key, schema, activeGroups, siblingSchema)
				val valueSchema = OpenApiMapValueSchemas.resolve(schema) ?: return
				writeShapeExtensions(shape.value, valueSchema, activeGroups, siblingSchema)
			}
		}
	}
	
	/**
	 * Appends active constraint entries and errors for one schema level.
	 *
	 * @param mergeIntoExisting when `true`, merges into existing extension arrays (shape constraints
	 *   on the same schema as property constraints)	 
	 */
	private fun writeLevel(
		schema: Schema<*>,
		constraints: List<CompiledConstraint>,
		activeGroups: Set<KClass<*>>,
		siblingSchema: ObjectSchema?,
		elementSchema: ObjectSchema?,
		includeTypeMismatch: Boolean,
		errorDocs: List<io.ghaylan.validata.schema.docs.SchemaErrorDoc>,
		mergeIntoExisting: Boolean = false,
		type: Class<*>,
	) {
		val entries = ArrayList<Map<String, Any?>>()
		
		if (mergeIntoExisting) {
			@Suppress("UNCHECKED_CAST")
			val existing = schema.extensions?.get(ConstraintExtensionKeys.CONSTRAINTS) as? List<Map<String, Any?>>
			if (existing != null) entries.addAll(existing)
		}
		
		appendConstraintEntries(
			compiled = constraints,
			out = entries,
			activeGroups = activeGroups,
			siblingSchema = siblingSchema,
			elementSchema = elementSchema)
		
		if (entries.isNotEmpty()) {
			schema.addExtension(ConstraintExtensionKeys.CONSTRAINTS, entries)
		}
		
		val errors = PropertyErrorCodeCollector.collectForLevel(
			constraints = constraints,
			activeGroups = activeGroups,
			includeTypeMismatch = includeTypeMismatch,
			errorDocs = errorDocs,
			type = type)
		
		if (errors.isEmpty()) return
		
		if (mergeIntoExisting) {
			@Suppress("UNCHECKED_CAST")
			val existingErrors = schema.extensions?.get(ConstraintExtensionKeys.ERRORS) as? List<Map<String, String>>
			
			if (existingErrors != null) {
				val byCode = linkedMapOf<String, String>()
				for (entry in existingErrors) {
					val code = entry["code"] ?: continue
					byCode[code] = entry["message"].orEmpty()
				}
				for (entry in errors) {
					val code = entry["code"]
						?: continue
					byCode[code] = entry["message"].orEmpty()
				}
				schema.addExtension(
					ConstraintExtensionKeys.ERRORS,
					byCode.entries.sortedBy { it.key }
						.map { (code, message) ->
							linkedMapOf("code" to code, "message" to message)
						},
				)
				return
			}
		}
		schema.addExtension(ConstraintExtensionKeys.ERRORS, errors)
	}
	
	/**
	 * Appends one entry per active compiled constraint in [compiled] into [out].
	 */
	private fun appendConstraintEntries(
		compiled: List<CompiledConstraint>,
		out: MutableList<Map<String, Any?>>,
		activeGroups: Set<KClass<*>>,
		siblingSchema: ObjectSchema?,
		elementSchema: ObjectSchema?,
	) {
		for (c in compiled) {
			val metadata = c.metadata as? ConstraintMetadata ?: continue
			if (!ConstraintGroupFilter.isActive(metadata, activeGroups)) continue
			out += ConstraintMetadataOpenApiSerializer.toConstraintEntry(
				metadata = metadata,
				siblingSchema = siblingSchema,
				elementSchema = elementSchema)
		}
	}
	
	/**
	 * Object schema of a list/array element when [shape] is an iterable of object refs.
	 */
	private fun elementObjectSchema(shape: TypeShape): ObjectSchema? {
		val element = (shape as? IterableShape)?.element ?: return null
		return (element as? ObjectRefShape)?.ref?.value
	}
}
