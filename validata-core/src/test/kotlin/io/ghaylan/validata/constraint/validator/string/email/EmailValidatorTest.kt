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
package io.ghaylan.validata.constraint.validator.string.email

import io.ghaylan.validata.constraint.annotation.EmailConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [EmailValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("EmailValidator")
class EmailValidatorTest {
	
	private val c = EmailConstraint("", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(EmailValidator, c)
		}
	}
	
	@Nested
	@DisplayName("valid addresses")
	inner class Valid {
		
		@Test
		@DisplayName("simple local@domain passes")
		fun simpleAddress() {
			assertValid(EmailValidator, "a@b.com", c)
		}
		
		@Test
		@DisplayName("dotted local part and multi-label domain pass")
		fun dottedLocalAndDomain() {
			assertValid(EmailValidator, "user.name@mail.example.co.uk", c)
		}
		
		@Test
		@DisplayName("plus-addressing is rejected by the built-in pattern with VALUE_FORMAT_INVALID")
		fun plusAddressingRejected() {
			assertInvalid(
				EmailValidator,
				"user.name+tag@mail.example.co.uk",
				c,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("empty string fails with VALUE_FORMAT_INVALID")
		fun emptyString() {
			assertInvalid(EmailValidator, "", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
	
	@Nested
	@DisplayName("invalid addresses")
	inner class Invalid {
		
		@Test
		@DisplayName("plain text fails with VALUE_FORMAT_INVALID")
		fun plainText() {
			assertInvalid(EmailValidator, "not-an-email", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("missing local part fails with VALUE_FORMAT_INVALID")
		fun missingLocalPart() {
			assertInvalid(EmailValidator, "@example.com", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("missing domain fails with VALUE_FORMAT_INVALID")
		fun missingDomain() {
			assertInvalid(EmailValidator, "user@", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("space in address fails with VALUE_FORMAT_INVALID")
		fun containsSpace() {
			assertInvalid(EmailValidator, "user @example.com", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("whitespace-only string fails with VALUE_FORMAT_INVALID")
		fun whitespaceOnly() {
			assertInvalid(EmailValidator, "   ", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
}
