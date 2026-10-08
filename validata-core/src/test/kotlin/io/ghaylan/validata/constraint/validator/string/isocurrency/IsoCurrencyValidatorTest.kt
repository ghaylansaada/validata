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
package io.ghaylan.validata.constraint.validator.string.isocurrency

import io.ghaylan.validata.constraint.annotation.IsoCurrencyConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [IsoCurrencyValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("IsoCurrencyValidator")
class IsoCurrencyValidatorTest {
	
	private val c = IsoCurrencyConstraint("", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(IsoCurrencyValidator, c)
		}
	}
	
	@Nested
	@DisplayName("valid codes")
	inner class Valid {
		
		@Test
		@DisplayName("USD passes")
		fun usDollar() {
			assertValid(IsoCurrencyValidator, "USD", c)
		}
		
		@Test
		@DisplayName("EUR passes")
		fun euro() {
			assertValid(IsoCurrencyValidator, "EUR", c)
		}
	}
	
	@Nested
	@DisplayName("invalid codes")
	inner class Invalid {
		
		@Test
		@DisplayName("unknown code fails with VALUE_NOT_ALLOWED")
		fun unknownCode() {
			assertInvalid(IsoCurrencyValidator, "ZZZ", c, ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
		
		@Test
		@DisplayName("lowercase code fails with VALUE_NOT_ALLOWED")
		fun lowercaseCode() {
			assertInvalid(IsoCurrencyValidator, "usd", c, ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
		
		@Test
		@DisplayName("numeric pseudo-code fails with VALUE_NOT_ALLOWED")
		fun numericCode() {
			assertInvalid(IsoCurrencyValidator, "123", c, ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
	}
}
