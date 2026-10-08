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
package io.ghaylan.validata.openapi.docs.builtin

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.DaysOfWeekConstraint
import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Maps [DaysOfWeekConstraint] allow / deny day lists to OpenAPI `enum` / `not.enum`.
 *
 * Values are `java.time.DayOfWeek` names (`MONDAY`, …). The property wire type remains temporal;
 * args are also published under `x-validata-constraints`.*
 * 
 * @author Ghaylan Saada
 */
class DaysOfWeekConstraintDocumentation: ConstraintDocumentation {
	
	/**
	 * Reports whether [metadata] is handled by this documenter.
	 *
	 * @param metadata constraint metadata under consideration
	 * @return `true` when this documenter applies	 
	 */
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata is DaysOfWeekConstraint
	
	/**
	 * Builds OpenAPI facets for [metadata].
	 *
	 * Pure mapping; does not mutate [metadata] or [shape].
	 *
	 * @param metadata constraint metadata to document
	 * @param shape property or type-use shape, or null when unavailable
	 * @return sorted day names as `enum` (allow) or `not.enum` (deny)	 
	 */
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints {
		val days = metadata as DaysOfWeekConstraint
		val sorted = days.days.map { it.name }
		val facet = if (days.negated) {
			JsonSchemaFacet.NotEnumValues(sorted)
		} else {
			JsonSchemaFacet.EnumValues(sorted)
		}
		return ConstraintDocHints(facets = setOf(facet))
	}
}
