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
import io.ghaylan.validata.constraint.annotation.AssertConstraint
import io.ghaylan.validata.constraint.annotation.ChecksumConstraint
import io.ghaylan.validata.constraint.annotation.RelativeToNowConstraint
import io.ghaylan.validata.constraint.annotation.ContainsConstraint
import io.ghaylan.validata.constraint.annotation.Coordinate
import io.ghaylan.validata.constraint.annotation.CoordinateConstraint
import io.ghaylan.validata.constraint.annotation.DistinctConstraint
import io.ghaylan.validata.constraint.annotation.HtmlConstraint
import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.ghaylan.validata.schema.shape.TypeShape
import java.math.BigDecimal

/**
 * Native OpenAPI facets for geo / presence / composition-adjacent custom constraints.
 *
 * Latitude / Longitude → inclusive geographic bounds. RelativeToNow / Html / Checksum /
 * Distinct / Assert / Contains have no honest native facets and stay extension-only under
 * `x-validata-constraints`.*
 * 
 * @author Ghaylan Saada
 */
class MiscCustomConstraintDocumentation: ConstraintDocumentation {
	
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata.javaClass in HANDLED
	
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints = when (metadata) {
		is CoordinateConstraint -> {
			val (min, max) = when (metadata.axis) {
				Coordinate.Axis.LATITUDE -> LATITUDE_MIN to LATITUDE_MAX
				Coordinate.Axis.LONGITUDE -> LONGITUDE_MIN to LONGITUDE_MAX
			}
			val facetMin = JsonSchemaFacet.Minimum(min, exclusive = false)
			val facetMax = JsonSchemaFacet.Maximum(max, exclusive = false)
			ConstraintDocHints(facets = setOf(facetMin, facetMax))
		}
		is RelativeToNowConstraint,
		is HtmlConstraint,
		is ChecksumConstraint,
		is DistinctConstraint,
		is AssertConstraint,
		is ContainsConstraint -> ConstraintDocHints.EMPTY
		else -> ConstraintDocHints.EMPTY
	}
	
	companion object {
		
		private val LATITUDE_MIN: BigDecimal = BigDecimal("-90")
		private val LATITUDE_MAX: BigDecimal = BigDecimal("90")
		private val LONGITUDE_MIN: BigDecimal = BigDecimal("-180")
		private val LONGITUDE_MAX: BigDecimal = BigDecimal("180")
		private val HANDLED: Set<Class<*>> = setOf(
			CoordinateConstraint::class.java,
			RelativeToNowConstraint::class.java,
			ContainsConstraint::class.java,
			HtmlConstraint::class.java,
			ChecksumConstraint::class.java,
			DistinctConstraint::class.java,
			AssertConstraint::class.java)
	}
}
