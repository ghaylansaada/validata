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
package io.ghaylan.validata.constraint.validator.bound.monthday

import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.MonthDay

/** Unit tests for [MonthDayRangeValidator].
 * 
 * @author Ghaylan Saada
 */
class MonthDayRangeValidatorTest {
	
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
		@DisplayName("skips null month-day")
		fun skipsNull() {
			assertSkipsNull(MonthDayRangeValidator, c("--06-01", "--09-30"))
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("month-day within inclusive --06-01..--09-30 passes")
		fun withinInclusiveRangePasses() {
			assertValid(MonthDayRangeValidator, MonthDay.of(7, 15), c("--06-01", "--09-30"))
		}
		
		@Test
		@DisplayName("month-day at inclusive bounds passes")
		fun atInclusiveBoundsPasses() {
			assertValid(MonthDayRangeValidator, MonthDay.of(6, 1), c("--06-01", "--09-30"))
			assertValid(MonthDayRangeValidator, MonthDay.of(9, 30), c("--06-01", "--09-30"))
		}
	}
	
	@Nested
	@DisplayName("lower bound violations")
	inner class LowerBoundViolations {
		
		@Test
		@DisplayName("month-day before from fails with TEMPORAL_TOO_EARLY")
		fun beforeFromFails() {
			assertInvalid(
				MonthDayRangeValidator,
				MonthDay.of(5, 31),
				c("--06-01", "--09-30"),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
				c("--06-01", "--09-30"),
			)
		}
		
		@Test
		@DisplayName("exclusive from rejects equality with TEMPORAL_TOO_EARLY")
		fun exclusiveFromRejectsEquality() {
			assertInvalid(
				MonthDayRangeValidator,
				MonthDay.of(6, 1),
				c("--06-01", "--09-30", fromInclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
		}
	}
	
	@Nested
	@DisplayName("upper bound violations")
	inner class UpperBoundViolations {
		
		@Test
		@DisplayName("month-day after to fails with TEMPORAL_TOO_LATE")
		fun afterToFails() {
			assertInvalid(
				MonthDayRangeValidator,
				MonthDay.of(10, 1),
				c("--06-01", "--09-30"),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
				c("--06-01", "--09-30"),
			)
		}
		
		@Test
		@DisplayName("exclusive to rejects equality with TEMPORAL_TOO_LATE")
		fun exclusiveToRejectsEquality() {
			assertInvalid(
				MonthDayRangeValidator,
				MonthDay.of(9, 30),
				c("--06-01", "--09-30", toInclusive = false),
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
				MonthDayRangeValidator,
				MonthDay.of(6, 1),
				c("", "--09-30"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
