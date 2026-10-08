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
package io.ghaylan.validata.constraint.validator.bound.duration

import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

/** Unit tests for [DurationRangeValidator].
 * 
 * @author Ghaylan Saada
 */
class DurationRangeValidatorTest {
	
	private fun c(
		from: String,
		to: String,
		fromInclusive: Boolean = true,
		toInclusive: Boolean = true,
	) = RangeConstraint(from, to, fromInclusive, toInclusive, false, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null duration")
		fun skipsNull() {
			assertSkipsNull(DurationRangeValidator, c("PT30M", "PT8H"))
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("duration within inclusive PT30M..PT8H passes")
		fun withinInclusiveRangePasses() {
			assertValid(DurationRangeValidator, Duration.ofHours(2), c("PT30M", "PT8H"))
		}
		
		@Test
		@DisplayName("duration at inclusive bounds passes")
		fun atInclusiveBoundsPasses() {
			assertValid(DurationRangeValidator, Duration.ofMinutes(30), c("PT30M", "PT8H"))
			assertValid(DurationRangeValidator, Duration.ofHours(8), c("PT30M", "PT8H"))
		}
	}
	
	@Nested
	@DisplayName("lower bound violations")
	inner class LowerBoundViolations {
		
		@Test
		@DisplayName("duration below from fails with TEMPORAL_DURATION_TOO_SHORT")
		fun belowFromFails() {
			assertInvalid(
				DurationRangeValidator,
				Duration.ofMinutes(15),
				c("PT30M", "PT8H"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
				c("PT30M", "PT8H"),
			)
		}
		
		@Test
		@DisplayName("exclusive from rejects equality with TEMPORAL_DURATION_TOO_SHORT")
		fun exclusiveFromRejectsEquality() {
			assertInvalid(
				DurationRangeValidator,
				Duration.ofMinutes(30),
				c("PT30M", "PT8H", fromInclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
			)
		}
	}
	
	@Nested
	@DisplayName("upper bound violations")
	inner class UpperBoundViolations {
		
		@Test
		@DisplayName("duration above to fails with TEMPORAL_DURATION_TOO_LONG")
		fun aboveToFails() {
			assertInvalid(
				DurationRangeValidator,
				Duration.ofHours(9),
				c("PT30M", "PT8H"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
				c("PT30M", "PT8H"),
			)
		}
		
		@Test
		@DisplayName("exclusive to rejects equality with TEMPORAL_DURATION_TOO_LONG")
		fun exclusiveToRejectsEquality() {
			assertInvalid(
				DurationRangeValidator,
				Duration.ofHours(8),
				c("PT30M", "PT8H", toInclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
			)
		}
	}
	
	@Nested
	@DisplayName("invalid bound literal")
	inner class InvalidBoundLiteral {
		
		@Test
		@DisplayName("blank from fails with VALUE_PARSING_FAILED")
		fun blankFromFails() {
			assertInvalid(
				DurationRangeValidator,
				Duration.ZERO,
				c("", "PT8H"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
