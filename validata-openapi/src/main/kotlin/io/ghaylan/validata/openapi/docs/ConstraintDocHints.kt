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

/**
 * Documentation payload for one constraint occurrence.
 *
 * Built-ins and apps produce this via [ConstraintDocumentation].
 * `io.ghaylan.validata.openapi.enrichment.JsonSchemaFacetApplicator` maps [facets] onto
 * swagger schemas and never writes `Schema.description` — that belongs to `@Schema` / authors.
 *
 * @property facets JSON Schema-oriented facets (length, pattern, enum, …)
 * 
 * @author Ghaylan Saada
 */
data class ConstraintDocHints(
	val facets: Set<JsonSchemaFacet> = emptySet(),
) {
	/**
	 * `true` when no facets are present.
	 */
	val isEmpty: Boolean get() = facets.isEmpty()
	
	/**
	 * Holds shared hint instances.
	 */
	companion object {
		
		/**
		 * Shared empty instance for “no documentation” outcomes.
		 */
		val EMPTY: ConstraintDocHints = ConstraintDocHints()
	}
}
