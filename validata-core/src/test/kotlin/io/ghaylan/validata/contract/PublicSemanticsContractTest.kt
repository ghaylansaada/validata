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
package io.ghaylan.validata.contract

import io.ghaylan.validata.constraint.ConstraintComposition
import io.ghaylan.validata.constraint.annotation.PasswordConstraint
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.string.password.PasswordValidator
import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.engine.ValidationOptions
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Freezes public enum entries and high-visibility semantics called out in Stability & SemVer.
 *
 * Changing these without a SemVer bump + CHANGELOG entry is a contract break.
 *
 * @author Ghaylan Saada
 */
class PublicSemanticsContractTest {

	@Nested
	@DisplayName("Required.Mode")
	inner class RequiredModeContract {

		@Test
		@DisplayName("Mode entry names stay NULL / EMPTY / STRICT")
		fun modeNamesAreFrozen() {
			assertThat(Required.Mode.entries.map { it.name }).containsExactly("NULL", "EMPTY", "STRICT")
		}

		@Test
		@DisplayName("STRICT rejects null, blank text, and empty collections")
		fun strictSemantics() {
			val c = required(Required.Mode.STRICT)
			assertInvalid(RequiredValidator, null, c, ConstraintErrorCode.VALUE_MISSING)
			assertInvalid(RequiredValidator, "  ", c, ConstraintErrorCode.TEXT_BLANK)
			assertInvalid(RequiredValidator, emptyList<Any>(), c, ConstraintErrorCode.VALUE_EMPTY)
			assertValid(RequiredValidator, "x", c)
		}

		@Test
		@DisplayName("NULL rejects only null; blank and empty pass")
		fun nullSemantics() {
			val c = required(Required.Mode.NULL)
			assertInvalid(RequiredValidator, null, c, ConstraintErrorCode.VALUE_MISSING)
			assertValid(RequiredValidator, "  ", c)
			assertValid(RequiredValidator, emptyList<Any>(), c)
		}

		@Test
		@DisplayName("EMPTY rejects null and empty text/collections; whitespace-only text passes")
		fun emptySemantics() {
			val c = required(Required.Mode.EMPTY)
			assertInvalid(RequiredValidator, null, c, ConstraintErrorCode.VALUE_MISSING)
			assertInvalid(RequiredValidator, "", c, ConstraintErrorCode.TEXT_BLANK)
			assertInvalid(RequiredValidator, emptyList<Any>(), c, ConstraintErrorCode.VALUE_EMPTY)
			assertValid(RequiredValidator, "  ", c)
		}
	}

	@Nested
	@DisplayName("ConstraintComposition.Mode")
	inner class CompositionModeContract {

		@Test
		@DisplayName("Mode entry names stay AND / OR")
		fun modeNamesAreFrozen() {
			assertThat(ConstraintComposition.Mode.entries.map { it.name }).containsExactly("AND", "OR")
		}

		@Test
		@DisplayName("annotation default mode is AND")
		fun defaultIsAnd() {
			assertThat(ConstraintComposition().value).isEqualTo(ConstraintComposition.Mode.AND)
		}
	}

	@Nested
	@DisplayName("ValidationOptions / ValidationLimits defaults")
	inner class EngineDefaultsContract {

		@Test
		@DisplayName("ValidationOptions defaults stay oneErrorPerParam=true, failFast=false, OnDefault")
		fun validationOptionsDefaults() {
			val options = ValidationOptions()
			assertThat(options.oneErrorPerParam).isTrue()
			assertThat(options.failFast).isFalse()
			assertThat(options.groups).containsExactly(OnDefault::class)
		}

		@Test
		@DisplayName("ValidationLimits defaults stay 32 / 10000 / 200")
		fun validationLimitsDefaults() {
			assertThat(ValidationLimits.DEFAULT_MAX_DEPTH).isEqualTo(32)
			assertThat(ValidationLimits.DEFAULT_MAX_ELEMENTS_PER_CONTAINER).isEqualTo(10_000)
			assertThat(ValidationLimits.DEFAULT_MAX_ERRORS).isEqualTo(200)
			val limits = ValidationLimits()
			assertThat(limits.maxDepth).isEqualTo(32)
			assertThat(limits.maxElementsPerContainer).isEqualTo(10_000)
			assertThat(limits.maxErrors).isEqualTo(200)
		}
	}

	@Nested
	@DisplayName("@Password pattern tokens")
	inner class PasswordPatternContract {

		@Test
		@DisplayName("sequential failure attaches PasswordConstraint with noSequentialChars")
		fun sequentialToken() {
			val constraint = password(noSequentialChars = true)
			assertInvalid(
				PasswordValidator,
				"xxabcxxz",
				constraint,
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				constraint,
			)
		}

		@Test
		@DisplayName("repetitive failure attaches PasswordConstraint with noRepetitivePatterns")
		fun repetitiveToken() {
			val constraint = password(noRepetitivePatterns = true)
			assertInvalid(
				PasswordValidator,
				"xxaaayzz",
				constraint,
				ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				constraint,
			)
		}
	}

	private fun required(mode: Required.Mode): RequiredConstraint =
		RequiredConstraint(mode = mode, message = "", groups = ValidatorTestSupport.defaultGroups)

	private fun password(
		noSequentialChars: Boolean = false,
		noRepetitivePatterns: Boolean = false,
	): PasswordConstraint = PasswordConstraint(
		minLength = 6,
		maxLength = 64,
		requireUppercase = false,
		requireLowercase = false,
		requireDigit = false,
		requireSpecialChar = false,
		allowedSpecialChars = "!@#$%^&*()-_=+[{]};:,<.>/?",
		noSequentialChars = noSequentialChars,
		noRepetitivePatterns = noRepetitivePatterns,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
}
