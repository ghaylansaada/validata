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

import io.ghaylan.validata.constraint.annotation.MaxConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Period

/** Unit tests for [PeriodMaxValidator].
 * 
 * @author Ghaylan Saada
 */
class PeriodMaxValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MaxConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null period")
		fun skipsNull() {
			assertSkipsNull(PeriodMaxValidator, c("P1Y"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Max allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(PeriodMaxValidator, Period.ofYears(1), c("P1Y", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Max rejects equality with TEMPORAL_DURATION_TOO_LONG")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				PeriodMaxValidator,
				Period.ofYears(1),
				c("P1Y", inclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("period below maximum passes")
		fun belowMaximumPasses() {
			assertValid(PeriodMaxValidator, Period.ofMonths(6), c("P1Y"))
		}
		
		@Test
		@DisplayName("shorter period with same month total but fewer days passes")
		fun sameMonthsFewerDaysPasses() {
			assertValid(PeriodMaxValidator, Period.of(0, 12, 0), c("P1Y1D"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("period above maximum fails with TEMPORAL_DURATION_TOO_LONG")
		fun aboveMaximumFails() {
			assertInvalid(
				PeriodMaxValidator,
				Period.ofYears(2),
				c("P1Y"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
			)
		}
		
		@Test
		@DisplayName("same month total but more days fails with TEMPORAL_DURATION_TOO_LONG")
		fun sameMonthsMoreDaysFails() {
			assertInvalid(
				PeriodMaxValidator,
				Period.of(0, 12, 5),
				c("P1Y"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
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
				PeriodMaxValidator,
				Period.ZERO,
				c(""),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
		
		@Test
		@DisplayName("unparseable bound fails with VALUE_PARSING_FAILED")
		fun unparseableBoundFails() {
			assertInvalid(
				PeriodMaxValidator,
				Period.ZERO,
				c("not-a-period"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
