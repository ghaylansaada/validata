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

import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SampleFloorValidator]: null-skip, inclusive floor, fail-closed parse.*
 * 
 * @author Ghaylan Saada
 */
class SampleFloorValidatorTest {
	
	/** Synthetic cursor; these validators do not read siblings.	 */
	private val context = SampleValidationContext()
	
	/** Generated metadata whose [SampleFloorConstraint.value] is the inclusive floor literal.	 */
	private fun constraint(value: String): SampleFloorConstraint =
		SampleFloorConstraint(value = value, message = "", groups = setOf(OnDefault::class))
	
	@Nested
	@DisplayName("Null input")
	inner class NullInput {
		
		@Test
		@DisplayName("null skips (presence is Required)")
				/** null skips (presence is Required)				 */
		fun nullSkips() {
			assertThat(SampleFloorValidator.runValidation(null, constraint("3"), context)).isNull()
		}
	}
	
	@Nested
	@DisplayName("Inclusive floor")
	inner class InclusiveFloor {
		
		@Test
		@DisplayName("value equal to floor 3 is accepted")
				/** value equal to floor 3 is accepted				 */
		fun equalToFloorPasses() {
			assertThat(SampleFloorValidator.runValidation(3, constraint("3"), context)).isNull()
		}
		
		@Test
		@DisplayName("value above floor is accepted")
				/** value above floor is accepted				 */
		fun aboveFloorPasses() {
			assertThat(SampleFloorValidator.runValidation(5, constraint("3"), context)).isNull()
		}
		
		@Test
		@DisplayName("value 2 below floor 3 fails with NUMBER_TOO_SMALL")
				/** value 2 below floor 3 fails with NUMBER_TOO_SMALL				 */
		fun belowFloorFails() {
			val error = SampleFloorValidator.runValidation(2, constraint("3"), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.NUMBER_TOO_SMALL)
		}
		
		@Test
		@DisplayName("value 1 below floor 3 fails with NUMBER_TOO_SMALL")
				/** value 1 below floor 3 fails with NUMBER_TOO_SMALL				 */
		fun wellBelowFloorFails() {
			val error = SampleFloorValidator.runValidation(1, constraint("3"), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.NUMBER_TOO_SMALL)
		}
	}
	
	@Nested
	@DisplayName("Fail-closed parse")
	inner class FailClosedParse {
		
		@Test
		@DisplayName("unparseable floor literal fails with VALUE_PARSING_FAILED")
				/** unparseable floor literal fails with VALUE_PARSING_FAILED				 */
		fun unparseableBoundFailsClosed() {
			val error = SampleFloorValidator.runValidation(10, constraint("not-a-number"), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
	}
}
