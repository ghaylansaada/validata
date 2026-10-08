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
package io.ghaylan.validata.constraint.validator.bound.period

import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Period

/** Unit tests for [PeriodMinValidator].
 * 
 * @author Ghaylan Saada
 */
class PeriodMinValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MinConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null period")
		fun skipsNull() {
			assertSkipsNull(PeriodMinValidator, c("P1Y"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Min allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(PeriodMinValidator, Period.ofYears(1), c("P1Y", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Min rejects equality with TEMPORAL_DURATION_TOO_SHORT")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				PeriodMinValidator,
				Period.ofYears(1),
				c("P1Y", inclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("period above minimum passes")
		fun aboveMinimumPasses() {
			assertValid(PeriodMinValidator, Period.ofYears(2), c("P1Y"))
		}
		
		@Test
		@DisplayName("longer period with same month total but more days passes")
		fun sameMonthsMoreDaysPasses() {
			assertValid(PeriodMinValidator, Period.of(0, 12, 5), c("P1Y"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("period below minimum fails with TEMPORAL_DURATION_TOO_SHORT")
		fun belowMinimumFails() {
			assertInvalid(
				PeriodMinValidator,
				Period.ofMonths(6),
				c("P1Y"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
			)
		}
		
		@Test
		@DisplayName("same month total but fewer days fails with TEMPORAL_DURATION_TOO_SHORT")
		fun sameMonthsFewerDaysFails() {
			assertInvalid(
				PeriodMinValidator,
				Period.of(0, 12, 0),
				c("P1Y1D"),
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
				PeriodMinValidator,
				Period.ZERO,
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unparseable bound fails with VALUE_PARSING_FAILED")
		fun unparseableBoundFails() {
			assertInvalid(
				PeriodMinValidator,
				Period.ZERO,
				c("not-a-period"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
