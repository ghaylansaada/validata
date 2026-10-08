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
package io.ghaylan.validata.constraint.validator.temporal.daysofmonth

import io.ghaylan.validata.constraint.annotation.DaysOfMonthConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/** Unit tests for [DaysOfMonthValidator].
 * 
 * @author Ghaylan Saada
 */
class DaysOfMonthValidatorTest {
	
	private fun c(vararg days: Int, negated: Boolean = false) =
		DaysOfMonthConstraint(setOf(*days.toTypedArray()), negated, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null temporal")
		fun skipsNull() {
			assertSkipsNull(DaysOfMonthValidator, c(1, 15))
		}
	}
	
	@Nested
	@DisplayName("allowed days")
	inner class AllowedDays {
		
		@Test
		@DisplayName("date on allowed day-of-month passes")
		fun allowedDayPasses() {
			assertValid(DaysOfMonthValidator, LocalDate.of(2026, 8, 15), c(1, 15))
		}
	}
	
	@Nested
	@DisplayName("disallowed days")
	inner class DisallowedDays {
		
		@Test
		@DisplayName("date on disallowed day fails with TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED")
		fun disallowedDayFails() {
			assertInvalid(
				DaysOfMonthValidator,
				LocalDate.of(2026, 8, 2),
				c(1, 15),
				ConstraintErrorCode.TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED,
			)
		}
	}
	
	@Nested
	@DisplayName("negated")
	inner class Negated {
		
		@Test
		@DisplayName("day in deny-list fails")
		fun deniedDayFails() {
			assertInvalid(
				DaysOfMonthValidator,
				LocalDate.of(2026, 8, 13),
				c(13, negated = true),
				ConstraintErrorCode.TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("day outside deny-list passes")
		fun otherDayPasses() {
			assertValid(DaysOfMonthValidator, LocalDate.of(2026, 8, 15), c(13, negated = true))
		}
	}
	
	@Nested
	@DisplayName("non-matching carrier")
	inner class NonMatchingCarrier {
		
		@Test
		@DisplayName("time-only temporal skips validation")
		fun timeOnlySkips() {
			assertValid(DaysOfMonthValidator, LocalTime.of(12, 0), c(1))
		}
	}
}
