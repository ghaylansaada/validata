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
package io.ghaylan.validata.openapi.docs

import java.math.BigDecimal

/**
 * JSON Schema / OpenAPI vocabulary for documenting a constraint occurrence.
 *
 * Applied by `io.ghaylan.validata.openapi.enrichment.JsonSchemaFacetApplicator`. Unknown facet
 * types must be ignored by consumers so the model stays forward-compatible.
 * 
 * @author Ghaylan Saada
 */
sealed interface JsonSchemaFacet {
	
	/**
	 * Minimum string length (`minLength`).
	 *
	 * @property value inclusive minimum character count
	 */
	data class MinLength(val value: Int): JsonSchemaFacet
	
	/**
	 * Maximum string length (`maxLength`).
	 *
	 * @property value inclusive maximum character count
	 */
	data class MaxLength(val value: Int): JsonSchemaFacet
	
	/**
	 * Minimum array size (`minItems`).
	 *
	 * @property value inclusive minimum element count
	 */
	data class MinItems(val value: Int): JsonSchemaFacet
	
	/**
	 * Maximum array size (`maxItems`).
	 *
	 * @property value inclusive maximum element count
	 */
	data class MaxItems(val value: Int): JsonSchemaFacet
	
	/**
	 * Minimum object property count (`minProperties`).
	 *
	 * @property value inclusive minimum property count
	 */
	data class MinProperties(val value: Int): JsonSchemaFacet
	
	/**
	 * Maximum object property count (`maxProperties`).
	 *
	 * @property value inclusive maximum property count
	 */
	data class MaxProperties(val value: Int): JsonSchemaFacet
	
	/**
	 * Regex pattern (`pattern`).
	 *
	 * @property value ECMA-262 / JSON Schema pattern string
	 */
	data class Pattern(val value: String): JsonSchemaFacet
	
	/**
	 * Allowed string values (`enum`).
	 *
	 * @property values distinct permitted literals (documenters typically sort them)
	 */
	data class EnumValues(val values: List<String>): JsonSchemaFacet
	
	/**
	 * Forbidden string values (`not.enum`).
	 *
	 * Used for deny-list membership (`@NotIn`, negated `@Months` / `@DaysOfWeek` /
	 * `@DaysOfMonth`). Applied as OpenAPI `not: { enum: […] }`.
	 *
	 * @property values distinct forbidden literals (documenters typically sort them)
	 */
	data class NotEnumValues(val values: List<String>): JsonSchemaFacet
	
	/**
	 * Numeric lower bound.
	 *
	 * @property value inclusive or exclusive floor
	 * @property exclusive when `true`, maps to `exclusiveMinimum`
	 */
	data class Minimum(
		val value: BigDecimal,
		val exclusive: Boolean = false
	): JsonSchemaFacet
	
	/**
	 * Numeric upper bound.
	 *
	 * @property value inclusive or exclusive ceiling
	 * @property exclusive when `true`, maps to `exclusiveMaximum`
	 */
	data class Maximum(
		val value: BigDecimal,
		val exclusive: Boolean = false
	): JsonSchemaFacet
	
	/**
	 * Numeric multiple (`multipleOf`).
	 *
	 * @property value strictly positive factor
	 */
	data class MultipleOf(val value: BigDecimal): JsonSchemaFacet
	
	/**
	 * OpenAPI / JSON Schema `format` (e.g. `email`, `uri`, `date`, `date-time`).
	 *
	 * @property value format token written onto the schema
	 */
	data class Format(val value: String): JsonSchemaFacet
	
	/**
	 * Marks the property as required on its parent object / parameter.
	 */
	data object Required: JsonSchemaFacet
	
	/**
	 * Property must not be null (`nullable = false` in OpenAPI).
	 */
	data object NotNullable: JsonSchemaFacet
}
