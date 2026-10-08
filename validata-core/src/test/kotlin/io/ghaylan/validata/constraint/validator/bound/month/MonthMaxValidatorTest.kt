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

import io.ghaylan.validata.constraint.annotation.MaxConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Month

/** Unit tests for [MonthMaxValidator].
 * 
 * @author Ghaylan Saada
 */
class MonthMaxValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MaxConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null month")
		fun skipsNull() {
			assertSkipsNull(MonthMaxValidator, c("JUNE"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Max allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(MonthMaxValidator, Month.JUNE, c("JUNE", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Max rejects equality with TEMPORAL_TOO_LATE")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				MonthMaxValidator,
				Month.JUNE,
				c("JUNE", inclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("month before maximum passes")
		fun beforeMaximumPasses() {
			assertValid(MonthMaxValidator, Month.MARCH, c("JUNE"))
		}
		
		@Test
		@DisplayName("numeric month literal is accepted")
		fun numericLiteralPasses() {
			assertValid(MonthMaxValidator, Month.JUNE, c("6"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("month after maximum fails with TEMPORAL_TOO_LATE")
		fun afterMaximumFails() {
			assertInvalid(
				MonthMaxValidator,
				Month.DECEMBER,
				c("JUNE"),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
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
				MonthMaxValidator,
				Month.JUNE,
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("out-of-range month number fails with VALUE_PARSING_FAILED")
		fun outOfRangeNumberFails() {
			assertInvalid(
				MonthMaxValidator,
				Month.JUNE,
				c("0"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unknown month name fails with VALUE_PARSING_FAILED")
		fun unknownNameFails() {
			assertInvalid(
				MonthMaxValidator,
				Month.JUNE,
				c("NOTAMONTH"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
