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
package io.ghaylan.validata.constraint.validator.temporal.daysofweek

import io.ghaylan.validata.constraint.annotation.DaysOfWeekConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Unit tests for [DaysOfWeekValidator].
 * 
 * @author Ghaylan Saada
 */
class DaysOfWeekValidatorTest {
	
	private fun c(vararg days: DayOfWeek) = DaysOfWeekConstraint(setOf(*days), false, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null temporal")
		fun skipsNull() {
			assertSkipsNull(DaysOfWeekValidator, c(DayOfWeek.MONDAY))
		}
	}
	
	@Nested
	@DisplayName("allowed days")
	inner class AllowedDays {
		
		@Test
		@DisplayName("date on allowed day passes")
		fun allowedDayPasses() {
			assertValid(DaysOfWeekValidator, LocalDate.of(2026, 8, 3), c(DayOfWeek.MONDAY))
		}
		
		@Test
		@DisplayName("date-time on allowed day passes")
		fun allowedDateTimePasses() {
			assertValid(
				DaysOfWeekValidator,
				LocalDateTime.of(2026, 8, 3, 9, 0),
				c(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
			)
		}
	}
	
	@Nested
	@DisplayName("disallowed days")
	inner class DisallowedDays {
		
		@Test
		@DisplayName("date on disallowed day fails with TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED")
		fun disallowedDayFails() {
			assertInvalid(
				DaysOfWeekValidator,
				LocalDate.of(2026, 8, 2),
				c(DayOfWeek.MONDAY),
				ConstraintErrorCode.TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED,
			)
		}
	}
	
	@Nested
	@DisplayName("non-matching carrier")
	inner class NonMatchingCarrier {
		
		@Test
		@DisplayName("time-only temporal skips validation")
		fun timeOnlySkips() {
			assertValid(DaysOfWeekValidator, LocalTime.of(12, 0), c(DayOfWeek.MONDAY))
		}
	}
}
