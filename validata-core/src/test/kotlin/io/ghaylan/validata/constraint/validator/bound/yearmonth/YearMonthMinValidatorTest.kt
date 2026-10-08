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

import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

/** Unit tests for [YearMonthMinValidator].
 * 
 * @author Ghaylan Saada
 */
class YearMonthMinValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MinConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null year-month")
		fun skipsNull() {
			assertSkipsNull(YearMonthMinValidator, c("2020-01"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Min allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(YearMonthMinValidator, YearMonth.of(2020, 1), c("2020-01", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Min rejects equality with TEMPORAL_TOO_EARLY")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				YearMonthMinValidator,
				YearMonth.of(2020, 1),
				c("2020-01", inclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("year-month after minimum passes")
		fun afterMinimumPasses() {
			assertValid(YearMonthMinValidator, YearMonth.of(2021, 6), c("2020-01"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("year-month before minimum fails with TEMPORAL_TOO_EARLY")
		fun beforeMinimumFails() {
			assertInvalid(
				YearMonthMinValidator,
				YearMonth.of(2019, 12),
				c("2020-01"),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
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
				YearMonthMinValidator,
				YearMonth.of(2020, 1),
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unparseable bound fails with VALUE_PARSING_FAILED")
		fun unparseableBoundFails() {
			assertInvalid(
				YearMonthMinValidator,
				YearMonth.of(2020, 1),
				c("not-a-year-month"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
