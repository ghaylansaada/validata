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
package io.ghaylan.validata.constraint.validator.bound.yearmonth

import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

/** Unit tests for [YearMonthRangeValidator].
 * 
 * @author Ghaylan Saada
 */
class YearMonthRangeValidatorTest {
	
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
		@DisplayName("skips null year-month")
		fun skipsNull() {
			assertSkipsNull(YearMonthRangeValidator, c("2020-01", "2030-12"))
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("year-month within inclusive 2020-01..2030-12 passes")
		fun withinInclusiveRangePasses() {
			assertValid(YearMonthRangeValidator, YearMonth.of(2025, 6), c("2020-01", "2030-12"))
		}
		
		@Test
		@DisplayName("year-month at inclusive bounds passes")
		fun atInclusiveBoundsPasses() {
			assertValid(YearMonthRangeValidator, YearMonth.of(2020, 1), c("2020-01", "2030-12"))
			assertValid(YearMonthRangeValidator, YearMonth.of(2030, 12), c("2020-01", "2030-12"))
		}
	}
	
	@Nested
	@DisplayName("lower bound violations")
	inner class LowerBoundViolations {
		
		@Test
		@DisplayName("year-month before from fails with TEMPORAL_TOO_EARLY")
		fun beforeFromFails() {
			assertInvalid(
				YearMonthRangeValidator,
				YearMonth.of(2019, 12),
				c("2020-01", "2030-12"),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
				c("2020-01", "2030-12"),
			)
		}
		
		@Test
		@DisplayName("exclusive from rejects equality with TEMPORAL_TOO_EARLY")
		fun exclusiveFromRejectsEquality() {
			assertInvalid(
				YearMonthRangeValidator,
				YearMonth.of(2020, 1),
				c("2020-01", "2030-12", fromInclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
		}
	}
	
	@Nested
	@DisplayName("upper bound violations")
	inner class UpperBoundViolations {
		
		@Test
		@DisplayName("year-month after to fails with TEMPORAL_TOO_LATE")
		fun afterToFails() {
			assertInvalid(
				YearMonthRangeValidator,
				YearMonth.of(2031, 1),
				c("2020-01", "2030-12"),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
				c("2020-01", "2030-12"),
			)
		}
		
		@Test
		@DisplayName("exclusive to rejects equality with TEMPORAL_TOO_LATE")
		fun exclusiveToRejectsEquality() {
			assertInvalid(
				YearMonthRangeValidator,
				YearMonth.of(2030, 12),
				c("2020-01", "2030-12", toInclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
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
				YearMonthRangeValidator,
				YearMonth.of(2020, 1),
				c("", "2030-12"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
