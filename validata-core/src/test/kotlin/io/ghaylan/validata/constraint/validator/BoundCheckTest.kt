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
package io.ghaylan.validata.constraint.validator

import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Unit tests for [BoundCheck] inclusive/exclusive comparisons and error builders.
 * 
 * @author Ghaylan Saada
 */
class BoundCheckTest {
	
	@Nested
	@DisplayName("violatesMin")
	inner class ViolatesMin {
		
		@Test
		@DisplayName("inclusive minimum allows equality")
		fun inclusiveAllowsEqual() {
			assertThat(BoundCheck.violatesMin(10, 10, inclusive = true)).isFalse()
		}
		
		@Test
		@DisplayName("inclusive minimum rejects values below bound")
		fun inclusiveRejectsBelow() {
			assertThat(BoundCheck.violatesMin(9, 10, inclusive = true)).isTrue()
		}
		
		@Test
		@DisplayName("exclusive minimum rejects equality")
		fun exclusiveRejectsEqual() {
			assertThat(BoundCheck.violatesMin(10, 10, inclusive = false)).isTrue()
		}
		
		@Test
		@DisplayName("exclusive minimum rejects values below bound")
		fun exclusiveRejectsBelow() {
			assertThat(BoundCheck.violatesMin(9, 10, inclusive = false)).isTrue()
		}
		
		@Test
		@DisplayName("works for temporal comparables")
		fun temporalComparable() {
			val bound = LocalDate.of(2024, 6, 1)
			assertThat(BoundCheck.violatesMin(LocalDate.of(2024, 5, 31), bound, inclusive = true)).isTrue()
			assertThat(BoundCheck.violatesMin(bound, bound, inclusive = true)).isFalse()
		}
	}
	
	@Nested
	@DisplayName("violatesMax")
	inner class ViolatesMax {
		
		@Test
		@DisplayName("inclusive maximum allows equality")
		fun inclusiveAllowsEqual() {
			assertThat(BoundCheck.violatesMax(10, 10, inclusive = true)).isFalse()
		}
		
		@Test
		@DisplayName("inclusive maximum rejects values above bound")
		fun inclusiveRejectsAbove() {
			assertThat(BoundCheck.violatesMax(11, 10, inclusive = true)).isTrue()
		}
		
		@Test
		@DisplayName("exclusive maximum rejects equality")
		fun exclusiveRejectsEqual() {
			assertThat(BoundCheck.violatesMax(10, 10, inclusive = false)).isTrue()
		}
		
		@Test
		@DisplayName("exclusive maximum rejects values above bound")
		fun exclusiveRejectsAbove() {
			assertThat(BoundCheck.violatesMax(11, 10, inclusive = false)).isTrue()
		}
		
		@Test
		@DisplayName("works for temporal comparables")
		fun temporalComparable() {
			val bound = LocalDate.of(2024, 6, 1)
			assertThat(BoundCheck.violatesMax(LocalDate.of(2024, 6, 2), bound, inclusive = true)).isTrue()
			assertThat(BoundCheck.violatesMax(bound, bound, inclusive = true)).isFalse()
		}
	}
	
	@Nested
	@DisplayName("minError")
	inner class MinError {
		
		@Test
		@DisplayName("builds NUMBER_TOO_SMALL with bound message and attached constraint")
		fun numberTooSmall() {
			val min = BigDecimal("10")
			val constraint = "min-constraint"
			val error = BoundCheck.minError(
				metadata = constraint,
				min = min,
				inclusive = true,
				code = ConstraintErrorCode.NUMBER_TOO_SMALL,
			)
			assertThat(error.code).isEqualTo(ConstraintErrorCode.NUMBER_TOO_SMALL)
			assertThat(error.message).isEqualTo("Must be at least $min.")
			assertThat(error.metadata).isSameAs(constraint)
		}
		
		@Test
		@DisplayName("builds TEMPORAL_TOO_EARLY with bound message and attached constraint")
		fun temporalTooEarly() {
			val min = LocalDate.of(2024, 1, 1)
			val constraint = "temporal-min-constraint"
			val error = BoundCheck.minError(
				metadata = constraint,
				min = min,
				inclusive = false,
				code = ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
			assertThat(error.code).isEqualTo(ConstraintErrorCode.TEMPORAL_TOO_EARLY)
			assertThat(error.message).isEqualTo("Must be after $min.")
			assertThat(error.metadata).isSameAs(constraint)
		}
	}
	
	@Nested
	@DisplayName("maxError")
	inner class MaxError {
		
		@Test
		@DisplayName("builds NUMBER_TOO_LARGE with bound message and attached constraint")
		fun numberTooLarge() {
			val max = BigDecimal("100")
			val constraint = "max-constraint"
			val error = BoundCheck.maxError(
				metadata = constraint,
				max = max,
				inclusive = false,
				code = ConstraintErrorCode.NUMBER_TOO_LARGE,
			)
			assertThat(error.code).isEqualTo(ConstraintErrorCode.NUMBER_TOO_LARGE)
			assertThat(error.message).isEqualTo("Must be less than $max.")
			assertThat(error.metadata).isSameAs(constraint)
		}
		
		@Test
		@DisplayName("builds TEMPORAL_TOO_LATE with bound message and attached constraint")
		fun temporalTooLate() {
			val max = LocalDate.of(2024, 12, 31)
			val constraint = "temporal-max-constraint"
			val error = BoundCheck.maxError(
				metadata = constraint,
				max = max,
				inclusive = true,
				code = ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
			assertThat(error.code).isEqualTo(ConstraintErrorCode.TEMPORAL_TOO_LATE)
			assertThat(error.message).isEqualTo("Must be on or before $max.")
			assertThat(error.metadata).isSameAs(constraint)
		}
	}
}
