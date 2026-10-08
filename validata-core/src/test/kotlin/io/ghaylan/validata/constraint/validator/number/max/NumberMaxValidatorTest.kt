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
package io.ghaylan.validata.constraint.validator.number.max

import io.ghaylan.validata.constraint.annotation.MaxConstraint
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
 * Unit tests for [NumberMaxValidator].
 * 
 * @author Ghaylan Saada
 */
class NumberMaxValidatorTest {
	
	private fun constraint(
		value: String,
		inclusive: Boolean = true
	): MaxConstraint = MaxConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("Null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value skips validation")
		fun nullValueSkipsValidation() {
			assertSkipsNull(NumberMaxValidator, constraint("10"))
		}
	}
	
	@Nested
	@DisplayName("Inclusive maximum")
	inner class InclusiveMaximum {
		
		@Test
		@DisplayName("value equal to maximum passes")
		fun valueEqualToMaximumPasses() {
			assertValid(NumberMaxValidator, 10, constraint("10", inclusive = true))
		}
		
		@Test
		@DisplayName("value below maximum passes")
		fun valueBelowMaximumPasses() {
			assertValid(NumberMaxValidator, 9, constraint("10", inclusive = true))
		}
		
		@Test
		@DisplayName("value above maximum fails with NUMBER_TOO_LARGE")
		fun valueAboveMaximumFails() {
			assertInvalid(
				NumberMaxValidator,
				11,
				constraint("10", inclusive = true),
				ConstraintErrorCode.NUMBER_TOO_LARGE,
				constraint("10", inclusive = true),
			)
		}
	}
	
	@Nested
	@DisplayName("Exclusive maximum")
	inner class ExclusiveMaximum {
		
		@Test
		@DisplayName("value equal to maximum fails with NUMBER_TOO_LARGE")
		fun valueEqualToMaximumFails() {
			assertInvalid(NumberMaxValidator, 10, constraint("10", inclusive = false), ConstraintErrorCode.NUMBER_TOO_LARGE)
		}
		
		@Test
		@DisplayName("value below maximum passes")
		fun valueBelowMaximumPasses() {
			assertValid(NumberMaxValidator, 9.9, constraint("10", inclusive = false))
		}
	}
	
	@Nested
	@DisplayName("Literal parsing")
	inner class LiteralParsing {
		
		@Test
		@DisplayName("blank bound fails with VALUE_PARSING_FAILED")
		fun blankBoundFailsWithFormatInvalid() {
			assertInvalid(NumberMaxValidator, 0, constraint(""), ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
		
		@Test
		@DisplayName("whitespace-only bound fails with VALUE_PARSING_FAILED")
		fun whitespaceBoundFailsWithFormatInvalid() {
			assertInvalid(NumberMaxValidator, 0, constraint("   "), ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
		
		@Test
		@DisplayName("underscore-separated literal passes")
		fun underscoreLiteralPasses() {
			assertValid(NumberMaxValidator, 1000, constraint("1_000"))
		}
		
		@Test
		@DisplayName("decimal bound compares correctly")
		fun decimalBoundComparesCorrectly() {
			assertValid(NumberMaxValidator, BigDecimal("1.5"), constraint("1.5"))
			assertInvalid(NumberMaxValidator, BigDecimal("1.6"), constraint("1.5"), ConstraintErrorCode.NUMBER_TOO_LARGE)
		}
	}
}
