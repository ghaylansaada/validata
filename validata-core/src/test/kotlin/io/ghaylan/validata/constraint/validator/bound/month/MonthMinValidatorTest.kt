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
package io.ghaylan.validata.constraint.validator.bound.month

import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Month

/** Unit tests for [MonthMinValidator].
 * 
 * @author Ghaylan Saada
 */
class MonthMinValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MinConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null month")
		fun skipsNull() {
			assertSkipsNull(MonthMinValidator, c("MARCH"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Min allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(MonthMinValidator, Month.MARCH, c("MARCH", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Min rejects equality with TEMPORAL_TOO_EARLY")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				MonthMinValidator,
				Month.MARCH,
				c("MARCH", inclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("month after minimum passes")
		fun afterMinimumPasses() {
			assertValid(MonthMinValidator, Month.JUNE, c("MARCH"))
		}
		
		@Test
		@DisplayName("numeric month literal is accepted")
		fun numericLiteralPasses() {
			assertValid(MonthMinValidator, Month.MARCH, c("3"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("month before minimum fails with TEMPORAL_TOO_EARLY")
		fun beforeMinimumFails() {
			assertInvalid(
				MonthMinValidator,
				Month.JANUARY,
				c("MARCH"),
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
				MonthMinValidator,
				Month.MARCH,
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("out-of-range month number fails with VALUE_PARSING_FAILED")
		fun outOfRangeNumberFails() {
			assertInvalid(
				MonthMinValidator,
				Month.MARCH,
				c("13"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unknown month name fails with VALUE_PARSING_FAILED")
		fun unknownNameFails() {
			assertInvalid(
				MonthMinValidator,
				Month.MARCH,
				c("NOTAMONTH"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
