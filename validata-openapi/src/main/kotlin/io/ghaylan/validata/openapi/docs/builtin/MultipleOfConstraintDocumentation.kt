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

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.MultipleOfConstraint
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.ghaylan.validata.openapi.docs.NumericBound
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Maps [MultipleOfConstraint] to OpenAPI `multipleOf` when the factor parses as a number.
 *
 * Non-numeric factors yield empty facets; they still appear under `x-validata-constraints`
 * (and are not `x-validata-unmapped`).*
 * 
 * @author Ghaylan Saada
 */
class MultipleOfConstraintDocumentation: ConstraintDocumentation {
	
	/**
	 * Reports whether [metadata] is handled by this documenter.
	 *
	 * @param metadata constraint metadata under consideration
	 * @return `true` when this documenter applies	 
	 */
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata is MultipleOfConstraint
	
	/**
	 * Builds OpenAPI facets for [metadata].
	 *
	 * Pure mapping; does not mutate [metadata] or [shape].
	 *
	 * @param metadata constraint metadata to document
	 * @param shape property or type-use shape, or null when unavailable
	 * @return `multipleOf` facet; empty when the factor is not numeric	 
	 */
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints {
		val multiple = metadata as MultipleOfConstraint
		val factor = NumericBound.parse(multiple.factor) ?: return ConstraintDocHints.EMPTY
		val facet = JsonSchemaFacet.MultipleOf(factor)
		return ConstraintDocHints(facets = setOf(facet))
	}
}
