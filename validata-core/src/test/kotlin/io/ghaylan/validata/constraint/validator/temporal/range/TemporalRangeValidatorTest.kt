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
package io.ghaylan.validata.constraint.validator.temporal.range

import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Unit tests for [TemporalRangeValidator].
 * 
 * @author Ghaylan Saada
 */
class TemporalRangeValidatorTest {
	
	private fun constraint(
		from: String,
		to: String,
		fromInclusive: Boolean = true,
		toInclusive: Boolean = true,
	) = RangeConstraint(from, to, fromInclusive, toInclusive, false, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null temporal")
		fun skipsNull() {
			assertSkipsNull(TemporalRangeValidator, constraint("2020-01-01", "2030-12-31"))
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("date within inclusive range passes")
		fun withinInclusiveRangePasses() {
			assertValid(
				TemporalRangeValidator,
				LocalDate.of(2025, 6, 1),
				constraint("2020-01-01", "2030-12-31"),
			)
		}
		
		@Test
		@DisplayName("date at inclusive bounds passes")
		fun atInclusiveBoundsPasses() {
			assertValid(
				TemporalRangeValidator,
				LocalDate.of(2020, 1, 1),
				constraint("2020-01-01", "2030-12-31"),
			)
			assertValid(
				TemporalRangeValidator,
				LocalDate.of(2030, 12, 31),
				constraint("2020-01-01", "2030-12-31"),
			)
		}
	}
	
	@Nested
	@DisplayName("lower bound violations")
	inner class LowerBoundViolations {
		
		@Test
		@DisplayName("date before from fails with TEMPORAL_TOO_EARLY")
		fun beforeFromFails() {
			assertInvalid(
				TemporalRangeValidator,
				LocalDate.of(2019, 12, 31),
				constraint("2020-01-01", "2030-12-31"),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
		}
		
		@Test
		@DisplayName("exclusive from rejects equality with TEMPORAL_TOO_EARLY")
		fun exclusiveFromRejectsEquality() {
			assertInvalid(
				TemporalRangeValidator,
				LocalDate.of(2020, 1, 1),
				constraint("2020-01-01", "2030-12-31", fromInclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
		}
	}
	
	@Nested
	@DisplayName("upper bound violations")
	inner class UpperBoundViolations {
		
		@Test
		@DisplayName("date after to fails with TEMPORAL_TOO_LATE")
		fun afterToFails() {
			assertInvalid(
				TemporalRangeValidator,
				LocalDate.of(2031, 1, 1),
				constraint("2020-01-01", "2030-12-31"),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
		}
		
		@Test
		@DisplayName("exclusive to rejects equality with TEMPORAL_TOO_LATE")
		fun exclusiveToRejectsEquality() {
			assertInvalid(
				TemporalRangeValidator,
				LocalDate.of(2030, 12, 31),
				constraint("2020-01-01", "2030-12-31", toInclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
		}
	}
}
