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
package io.ghaylan.validata.constraint.validator.number.digits

import io.ghaylan.validata.constraint.annotation.DigitsConstraint
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
 * Unit tests for [DigitsValidator].
 * 
 * @author Ghaylan Saada
 */
class DigitsValidatorTest {
	
	private fun constraint(
		integer: Int,
		fraction: Int
	): DigitsConstraint = DigitsConstraint(integer, fraction, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("Null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value skips validation")
		fun nullValueSkipsValidation() {
			assertSkipsNull(DigitsValidator, constraint(3, 2))
		}
	}
	
	@Nested
	@DisplayName("Within limits")
	inner class WithinLimits {
		
		@Test
		@DisplayName("value within integer and fraction limits passes")
		fun withinLimitsPasses() {
			assertValid(DigitsValidator, BigDecimal("123.45"), constraint(3, 2))
			assertValid(DigitsValidator, BigDecimal("10.50"), constraint(2, 2))
		}
		
		@Test
		@DisplayName("negative sign does not affect digit counts")
		fun negativeSignIgnored() {
			assertValid(DigitsValidator, BigDecimal("-12.3"), constraint(2, 1))
		}
		
		@Test
		@DisplayName("zero integer part passes")
		fun zeroIntegerPartPasses() {
			assertValid(DigitsValidator, BigDecimal("0.001"), constraint(0, 3))
		}
	}
	
	@Nested
	@DisplayName("Integer digit limit")
	inner class IntegerLimit {
		
		@Test
		@DisplayName("too many integer digits fails with NUMBER_PRECISION_EXCEEDED")
		fun tooManyIntegerDigitsFails() {
			assertInvalid(
				DigitsValidator,
				BigDecimal("1234.5"),
				constraint(3, 2),
				ConstraintErrorCode.NUMBER_PRECISION_EXCEEDED,
			)
		}
	}
	
	@Nested
	@DisplayName("Fraction digit limit")
	inner class FractionLimit {
		
		@Test
		@DisplayName("too many fraction digits fails with NUMBER_SCALE_EXCEEDED")
		fun tooManyFractionDigitsFails() {
			assertInvalid(
				DigitsValidator,
				BigDecimal("12.345"),
				constraint(3, 2),
				ConstraintErrorCode.NUMBER_SCALE_EXCEEDED,
			)
		}
		
		@Test
		@DisplayName("trailing fractional zeros count toward scale")
		fun trailingZerosCount() {
			assertInvalid(
				DigitsValidator,
				BigDecimal("10.50"),
				constraint(2, 1),
				ConstraintErrorCode.NUMBER_SCALE_EXCEEDED,
			)
		}
	}
	
	@Nested
	@DisplayName("Non-finite values")
	inner class NonFinite {
		
		@Test
		@DisplayName("NaN fails with VALUE_PARSING_FAILED")
		fun nanFails() {
			assertInvalid(DigitsValidator, Double.NaN, constraint(3, 2), ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
	}
}
