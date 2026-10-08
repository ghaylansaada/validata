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
package io.ghaylan.validata.constraint.validator.number.min

import io.ghaylan.validata.constraint.annotation.MinConstraint
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
 * Unit tests for [NumberMinValidator].
 * 
 * @author Ghaylan Saada
 */
class NumberMinValidatorTest {
	
	private fun constraint(
		value: String,
		inclusive: Boolean = true
	): MinConstraint = MinConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("Null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value skips validation")
		fun nullValueSkipsValidation() {
			assertSkipsNull(NumberMinValidator, constraint("10"))
		}
	}
	
	@Nested
	@DisplayName("Inclusive minimum")
	inner class InclusiveMinimum {
		
		@Test
		@DisplayName("value equal to minimum passes")
		fun valueEqualToMinimumPasses() {
			assertValid(NumberMinValidator, 10, constraint("10", inclusive = true))
		}
		
		@Test
		@DisplayName("value above minimum passes")
		fun valueAboveMinimumPasses() {
			assertValid(NumberMinValidator, 11, constraint("10", inclusive = true))
		}
		
		@Test
		@DisplayName("value below minimum fails with NUMBER_TOO_SMALL")
		fun valueBelowMinimumFails() {
			assertInvalid(
				NumberMinValidator,
				9,
				constraint("10", inclusive = true),
				ConstraintErrorCode.NUMBER_TOO_SMALL,
				constraint("10", inclusive = true),
			)
		}
	}
	
	@Nested
	@DisplayName("Exclusive minimum")
	inner class ExclusiveMinimum {
		
		@Test
		@DisplayName("value equal to minimum fails with NUMBER_TOO_SMALL")
		fun valueEqualToMinimumFails() {
			assertInvalid(NumberMinValidator, 10, constraint("10", inclusive = false), ConstraintErrorCode.NUMBER_TOO_SMALL)
		}
		
		@Test
		@DisplayName("value above minimum passes")
		fun valueAboveMinimumPasses() {
			assertValid(NumberMinValidator, 10.1, constraint("10", inclusive = false))
		}
	}
	
	@Nested
	@DisplayName("Literal parsing")
	inner class LiteralParsing {
		
		@Test
		@DisplayName("blank bound fails with VALUE_PARSING_FAILED")
		fun blankBoundFailsWithFormatInvalid() {
			assertInvalid(NumberMinValidator, 0, constraint(""), ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
		
		@Test
		@DisplayName("whitespace-only bound fails with VALUE_PARSING_FAILED")
		fun whitespaceBoundFailsWithFormatInvalid() {
			assertInvalid(NumberMinValidator, 0, constraint("   "), ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
		
		@Test
		@DisplayName("underscore-separated literal passes")
		fun underscoreLiteralPasses() {
			assertValid(NumberMinValidator, 1000, constraint("1_000"))
		}
		
		@Test
		@DisplayName("decimal bound compares correctly")
		fun decimalBoundComparesCorrectly() {
			assertValid(NumberMinValidator, BigDecimal("1.5"), constraint("1.5"))
			assertInvalid(NumberMinValidator, BigDecimal("1.4"), constraint("1.5"), ConstraintErrorCode.NUMBER_TOO_SMALL)
		}
	}
}
