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
package io.ghaylan.validata.constraint.validator.number.multiple

import io.ghaylan.validata.constraint.annotation.MultipleOfConstraint
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
 * Unit tests for [MultipleOfValidator].
 * 
 * @author Ghaylan Saada
 */
class MultipleOfValidatorTest {
	
	private fun constraint(factor: String): MultipleOfConstraint = MultipleOfConstraint(factor, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("Null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value skips validation")
		fun nullValueSkipsValidation() {
			assertSkipsNull(MultipleOfValidator, constraint("2"))
		}
	}
	
	@Nested
	@DisplayName("Multiple check")
	inner class MultipleCheck {
		
		@Test
		@DisplayName("exact multiple passes")
		fun exactMultiplePasses() {
			assertValid(MultipleOfValidator, 6, constraint("2"))
		}
		
		@Test
		@DisplayName("zero is a multiple of any positive factor")
		fun zeroIsMultipleOfPositiveFactor() {
			assertValid(MultipleOfValidator, 0, constraint("2"))
		}
		
		@Test
		@DisplayName("non-multiple fails with NUMBER_NOT_MULTIPLE")
		fun nonMultipleFails() {
			assertInvalid(MultipleOfValidator, 5, constraint("2"), ConstraintErrorCode.NUMBER_NOT_MULTIPLE)
		}
		
		@Test
		@DisplayName("decimal multiple passes")
		fun decimalMultiplePasses() {
			assertValid(MultipleOfValidator, BigDecimal("1.5"), constraint("0.5"))
			assertInvalid(
				MultipleOfValidator,
				BigDecimal("1.6"),
				constraint("0.5"),
				ConstraintErrorCode.NUMBER_NOT_MULTIPLE,
			)
		}
	}
	
	@Nested
	@DisplayName("Factor parsing and edge cases")
	inner class FactorParsing {
		
		@Test
		@DisplayName("blank factor fails with VALUE_PARSING_FAILED")
		fun blankFactorFailsWithFormatInvalid() {
			assertInvalid(MultipleOfValidator, 4, constraint(""), ConstraintErrorCode.VALUE_PARSING_FAILED)
		}
		
		@Test
		@DisplayName("underscore factor passes")
		fun underscoreFactorPasses() {
			assertValid(MultipleOfValidator, 2000, constraint("1_000"))
		}
		
		@Test
		@DisplayName("zero factor skips validation")
		fun zeroFactorSkipsValidation() {
			assertValid(MultipleOfValidator, 4, constraint("0"))
		}
		
		@Test
		@DisplayName("negative factor skips validation")
		fun negativeFactorSkipsValidation() {
			assertValid(MultipleOfValidator, 4, constraint("-2"))
		}
	}
}
