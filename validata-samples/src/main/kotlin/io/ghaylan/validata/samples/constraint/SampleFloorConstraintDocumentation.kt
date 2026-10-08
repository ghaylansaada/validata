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
package io.ghaylan.validata.samples.constraint

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.ghaylan.validata.schema.shape.TypeShape
import java.math.BigDecimal

/**
 * Sample [ConstraintDocumentation] for [SampleFloorConstraint].
 *
 * Publishes an inclusive OpenAPI `minimum` when the floor literal parses as a decimal, so the
 * property is not listed under `x-validata-unmapped`. The constraint still appears as a
 * `SampleFloor` entry under `x-validata-constraints`.*
 * 
 * @author Ghaylan Saada
 */
class SampleFloorConstraintDocumentation: ConstraintDocumentation {
	
	/**
	 * @param metadata constraint metadata from the schema IR
	 * @return `true` when [metadata] is [SampleFloorConstraint]	 
	 */
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata is SampleFloorConstraint
	
	/**
	 * @param metadata must be [SampleFloorConstraint] when [supports] was true
	 * @param shape unused; the floor is a number regardless of declared type
	 * @return inclusive `minimum` when [SampleFloorConstraint.value] parses; otherwise empty
	 *   hints (still mapped — this documenter [supports] the type)	 
	 */
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints {
		val floor = metadata as SampleFloorConstraint
		val bound = parseBound(floor.value)
			?: return ConstraintDocHints.EMPTY
		return ConstraintDocHints(
			facets = setOf(JsonSchemaFacet.Minimum(bound, exclusive = false)),
		)
	}
	
	/**
	 * @param raw [SampleFloorConstraint.value] literal
	 * @return parsed inclusive floor, or `null` when [raw] is not a decimal	 
	 */
	private fun parseBound(raw: String): BigDecimal? = runCatching { BigDecimal(raw.trim()) }.getOrNull()
}
