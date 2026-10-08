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
package io.ghaylan.validata.constraint.validator.string.checksum

import io.ghaylan.validata.constraint.annotation.Checksum
import io.ghaylan.validata.constraint.annotation.ChecksumConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ChecksumValidator].
 *
 * @author Ghaylan Saada
 */
@DisplayName("ChecksumValidator")
class ChecksumValidatorTest {

	private fun c(
		algorithm: Checksum.Algorithm,
		checkDigitIndex: Int = -1,
		startIndex: Int = 0,
		endIndex: Int = -1,
		ignoreNonDigits: Boolean = false,
	) = ChecksumConstraint(
		algorithm = algorithm,
		checkDigitIndex = checkDigitIndex,
		startIndex = startIndex,
		endIndex = endIndex,
		ignoreNonDigits = ignoreNonDigits,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)

	@Nested
	@DisplayName("null handling")
	inner class NullHandling {

		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(ChecksumValidator, c(Checksum.Algorithm.LUHN))
		}
	}

	@Nested
	@DisplayName("LUHN / MOD10")
	inner class Luhn {

		@Test
		@DisplayName("valid Luhn number passes")
		fun validLuhn() {
			assertValid(ChecksumValidator, "79927398713", c(Checksum.Algorithm.LUHN))
		}

		@Test
		@DisplayName("MOD10 aliases Luhn")
		fun mod10Alias() {
			assertValid(ChecksumValidator, "79927398713", c(Checksum.Algorithm.MOD10))
		}

		@Test
		@DisplayName("ignoreNonDigits strips separators")
		fun ignoreSeparators() {
			assertValid(
				ChecksumValidator,
				"7992-7398-713",
				c(Checksum.Algorithm.LUHN, ignoreNonDigits = true),
			)
		}

		@Test
		@DisplayName("bad Luhn fails with VALUE_CHECKSUM_INVALID")
		fun badLuhn() {
			assertInvalid(
				ChecksumValidator,
				"79927398714",
				c(Checksum.Algorithm.LUHN),
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}

		@Test
		@DisplayName("letters without ignoreNonDigits fail with VALUE_FORMAT_INVALID")
		fun badChars() {
			assertInvalid(
				ChecksumValidator,
				"7992739871A",
				c(Checksum.Algorithm.LUHN),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}

	@Nested
	@DisplayName("MOD11")
	inner class Mod11 {

		@Test
		@DisplayName("valid ISBN-10 check passes")
		fun validIsbn10() {
			assertValid(ChecksumValidator, "0306406152", c(Checksum.Algorithm.MOD11))
		}

		@Test
		@DisplayName("valid ISBN-10 with X check digit passes")
		fun validIsbn10X() {
			assertValid(ChecksumValidator, "043942089X", c(Checksum.Algorithm.MOD11))
		}

		@Test
		@DisplayName("bad Mod-11 fails with VALUE_CHECKSUM_INVALID")
		fun badMod11() {
			assertInvalid(
				ChecksumValidator,
				"0306406153",
				c(Checksum.Algorithm.MOD11),
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}

	@Nested
	@DisplayName("VERHOEFF")
	inner class Verhoeff {

		@Test
		@DisplayName("valid Verhoeff number passes")
		fun valid() {
			assertValid(ChecksumValidator, "2363", c(Checksum.Algorithm.VERHOEFF))
		}

		@Test
		@DisplayName("bad Verhoeff fails with VALUE_CHECKSUM_INVALID")
		fun bad() {
			assertInvalid(
				ChecksumValidator,
				"2364",
				c(Checksum.Algorithm.VERHOEFF),
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}

	@Nested
	@DisplayName("DAMM")
	inner class Damm {

		@Test
		@DisplayName("valid Damm number passes")
		fun valid() {
			assertValid(ChecksumValidator, "5724", c(Checksum.Algorithm.DAMM))
		}

		@Test
		@DisplayName("bad Damm fails with VALUE_CHECKSUM_INVALID")
		fun bad() {
			assertInvalid(
				ChecksumValidator,
				"5725",
				c(Checksum.Algorithm.DAMM),
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}

	@Nested
	@DisplayName("MOD97_10")
	inner class Mod97 {

		@Test
		@DisplayName("valid IBAN-style rearranged payload passes when remainder is 1")
		fun validMod97() {
			// Full IBAN without rearrange would fail; use a known remainder-1 alphanumeric string.
			// "00" check appended style: use rearranged IBAN body which FinancialCodeSupport.verifyMod97 accepts.
			val rearranged = "370400440532013000DE89"
			assertValid(ChecksumValidator, rearranged, c(Checksum.Algorithm.MOD97_10))
		}

		@Test
		@DisplayName("bad Mod-97 fails with VALUE_CHECKSUM_INVALID")
		fun badMod97() {
			assertInvalid(
				ChecksumValidator,
				"370400440532013000DE80",
				c(Checksum.Algorithm.MOD97_10),
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}

	@Nested
	@DisplayName("slice bounds")
	inner class SliceBounds {

		@Test
		@DisplayName("startIndex/endIndex select the validated slice")
		fun sliceSelection() {
			assertValid(
				ChecksumValidator,
				"xx79927398713yy",
				c(Checksum.Algorithm.LUHN, startIndex = 2, endIndex = 13),
			)
		}

		@Test
		@DisplayName("out-of-range indices fail with VALUE_FORMAT_INVALID")
		fun badIndices() {
			assertInvalid(
				ChecksumValidator,
				"123",
				c(Checksum.Algorithm.LUHN, startIndex = 5),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
}
