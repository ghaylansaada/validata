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
package io.ghaylan.validata.constraint.validator.number.range

import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Unit tests for [NumberRangeValidator].
 * 
 * @author Ghaylan Saada
 */
class NumberRangeValidatorTest {
	
	private fun constraint(
		from: String,
		to: String,
		fromInclusive: Boolean = true,
		toInclusive: Boolean = true,
		negated: Boolean = false,
	): RangeConstraint = RangeConstraint(from, to, fromInclusive, toInclusive, negated, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("Null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value skips validation")
		fun nullValueSkipsValidation() {
			assertSkipsNull(NumberRangeValidator, constraint("10", "100"))
		}
	}
	
	@Nested
	@DisplayName("Inclusive defaults")
	inner class InclusiveDefaults {
		
		@Test
		@DisplayName("value at lower bound passes with default inclusivity")
		fun valueAtLowerBoundPasses() {
			assertValid(NumberRangeValidator, 10, constraint("10", "100"))
		}
		
		@Test
		@DisplayName("value at upper bound passes with default inclusivity")
		fun valueAtUpperBoundPasses() {
			assertValid(NumberRangeValidator, 100, constraint("10", "100"))
		}
		
		@Test
		@DisplayName("value within range passes")
		fun valueWithinRangePasses() {
			assertValid(NumberRangeValidator, 50, constraint("10", "100"))
		}
	}
	
	@Nested
	@DisplayName("Lower bound violations")
	inner class LowerBoundViolations {
		
		@Test
		@DisplayName("value below from fails with NUMBER_TOO_SMALL")
		fun valueBelowFromFails() {
			assertInvalid(
				NumberRangeValidator,
				9,
				constraint("10", "100"),
				ConstraintErrorCode.NUMBER_TOO_SMALL,
				constraint("10", "100"),
			)
		}
		
		@Test
		@DisplayName("value equal to exclusive from fails with NUMBER_TOO_SMALL")
		fun exclusiveFromRejectsEquality() {
			assertInvalid(
				NumberRangeValidator,
				10,
				constraint("10", "100", fromInclusive = false),
				ConstraintErrorCode.NUMBER_TOO_SMALL,
			)
		}
	}
	
	@Nested
	@DisplayName("Upper bound violations")
	inner class UpperBoundViolations {
		
		@Test
		@DisplayName("value above to fails with NUMBER_TOO_LARGE")
		fun valueAboveToFails() {
			assertInvalid(
				NumberRangeValidator,
				101,
				constraint("10", "100"),
				ConstraintErrorCode.NUMBER_TOO_LARGE,
				constraint("10", "100"),
			)
		}
		
		@Test
		@DisplayName("value equal to exclusive to fails with NUMBER_TOO_LARGE")
		fun exclusiveToRejectsEquality() {
			assertInvalid(
				NumberRangeValidator,
				100,
				constraint("10", "100", toInclusive = false),
				ConstraintErrorCode.NUMBER_TOO_LARGE,
			)
		}
	}
	
	@Nested
	@DisplayName("Literal parsing")
	inner class LiteralParsing {
		
		@Test
		@DisplayName("decimal bounds compare correctly")
		fun decimalBoundsCompareCorrectly() {
			assertValid(NumberRangeValidator, BigDecimal("1.5"), constraint("1.0", "2.0"))
			assertInvalid(
				NumberRangeValidator,
				BigDecimal("0.9"),
				constraint("1.0", "2.0"),
				ConstraintErrorCode.NUMBER_TOO_SMALL,
			)
			assertInvalid(
				NumberRangeValidator,
				BigDecimal("2.1"),
				constraint("1.0", "2.0"),
				ConstraintErrorCode.NUMBER_TOO_LARGE,
			)
		}
	}

	@Nested
	@DisplayName("negated")
	inner class Negated {

		@Test
		@DisplayName("value inside range fails with NUMBER_OUT_OF_RANGE")
		fun insideFails() {
			assertInvalid(
				NumberRangeValidator,
				25,
				constraint("18", "120", negated = true),
				ConstraintErrorCode.NUMBER_OUT_OF_RANGE,
			)
		}

		@Test
		@DisplayName("value outside range passes")
		fun outsidePasses() {
			assertValid(NumberRangeValidator, 10, constraint("18", "120", negated = true))
			assertValid(NumberRangeValidator, 200, constraint("18", "120", negated = true))
		}
	}
}
