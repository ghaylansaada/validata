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
package io.ghaylan.validata.constraint.validator.string.phone

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import io.ghaylan.validata.constraint.annotation.PhoneConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [PhoneValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("PhoneValidator")
class PhoneValidatorTest {
	
	private fun c(
		allowedTypes: Set<PhoneNumberType> = emptySet(),
		allowedCountries: Set<String> = emptySet(),
	) = PhoneConstraint(allowedTypes, allowedCountries, "", ValidatorTestSupport.defaultGroups)
	
	private val usMobile = "14155552671"
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(PhoneValidator, c())
		}
	}
	
	@Nested
	@DisplayName("valid numbers")
	inner class Valid {
		
		@Test
		@DisplayName("digits-only international number passes with open policy")
		fun usNumber() {
			assertValid(PhoneValidator, usMobile, c())
		}
	}
	
	@Nested
	@DisplayName("format failures")
	inner class FormatFailures {
		
		@Test
		@DisplayName("non-digit characters fail with VALUE_FORMAT_INVALID")
		fun formattedInput() {
			assertInvalid(
				PhoneValidator,
				"415-555-2671",
				c(),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("too-short digit string fails with VALUE_FORMAT_INVALID")
		fun tooShort() {
			assertInvalid(
				PhoneValidator,
				"123",
				c(),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("alphabetic input fails with VALUE_FORMAT_INVALID")
		fun alphabetic() {
			assertInvalid(
				PhoneValidator,
				"nope",
				c(),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("type and country policy")
	inner class PolicyFilters {
		
		@Test
		@DisplayName("number type outside allow-list fails with VALUE_NOT_ALLOWED")
		fun typeRestricted() {
			assertInvalid(
				PhoneValidator,
				usMobile,
				c(allowedTypes = setOf(PhoneNumberType.FIXED_LINE)),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("number country outside allow-list fails with VALUE_NOT_ALLOWED")
		fun countryRestricted() {
			assertInvalid(
				PhoneValidator,
				usMobile,
				c(allowedCountries = setOf("DE")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
	}
}
