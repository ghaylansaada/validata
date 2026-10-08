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
import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Unit tests for [SampleFloorConstraintDocumentation]: inclusive `minimum` facet.*
 * 
 * @author Ghaylan Saada
 */
class SampleFloorConstraintDocumentationTest {
	
	private val docs = SampleFloorConstraintDocumentation()
	
	private fun floor(value: String): SampleFloorConstraint = SampleFloorConstraint(value = value, message = "", groups = setOf(OnDefault::class))
	
	@Nested
	@DisplayName("supports")
	inner class Supports {
		
		@Test
		@DisplayName("supports SampleFloorConstraint")
				/** supports SampleFloorConstraint				 */
		fun supportsSampleFloor() {
			assertThat(docs.supports(floor("3"))).isTrue()
		}
		
		@Test
		@DisplayName("does not support unrelated metadata")
				/** does not support unrelated metadata				 */
		fun rejectsOtherMetadata() {
			val other = MinConstraint("1", true, "", setOf(OnDefault::class))
			assertThat(docs.supports(other)).isFalse()
		}
	}
	
	@Nested
	@DisplayName("hints")
	inner class Hints {
		
		@Test
		@DisplayName("publishes inclusive minimum when the bound parses")
				/** publishes inclusive minimum when the bound parses				 */
		fun inclusiveMinimum() {
			val hints = docs.hints(floor("3"), null)
			assertThat(hints.facets).containsExactly(
				JsonSchemaFacet.Minimum(BigDecimal("3"), exclusive = false),
			)
		}
		
		@Test
		@DisplayName("unparseable bound still maps (empty facets, not unmapped)")
				/** unparseable bound still maps (empty facets, not unmapped)				 */
		fun unparseableBoundEmptyHints() {
			assertThat(docs.hints(floor("not-a-number"), null)).isEqualTo(ConstraintDocHints.EMPTY)
		}
	}
}
