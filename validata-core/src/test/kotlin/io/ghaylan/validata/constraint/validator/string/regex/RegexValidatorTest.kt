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
package io.ghaylan.validata.constraint.validator.string.regex

import io.ghaylan.validata.constraint.annotation.RegexConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [RegexValidator].
 
 * Guards: pattern match / skip-null / invalid pattern; overlong input must fail closed
 * (ReDoS / length-bound regression) without hanging.
 * 
 * @author Ghaylan Saada
 */
@DisplayName("RegexValidator")
class RegexValidatorTest {
	
	private fun c(
		pattern: String,
		name: String = "DIGITS"
	) = RegexConstraint(pattern, name, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(RegexValidator, c("^[0-9]+$"))
		}
	}
	
	@Nested
	@DisplayName("matching values")
	inner class Valid {
		
		@Test
		@DisplayName("value matching pattern passes")
		fun digitsOnly() {
			assertValid(RegexValidator, "12345", c("^[0-9]+$"))
		}
	}
	
	@Nested
	@DisplayName("non-matching values")
	inner class Invalid {
		
		@Test
		@DisplayName("value not matching pattern fails with TEXT_PATTERN_MISMATCH")
		fun lettersAgainstDigitPattern() {
			assertInvalid(
				RegexValidator,
				"abc",
				c("^[0-9]+$"),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}
		
		@Test
		@DisplayName("invalid regex pattern fails with TEXT_PATTERN_MISMATCH")
		fun invalidPattern() {
			assertInvalid(
				RegexValidator,
				"anything",
				c("[unclosed"),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}
		
		@Test
		@DisplayName("whitespace-only string fails with TEXT_PATTERN_MISMATCH")
		fun whitespaceOnly() {
			assertInvalid(
				RegexValidator,
				"   ",
				c("^[0-9]+$"),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}
		
		@Test
		@DisplayName("input longer than MAX_INPUT_LENGTH fails without matching")
		fun rejectsOverlongInput() {
			val overlong = "1".repeat(RegexValidator.MAX_INPUT_LENGTH + 1)
			assertInvalid(
				RegexValidator,
				overlong,
				c("^[0-9]+$"),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}
	}
}
