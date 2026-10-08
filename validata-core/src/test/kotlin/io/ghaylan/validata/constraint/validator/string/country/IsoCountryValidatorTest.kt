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
package io.ghaylan.validata.constraint.validator.string.country

import io.ghaylan.validata.constraint.annotation.IsoCountryConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [IsoCountryValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("IsoCountryValidator")
class IsoCountryValidatorTest {
	
	private val c = IsoCountryConstraint("", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(IsoCountryValidator, c)
		}
	}
	
	@Nested
	@DisplayName("valid codes")
	inner class Valid {
		
		@Test
		@DisplayName("US alpha-2 code passes")
		fun unitedStates() {
			assertValid(IsoCountryValidator, "US", c)
		}
		
		@Test
		@DisplayName("DE alpha-2 code passes")
		fun germany() {
			assertValid(IsoCountryValidator, "DE", c)
		}
	}
	
	@Nested
	@DisplayName("invalid codes")
	inner class Invalid {
		
		@Test
		@DisplayName("unknown alpha-2 code fails with VALUE_NOT_ALLOWED")
		fun unknownCode() {
			assertInvalid(IsoCountryValidator, "XX", c, ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
		
		@Test
		@DisplayName("full country name fails with VALUE_NOT_ALLOWED")
		fun fullName() {
			assertInvalid(IsoCountryValidator, "United States", c, ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
		
		@Test
		@DisplayName("lowercase code fails with VALUE_NOT_ALLOWED")
		fun lowercaseCode() {
			assertInvalid(IsoCountryValidator, "us", c, ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
	}
}
