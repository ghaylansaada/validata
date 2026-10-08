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
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.MapShape
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Maps [SizeConstraint] to OpenAPI JSON Schema length / items / properties facets by [TypeShape].*
 * 
 * @author Ghaylan Saada
 */
class SizeConstraintDocumentation: ConstraintDocumentation {
	
	/**
	 * Reports whether [metadata] is handled by this documenter.
	 *
	 * @param metadata constraint metadata under consideration
	 * @return `true` when this documenter applies	 
	 */
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata is SizeConstraint
	
	/**
	 * Builds OpenAPI facets for [metadata] from [shape].
	 *
	 * Pure mapping; does not mutate [metadata] or [shape].
	 * Emits `minItems`/`maxItems` for [IterableShape], `minProperties`/`maxProperties` for [MapShape],
	 * otherwise `minLength`/`maxLength`. Omits max facets when max is [Int.MAX_VALUE].
	 *
	 * @param metadata constraint metadata to document
	 * @param shape property or type-use shape, or null when unavailable
	 * @return facets for the size bound; no extension hints	 
	 */
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints {
		val size = metadata as SizeConstraint
		val facets = when (shape) {
			is IterableShape -> buildSet {
				add(JsonSchemaFacet.MinItems(size.min))
				if (size.max != Int.MAX_VALUE) add(JsonSchemaFacet.MaxItems(size.max))
			}
			
			is MapShape -> buildSet {
				add(JsonSchemaFacet.MinProperties(size.min))
				if (size.max != Int.MAX_VALUE) add(JsonSchemaFacet.MaxProperties(size.max))
			}
			
			else -> buildSet {
				add(JsonSchemaFacet.MinLength(size.min))
				if (size.max != Int.MAX_VALUE) add(JsonSchemaFacet.MaxLength(size.max))
			}
		}
		return ConstraintDocHints(facets = facets)
	}
}
