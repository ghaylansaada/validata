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

import io.ghaylan.validata.constraint.annotation.MaxConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

/** Unit tests for [DurationMaxValidator].
 * 
 * @author Ghaylan Saada
 */
class DurationMaxValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MaxConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null duration")
		fun skipsNull() {
			assertSkipsNull(DurationMaxValidator, c("PT1H"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Max allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(DurationMaxValidator, Duration.ofHours(1), c("PT1H", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Max rejects equality with TEMPORAL_DURATION_TOO_LONG")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				DurationMaxValidator,
				Duration.ofHours(1),
				c("PT1H", inclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("duration below maximum passes")
		fun belowMaximumPasses() {
			assertValid(DurationMaxValidator, Duration.ofMinutes(30), c("PT1H"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("duration above maximum fails with TEMPORAL_DURATION_TOO_LONG")
		fun aboveMaximumFails() {
			assertInvalid(
				DurationMaxValidator,
				Duration.ofHours(2),
				c("PT1H"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
			)
		}
	}
	
	@Nested
	@DisplayName("invalid bound literal")
	inner class InvalidBoundLiteral {
		
		@Test
		@DisplayName("blank bound fails with VALUE_PARSING_FAILED")
		fun blankBoundFails() {
			assertInvalid(
				DurationMaxValidator,
				Duration.ZERO,
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unparseable bound fails with VALUE_PARSING_FAILED")
		fun unparseableBoundFails() {
			assertInvalid(
				DurationMaxValidator,
				Duration.ZERO,
				c("not-a-duration"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
