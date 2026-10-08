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
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode

/**
 * Shared structure and checksum helpers for [FinancialCodeValidator].
 *
 * Side effects: none.
 *
 * @author Ghaylan Saada
 */
internal object FinancialCodeSupport {

	/**
	 * ISO 13616 country code → expected IBAN character length (static registry).
	 */
	val ibanCountryLengths: Map<String, Int> = mapOf(
		"AL" to 28, "AD" to 24, "AT" to 20, "AZ" to 28, "BH" to 22, "BE" to 16,
		"BA" to 20, "BR" to 29, "BG" to 22, "CR" to 22, "HR" to 21, "CY" to 28,
		"CZ" to 24, "DK" to 18, "DO" to 28, "EE" to 20, "FO" to 18, "FI" to 18,
		"FR" to 27, "GE" to 22, "DE" to 22, "GI" to 23, "GR" to 27, "GL" to 18,
		"GT" to 28, "HU" to 28, "IS" to 26, "IE" to 22, "IL" to 23, "IT" to 27,
		"JO" to 30, "KZ" to 20, "KW" to 30, "LV" to 21, "LB" to 28, "LI" to 21,
		"LT" to 20, "LU" to 20, "MT" to 31, "MR" to 27, "MU" to 30, "MC" to 27,
		"MD" to 24, "ME" to 22, "NL" to 18, "MK" to 19, "NO" to 15, "PK" to 24,
		"PS" to 29, "PL" to 28, "PT" to 25, "QA" to 29, "RO" to 24, "SM" to 27,
		"SA" to 24, "RS" to 22, "SK" to 24, "SI" to 19, "ES" to 24, "SE" to 24,
		"CH" to 21, "TN" to 24, "TR" to 26, "AE" to 23, "GB" to 22, "VG" to 24,
	)

	private val bicPattern = Regex("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")

	/**
	 * Normalizes financial input: strip whitespace and uppercase.
	 *
	 * Side effects: none.
	 *
	 * @param value Raw identifier text.
	 * @return Compact uppercase form.
	 */
	fun normalize(value: CharSequence): String =
		value.toString().filterNot(Char::isWhitespace).uppercase()

	/**
	 * Whether [countryCode] is permitted by the optional [countries] filter.
	 *
	 * Empty [countries] accepts any country. Side effects: none.
	 *
	 * @param countryCode ISO country code from the identifier.
	 * @param countries Allowed country codes from the constraint (may be empty).
	 * @return `true` when unrestricted or [countryCode] is listed.
	 */
	fun countryAllowed(
		countryCode: String,
		countries: Set<String>,
	): Boolean {
		if (countries.isEmpty()) return true
		val allowed = countries.map { it.uppercase() }.toSet()
		return countryCode.uppercase() in allowed
	}

	/**
	 * ISO 7064 MOD-97-10 after rotating the first four characters to the end (IBAN layout).
	 *
	 * Side effects: none.
	 *
	 * @param iban Normalized uppercase IBAN (no whitespace).
	 * @return `true` when the running remainder equals 1.
	 */
	fun verifyIbanMod97(iban: String): Boolean {
		val rearranged = iban.substring(4) + iban.substring(0, 4)
		return mod97Remainder(rearranged) == 1
	}

	/**
	 * ISO 7064 MOD-97-10 over an alphanumeric string (no IBAN rearrangement).
	 *
	 * Side effects: none.
	 *
	 * @param value Uppercase alphanumeric payload including check digits.
	 * @return `true` when the running remainder equals 1.
	 */
	fun verifyMod97(value: String): Boolean = mod97Remainder(value) == 1

	/**
	 * Running MOD-97 remainder with letters expanded to two decimal digits (`A`=10 … `Z`=35).
	 *
	 * Side effects: none.
	 *
	 * @param value Uppercase alphanumeric payload.
	 * @return Remainder in `0..96`, or `-1` when a non-alphanumeric character appears.
	 */
	fun mod97Remainder(value: String): Int {
		var remainder = 0
		for (ch in value) {
			when {
				ch.isDigit() -> remainder = (remainder * 10 + (ch - '0')) % 97
				ch in 'A'..'Z' -> {
					val letterNumericValue = ch - 'A' + 10
					remainder = (remainder * 10 + letterNumericValue / 10) % 97
					remainder = (remainder * 10 + letterNumericValue % 10) % 97
				}
				else -> return -1
			}
		}
		return remainder
	}

	/**
	 * Validates a normalized IBAN against length, country filter, and MOD-97.
	 *
	 * Side effects: none.
	 *
	 * @param clean Normalized uppercase IBAN (no whitespace).
	 * @param constraint Financial-code constraint (type / country allow-list).
	 * @return Violation with [constraint] attached, or `null` when valid.
	 */
	fun validateIban(
		clean: String,
		constraint: FinancialCodeConstraint,
	): ConstraintError<*>? {
		if (clean.length < 4 || !clean.all { it.isDigit() || it in 'A'..'Z' }) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "Must be a valid ${FinancialCode.Type.IBAN.name}: a two-letter country code followed by letters and digits.",
				metadata = constraint,
			)
		}
		val countryCode = clean.take(2)
		if (!countryAllowed(countryCode, constraint.countries)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
				message = "IBAN country code '$countryCode' is not in the allowed country list.",
				metadata = constraint,
			)
		}
		val expectedLength = ibanCountryLengths[countryCode]
			?: return ConstraintError(
				code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
				message = "IBAN country code '$countryCode' is not recognized.",
				metadata = constraint,
			)

		if (clean.length != expectedLength) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "IBAN must be exactly $expectedLength characters for country $countryCode.",
				metadata = constraint,
			)
		}

		if (!verifyIbanMod97(clean)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
				message = "IBAN is not valid; check it for a typing error.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Validates a normalized BIC/SWIFT code (8 or 11 characters) and optional country filter.
	 *
	 * Side effects: none.
	 *
	 * @param clean Normalized uppercase BIC (no whitespace).
	 * @param constraint Financial-code constraint (type / country allow-list).
	 * @return Violation with [constraint] attached, or `null` when valid.
	 */
	fun validateBic(
		clean: String,
		constraint: FinancialCodeConstraint,
	): ConstraintError<*>? {
		if (clean.length !in setOf(8, 11) || !bicPattern.matches(clean)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "Must be a valid ${FinancialCode.Type.BIC.name}/SWIFT code (8 or 11 characters: bank, country, location, optional branch).",
				metadata = constraint,
			)
		}
		val countryCode = clean.substring(4, 6)
		if (!countryAllowed(countryCode, constraint.countries)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
				message = "BIC country code '$countryCode' is not in the allowed country list.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Validates a normalized ISIN (layout + Luhn) and optional country filter.
	 *
	 * Side effects: none.
	 *
	 * @param clean Normalized uppercase ISIN (no whitespace).
	 * @param constraint Financial-code constraint (type / country allow-list).
	 * @return Violation with [constraint] attached, or `null` when valid.
	 */
	fun validateIsin(
		clean: String,
		constraint: FinancialCodeConstraint,
	): ConstraintError<*>? {
		if (clean.length != 12 ||
			clean[0] !in 'A'..'Z' ||
			clean[1] !in 'A'..'Z' ||
			!clean.substring(2, 11).all { it.isDigit() || it in 'A'..'Z' } ||
			clean[11] !in '0'..'9'
		) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "Must be a valid ${FinancialCode.Type.ISIN.name}: 2-letter country, 9 alphanumeric characters, and 1 check digit.",
				metadata = constraint,
			)
		}
		val countryCode = clean.take(2)
		if (!countryAllowed(countryCode, constraint.countries)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
				message = "ISIN country code '$countryCode' is not allowed.",
				metadata = constraint,
			)
		}
		if (!passesIsinLuhn(clean)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
				message = "ISIN is not valid; check it for a typing error.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * ISO 6166 ISIN Luhn: expand `A`–`Z` to 10–35, then classic Luhn over the digit stream.
	 *
	 * Side effects: none.
	 *
	 * @param isin Normalized 12-character ISIN.
	 * @return `true` when the Luhn check passes.
	 */
	fun passesIsinLuhn(isin: String): Boolean {
		val digits = StringBuilder(22)
		for (ch in isin) {
			when {
				ch.isDigit() -> digits.append(ch)
				ch in 'A'..'Z' -> digits.append(ch - 'A' + 10)
				else -> return false
			}
		}
		var sum = 0
		var doubleDigit = false
		for (i in digits.lastIndex downTo 0) {
			var d = digits[i] - '0'
			if (doubleDigit) {
				d *= 2
				if (d > 9) d -= 9
			}
			sum += d
			doubleDigit = !doubleDigit
		}
		return sum % 10 == 0
	}
}
