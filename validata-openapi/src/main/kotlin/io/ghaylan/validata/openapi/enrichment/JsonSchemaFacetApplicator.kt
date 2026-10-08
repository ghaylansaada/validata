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

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

/**
 * Applies [ConstraintDocHints] / [JsonSchemaFacet]s onto an OpenAPI [Schema].
 *
 * Unknown facet types are ignored so the facet model stays forward-compatible.
 *
 * Never mutates [Schema.description]. Does not write vendor tags — custom constraint
 * Custom constraint payloads (and all constraint entries) live under
 * [ConstraintExtensionKeys.CONSTRAINTS].
 *
 * When several constraints enrich the same schema, bounds are merged **strictest-wins**;
 * `format` / `pattern` keep the first value; `enum` is intersected.*
 * 
 * @author Ghaylan Saada
 */
internal object JsonSchemaFacetApplicator {
	
	/**
	 * Writes facets from [hints] onto [schema], merging with any values already present.
	 *
	 * Mutates [schema] facets when [hints] is non-empty. Never mutates [Schema.description].
	 *
	 * @param hints documentation payload for one constraint occurrence
	 * @param schema OpenAPI schema being enriched
	 * @return `true` when [JsonSchemaFacet.Required] was present (caller adds to parent `required`)	 
	 */
	fun apply(
		hints: ConstraintDocHints,
		schema: Schema<*>
	): Boolean {
		if (hints.facets.isEmpty()) return false
		var required = false
		for (facet in hints.facets) {
			when (facet) {
				is JsonSchemaFacet.MinLength -> {
					schema.minLength = maxOfNullable(schema.minLength, facet.value)
				}
				
				is JsonSchemaFacet.MaxLength -> {
					schema.maxLength = minOfNullable(schema.maxLength, facet.value)
				}
				
				is JsonSchemaFacet.MinItems -> {
					schema.minItems = maxOfNullable(schema.minItems, facet.value)
				}
				
				is JsonSchemaFacet.MaxItems -> {
					schema.maxItems = minOfNullable(schema.maxItems, facet.value)
				}
				
				is JsonSchemaFacet.MinProperties -> {
					schema.minProperties = maxOfNullable(schema.minProperties, facet.value)
				}
				
				is JsonSchemaFacet.MaxProperties -> {
					schema.maxProperties = minOfNullable(schema.maxProperties, facet.value)
				}
				
				is JsonSchemaFacet.Pattern -> {
					if (schema.pattern.isNullOrBlank()) {
						schema.pattern = facet.value
					}
				}
				
				is JsonSchemaFacet.EnumValues -> mergeEnum(schema, facet.values)
				is JsonSchemaFacet.NotEnumValues -> mergeNotEnum(schema, facet.values)
				is JsonSchemaFacet.Minimum -> mergeMinimum(schema, facet.value, facet.exclusive)
				is JsonSchemaFacet.Maximum -> mergeMaximum(schema, facet.value, facet.exclusive)
				is JsonSchemaFacet.MultipleOf -> {
					if (schema.multipleOf == null) {
						schema.multipleOf = facet.value
					}
				}
				
				is JsonSchemaFacet.Format -> {
					if (schema.format.isNullOrBlank()) {
						schema.format = facet.value
					}
				}
				
				is JsonSchemaFacet.NotNullable -> schema.nullable = false
				is JsonSchemaFacet.Required -> {
					required = true
					schema.nullable = false
				}
			}
		}
		return required
	}
	
	/**
	 * Inclusive lower-bound merge: keep the larger of [existing] and [incoming].
	 *
	 * @param existing current schema bound, or `null` if unset
	 * @param incoming candidate bound from a facet
	 * @return strictest lower bound	 
	 */
	private fun maxOfNullable(
		existing: Int?,
		incoming: Int
	): Int = if (existing == null) incoming else maxOf(existing, incoming)
	
	/**
	 * Inclusive upper-bound merge: keep the smaller of [existing] and [incoming].
	 *
	 * @param existing current schema bound, or `null` if unset
	 * @param incoming candidate bound from a facet
	 * @return strictest upper bound	 
	 */
	private fun minOfNullable(
		existing: Int?,
		incoming: Int
	): Int = if (existing == null) incoming else minOf(existing, incoming)
	
	/**
	 * Strictest-wins merge for `minimum` / `exclusiveMinimum`.
	 *
	 * Mutates [schema] when [value] is stricter or equal with exclusive.
	 *
	 * @param schema OpenAPI schema being enriched
	 * @param value candidate minimum
	 * @param exclusive whether the bound is exclusive	 
	 */
	private fun mergeMinimum(
		schema: Schema<*>,
		value: BigDecimal,
		exclusive: Boolean
	) {
		val current = schema.minimum
		when {
			current == null -> {
				schema.minimum = value
				schema.exclusiveMinimum = exclusive
			}
			
			value > current -> {
				schema.minimum = value
				schema.exclusiveMinimum = exclusive
			}
			
			value.compareTo(current) == 0 && exclusive -> {
				schema.exclusiveMinimum = true
			}
		}
	}
	
	/**
	 * Strictest-wins merge for `maximum` / `exclusiveMaximum`.
	 *
	 * Mutates [schema] when [value] is stricter or equal with exclusive.
	 *
	 * @param schema OpenAPI schema being enriched
	 * @param value candidate maximum
	 * @param exclusive whether the bound is exclusive	 
	 */
	private fun mergeMaximum(
		schema: Schema<*>,
		value: BigDecimal,
		exclusive: Boolean
	) {
		val current = schema.maximum
		when {
			current == null -> {
				schema.maximum = value
				schema.exclusiveMaximum = exclusive
			}
			
			value < current -> {
				schema.maximum = value
				schema.exclusiveMaximum = exclusive
			}
			
			value.compareTo(current) == 0 && exclusive -> {
				schema.exclusiveMaximum = true
			}
		}
	}
	
	/**
	 * Intersects existing `enum` with [values]; empty intersection keeps the incoming set.
	 *
	 * Mutates [schema] `enum`.
	 *
	 * @param schema OpenAPI schema being enriched
	 * @param values allowed string values from a facet
	 */
	@Suppress("UNCHECKED_CAST")
	private fun mergeEnum(
		schema: Schema<*>,
		values: List<String>
	) {
		val target = schema as Schema<Any?>
		val incoming = values.toSet()
		
		val existing = target.enum?.map {
			it?.toString().orEmpty()
		}?.toSet()
		
		val merged = when {
			existing.isNullOrEmpty() -> incoming
			else -> existing.intersect(incoming).ifEmpty { incoming }
		}
		target.enum = merged.sorted()
	}
	
	/**
	 * Unions forbidden values into OpenAPI `not.enum` (`not: { enum: […] }`).
	 *
	 * Mutates [schema] `not`. When [Schema.not] already carries an `enum`, values are unioned;
	 * otherwise a fresh `not` schema is created.
	 *
	 * @param schema OpenAPI schema being enriched
	 * @param values forbidden string values from a facet
	 */
	@Suppress("UNCHECKED_CAST")
	private fun mergeNotEnum(
		schema: Schema<*>,
		values: List<String>
	) {
		val incoming = values.toSet()
		val existingNot = schema.not as Schema<Any?>?
		val existing = existingNot?.enum?.map {
			it?.toString().orEmpty()
		}?.toSet().orEmpty()
		val merged = (existing + incoming).sorted()
		val notSchema = existingNot ?: Schema()
		notSchema.enum = merged
		schema.not = notSchema
	}
}
