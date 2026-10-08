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
package io.ghaylan.validata.constraint.validator.string.creditcard

import io.ghaylan.validata.constraint.annotation.CreditCardConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [CreditCardValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("CreditCardValidator")
class CreditCardValidatorTest {
	
	private val c = CreditCardConstraint("", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(CreditCardValidator, c)
		}
	}
	
	@Nested
	@DisplayName("valid card numbers")
	inner class Valid {
		
		@Test
		@DisplayName("Visa test PAN passes Luhn check")
		fun visa() {
			assertValid(CreditCardValidator, "4111111111111111", c)
		}
		
		@Test
		@DisplayName("PAN with spaces and dashes passes when digit count is valid")
		fun spacedAndDashed() {
			assertValid(CreditCardValidator, "4111 1111-1111 1111", c)
		}
	}
	
	@Nested
	@DisplayName("format failures")
	inner class FormatFailures {
		
		@Test
		@DisplayName("too few digits fail with VALUE_FORMAT_INVALID")
		fun tooFewDigits() {
			assertInvalid(
				CreditCardValidator,
				"1234",
				c,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("illegal characters fail with VALUE_FORMAT_INVALID")
		fun illegalCharacter() {
			assertInvalid(
				CreditCardValidator,
				"411111111111111x",
				c,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("whitespace-only string fails with VALUE_FORMAT_INVALID")
		fun whitespaceOnly() {
			assertInvalid(
				CreditCardValidator,
				"   ",
				c,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("checksum failures")
	inner class ChecksumFailures {
		
		@Test
		@DisplayName("valid length with failed Luhn fails with VALUE_CHECKSUM_INVALID")
		fun luhnFailure() {
			assertInvalid(
				CreditCardValidator,
				"4111111111111112",
				c,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
}
