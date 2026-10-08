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

import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Period

/** Unit tests for [PeriodRangeValidator].
 * 
 * @author Ghaylan Saada
 */
class PeriodRangeValidatorTest {
	
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
		@DisplayName("skips null period")
		fun skipsNull() {
			assertSkipsNull(PeriodRangeValidator, c("P1Y", "P5Y"))
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("period within inclusive P1Y..P5Y passes")
		fun withinInclusiveRangePasses() {
			assertValid(PeriodRangeValidator, Period.ofYears(3), c("P1Y", "P5Y"))
		}
		
		@Test
		@DisplayName("period at inclusive bounds passes")
		fun atInclusiveBoundsPasses() {
			assertValid(PeriodRangeValidator, Period.ofYears(1), c("P1Y", "P5Y"))
			assertValid(PeriodRangeValidator, Period.ofYears(5), c("P1Y", "P5Y"))
		}
	}
	
	@Nested
	@DisplayName("lower bound violations")
	inner class LowerBoundViolations {
		
		@Test
		@DisplayName("period below from fails with TEMPORAL_DURATION_TOO_SHORT")
		fun belowFromFails() {
			assertInvalid(
				PeriodRangeValidator,
				Period.ofMonths(6),
				c("P1Y", "P5Y"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
				c("P1Y", "P5Y"),
			)
		}
		
		@Test
		@DisplayName("exclusive from rejects equality with TEMPORAL_DURATION_TOO_SHORT")
		fun exclusiveFromRejectsEquality() {
			assertInvalid(
				PeriodRangeValidator,
				Period.ofYears(1),
				c("P1Y", "P5Y", fromInclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
			)
		}
	}
	
	@Nested
	@DisplayName("upper bound violations")
	inner class UpperBoundViolations {
		
		@Test
		@DisplayName("period above to fails with TEMPORAL_DURATION_TOO_LONG")
		fun aboveToFails() {
			assertInvalid(
				PeriodRangeValidator,
				Period.ofYears(6),
				c("P1Y", "P5Y"),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
				c("P1Y", "P5Y"),
			)
		}
		
		@Test
		@DisplayName("exclusive to rejects equality with TEMPORAL_DURATION_TOO_LONG")
		fun exclusiveToRejectsEquality() {
			assertInvalid(
				PeriodRangeValidator,
				Period.ofYears(5),
				c("P1Y", "P5Y", toInclusive = false),
				ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG,
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
				PeriodRangeValidator,
				Period.ZERO,
				c("", "P5Y"),
				ConstraintErrorCode.VALUE_PARSING_FAILED,
			)
		}
	}
}
