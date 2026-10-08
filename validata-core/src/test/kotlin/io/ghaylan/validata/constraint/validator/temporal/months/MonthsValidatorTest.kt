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
package io.ghaylan.validata.constraint.validator.temporal.months

import io.ghaylan.validata.constraint.annotation.MonthsConstraint
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
import java.time.Month
import java.time.YearMonth

/** Unit tests for [MonthsValidator].
 * 
 * @author Ghaylan Saada
 */
class MonthsValidatorTest {
	
	private fun c(vararg months: Month) = MonthsConstraint(setOf(*months), false, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null temporal")
		fun skipsNull() {
			assertSkipsNull(MonthsValidator, c(Month.JUNE))
		}
	}
	
	@Nested
	@DisplayName("allowed months")
	inner class AllowedMonths {
		
		@Test
		@DisplayName("date in allowed month passes")
		fun allowedMonthPasses() {
			assertValid(MonthsValidator, LocalDate.of(2026, 6, 15), c(Month.JUNE, Month.JULY))
		}
		
		@Test
		@DisplayName("YearMonth in allowed month passes")
		fun yearMonthPasses() {
			assertValid(MonthsValidator, YearMonth.of(2026, 7), c(Month.JUNE, Month.JULY))
		}
	}
	
	@Nested
	@DisplayName("disallowed months")
	inner class DisallowedMonths {
		
		@Test
		@DisplayName("date in disallowed month fails with TEMPORAL_MONTH_NOT_ALLOWED")
		fun disallowedMonthFails() {
			assertInvalid(
				MonthsValidator,
				LocalDate.of(2026, 1, 15),
				c(Month.JUNE, Month.JULY),
				ConstraintErrorCode.TEMPORAL_MONTH_NOT_ALLOWED,
			)
		}
	}
	
	@Nested
	@DisplayName("non-matching carrier")
	inner class NonMatchingCarrier {
		
		@Test
		@DisplayName("time-only temporal skips validation")
		fun timeOnlySkips() {
			assertValid(MonthsValidator, LocalTime.of(12, 0), c(Month.JUNE))
		}
	}
}
