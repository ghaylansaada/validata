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

import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

/** Unit tests for [DurationMinValidator].
 * 
 * @author Ghaylan Saada
 */
class DurationMinValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MinConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null duration")
		fun skipsNull() {
			assertSkipsNull(DurationMinValidator, c("PT1H"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Min allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(DurationMinValidator, Duration.ofHours(1), c("PT1H", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Min rejects equality with TEMPORAL_DURATION_TOO_SHORT")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				DurationMinValidator,
				Duration.ofHours(1),
				c("PT1H", inclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("duration above minimum passes")
		fun aboveMinimumPasses() {
			assertValid(DurationMinValidator, Duration.ofHours(2), c("PT1H"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("duration below minimum fails with TEMPORAL_DURATION_TOO_SHORT")
		fun belowMinimumFails() {
			assertInvalid(
				DurationMinValidator,
				Duration.ofMinutes(30),
				c("PT1H"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
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
				DurationMinValidator,
				Duration.ZERO,
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unparseable bound fails with VALUE_PARSING_FAILED")
		fun unparseableBoundFails() {
			assertInvalid(
				DurationMinValidator,
				Duration.ZERO,
				c("not-a-duration"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
