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
package io.ghaylan.validata.constraint.validator.string.financial

import io.ghaylan.validata.constraint.annotation.FinancialCode
import io.ghaylan.validata.constraint.annotation.FinancialCodeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FinancialCodeValidator].
 *
 * @author Ghaylan Saada
 */
@DisplayName("FinancialCodeValidator")
class FinancialCodeValidatorTest {

	private fun c(
		type: FinancialCode.Type,
		countries: Set<String> = emptySet(),
	) = FinancialCodeConstraint(
		type = type,
		countries = countries,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)

	private val validDe = "DE89370400440532013000"

	@Nested
	@DisplayName("null handling")
	inner class NullHandling {

		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(FinancialCodeValidator, c(FinancialCode.Type.IBAN))
		}
	}

	@Nested
	@DisplayName("IBAN")
	inner class Iban {

		private val constraint = c(FinancialCode.Type.IBAN)

		@Test
		@DisplayName("German IBAN with valid checksum passes")
		fun germanIban() {
			assertValid(FinancialCodeValidator, validDe, constraint)
		}

		@Test
		@DisplayName("whitespace in IBAN is ignored")
		fun ignoresWhitespace() {
			assertValid(FinancialCodeValidator, "DE89 3704 0044 0532 0130 00", constraint)
		}

		@Test
		@DisplayName("country filter accepts matching country")
		fun countryFilterPass() {
			assertValid(FinancialCodeValidator, validDe, c(FinancialCode.Type.IBAN, setOf("DE", "FR")))
		}

		@Test
		@DisplayName("country filter rejects other country")
		fun countryFilterFail() {
			assertInvalid(
				FinancialCodeValidator,
				validDe,
				c(FinancialCode.Type.IBAN, setOf("FR")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}

		@Test
		@DisplayName("too-short value fails with VALUE_FORMAT_INVALID")
		fun tooShort() {
			assertInvalid(FinancialCodeValidator, "DE00", constraint, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}

		@Test
		@DisplayName("invalid characters fail with VALUE_FORMAT_INVALID")
		fun invalidCharacters() {
			assertInvalid(FinancialCodeValidator, "DE@9", constraint, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}

		@Test
		@DisplayName("wrong length for known country fails with VALUE_FORMAT_INVALID")
		fun wrongCountryLength() {
			assertInvalid(
				FinancialCodeValidator,
				"DE893704004405320130",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}

		@Test
		@DisplayName("unsupported country code fails with VALUE_NOT_ALLOWED")
		fun unsupportedCountry() {
			assertInvalid(
				FinancialCodeValidator,
				"US89370400440532013000",
				constraint,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}

		@Test
		@DisplayName("failed MOD-97 checksum fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				FinancialCodeValidator,
				"DE89370400440532013001",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}

	@Nested
	@DisplayName("BIC")
	inner class Bic {

		private val constraint = c(FinancialCode.Type.BIC)

		@Test
		@DisplayName("valid 8-character BIC passes")
		fun valid8() {
			assertValid(FinancialCodeValidator, "DEUTDEFF", constraint)
		}

		@Test
		@DisplayName("valid 11-character BIC passes")
		fun valid11() {
			assertValid(FinancialCodeValidator, "DEUTDEFF500", constraint)
		}

		@Test
		@DisplayName("country filter rejects other country")
		fun countryFilterFail() {
			assertInvalid(
				FinancialCodeValidator,
				"DEUTDEFF",
				c(FinancialCode.Type.BIC, setOf("FR")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}

		@Test
		@DisplayName("invalid format fails with VALUE_FORMAT_INVALID")
		fun badFormat() {
			assertInvalid(FinancialCodeValidator, "DEUT", constraint, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}

	@Nested
	@DisplayName("ISIN")
	inner class Isin {

		private val constraint = c(FinancialCode.Type.ISIN)

		@Test
		@DisplayName("valid Apple ISIN passes")
		fun validApple() {
			assertValid(FinancialCodeValidator, "US0378331005", constraint)
		}

		@Test
		@DisplayName("wrong length fails with VALUE_FORMAT_INVALID")
		fun badLength() {
			assertInvalid(FinancialCodeValidator, "US037833100", constraint, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}

		@Test
		@DisplayName("failed Luhn check fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				FinancialCodeValidator,
				"US0378331006",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
}
