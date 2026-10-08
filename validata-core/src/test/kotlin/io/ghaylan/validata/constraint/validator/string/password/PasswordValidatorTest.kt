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
package io.ghaylan.validata.constraint.validator.string.password

import io.ghaylan.validata.constraint.annotation.PasswordConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PasswordValidator].*
 *
 * @author Ghaylan Saada
 */
@DisplayName("PasswordValidator")
class PasswordValidatorTest {

	/**
	 * Builds a [PasswordConstraint] with the given policy overrides.
	 *
	 * @param minLength Minimum length.
	 * @param maxLength Maximum length.
	 * @param requireUppercase Whether uppercase is required.
	 * @param requireLowercase Whether lowercase is required.
	 * @param requireDigit Whether a digit is required.
	 * @param requireSpecialChar Whether a special character is required.
	 * @param allowedSpecialChars Special-character set when [requireSpecialChar] is true.
	 * @param noSequentialChars Reject sequential letter/digit runs.
	 * @param noRepetitivePatterns Reject repeated characters / blocks.
	 * @return Constraint instance for test invocations.
	 */
	private fun c(
		minLength: Int = 8,
		maxLength: Int = 64,
		requireUppercase: Boolean = false,
		requireLowercase: Boolean = false,
		requireDigit: Boolean = false,
		requireSpecialChar: Boolean = false,
		allowedSpecialChars: String = "!@#",
		noSequentialChars: Boolean = false,
		noRepetitivePatterns: Boolean = false,
	) = PasswordConstraint(
		minLength = minLength,
		maxLength = maxLength,
		requireUppercase = requireUppercase,
		requireLowercase = requireLowercase,
		requireDigit = requireDigit,
		requireSpecialChar = requireSpecialChar,
		allowedSpecialChars = allowedSpecialChars,
		noSequentialChars = noSequentialChars,
		noRepetitivePatterns = noRepetitivePatterns,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)

	@Nested
	@DisplayName("null handling")
	inner class NullHandling {

		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(PasswordValidator, c())
		}
	}

	@Nested
	@DisplayName("valid passwords")
	inner class Valid {

		@Test
		@DisplayName("password meeting all enabled requirements passes")
		fun allRequirements() {
			assertValid(
				PasswordValidator,
				"Ak9m!Pxq",
				c(
					requireUppercase = true,
					requireLowercase = true,
					requireDigit = true,
					requireSpecialChar = true,
					noSequentialChars = true,
					noRepetitivePatterns = true,
				),
			)
		}
	}

	@Nested
	@DisplayName("length bounds")
	inner class LengthBounds {

		@Test
		@DisplayName("password shorter than minLength fails with TEXT_TOO_SHORT")
		fun tooShort() {
			assertInvalid(
				PasswordValidator,
				"Ab1!",
				c(minLength = 8),
				ConstraintErrorCode.TEXT_TOO_SHORT,
			)
		}

		@Test
		@DisplayName("password longer than maxLength fails with TEXT_TOO_LONG")
		fun tooLong() {
			assertInvalid(
				PasswordValidator,
				"Abcd1234!extra",
				c(maxLength = 10),
				ConstraintErrorCode.TEXT_TOO_LONG,
			)
		}
	}

	@Nested
	@DisplayName("character class requirements")
	inner class CharacterClasses {

		@Test
		@DisplayName("missing uppercase fails with TEXT_PATTERN_MISMATCH")
		fun uppercaseMissing() {
			assertInvalid(
				PasswordValidator,
				"abcd1234!",
				c(requireUppercase = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("missing lowercase fails with TEXT_PATTERN_MISMATCH")
		fun lowercaseMissing() {
			assertInvalid(
				PasswordValidator,
				"ABCD1234!",
				c(requireLowercase = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("missing digit fails with TEXT_PATTERN_MISMATCH")
		fun digitMissing() {
			assertInvalid(
				PasswordValidator,
				"Abcdefgh!",
				c(requireDigit = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("missing special character fails with TEXT_PATTERN_MISMATCH")
		fun specialMissing() {
			assertInvalid(
				PasswordValidator,
				"Abcd1234",
				c(requireSpecialChar = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("special char outside allowedSpecialChars fails with TEXT_PATTERN_MISMATCH")
		fun specialOutsideAllowedSet() {
			assertInvalid(
				PasswordValidator,
				"Abcd1234!",
				c(requireSpecialChar = true, allowedSpecialChars = "@#"),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}
	}

	@Nested
	@DisplayName("sequential characters")
	inner class SequentialChars {

		@Test
		@DisplayName("ascending letters fail when noSequentialChars is true")
		fun ascendingLetters() {
			val constraint = c(minLength = 6, noSequentialChars = true)
			assertInvalid(
				PasswordValidator,
				"xxabcxxz",
				constraint,
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				constraint,
			)
		}

		@Test
		@DisplayName("descending digits fail when noSequentialChars is true")
		fun descendingDigits() {
			assertInvalid(
				PasswordValidator,
				"xx321xxz",
				c(minLength = 6, noSequentialChars = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("mixed-case ascending letters fail when noSequentialChars is true")
		fun mixedCaseAscending() {
			assertInvalid(
				PasswordValidator,
				"xAbCdxzz",
				c(minLength = 6, noSequentialChars = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("non-sequential password passes when noSequentialChars is true")
		fun nonSequentialPasses() {
			assertValid(
				PasswordValidator,
				"Akb19!xm",
				c(minLength = 6, noSequentialChars = true),
			)
		}

		@Test
		@DisplayName("sequential run is allowed when noSequentialChars is false")
		fun flagOffAllowsSequential() {
			assertValid(
				PasswordValidator,
				"xxabcxxz",
				c(minLength = 6, noSequentialChars = false),
			)
		}
	}

	@Nested
	@DisplayName("repetitive patterns")
	inner class RepetitivePatterns {

		@Test
		@DisplayName("same character three times fails when noRepetitivePatterns is true")
		fun sameCharRun() {
			val constraint = c(minLength = 6, noRepetitivePatterns = true)
			assertInvalid(
				PasswordValidator,
				"xxaaayzz",
				constraint,
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				constraint,
			)
		}

		@Test
		@DisplayName("repeating block fails when noRepetitivePatterns is true")
		fun repeatingBlock() {
			assertInvalid(
				PasswordValidator,
				"xxabcabcz",
				c(minLength = 6, noRepetitivePatterns = true),
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			)
		}

		@Test
		@DisplayName("non-repetitive password passes when noRepetitivePatterns is true")
		fun nonRepetitivePasses() {
			assertValid(
				PasswordValidator,
				"Akb19!xm",
				c(minLength = 6, noRepetitivePatterns = true),
			)
		}

		@Test
		@DisplayName("repetitive run is allowed when noRepetitivePatterns is false")
		fun flagOffAllowsRepetitive() {
			assertValid(
				PasswordValidator,
				"xxaaayzz",
				c(minLength = 6, noRepetitivePatterns = false),
			)
		}
	}
}
