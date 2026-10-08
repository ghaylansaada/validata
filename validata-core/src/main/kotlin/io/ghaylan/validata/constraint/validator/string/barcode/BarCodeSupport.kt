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
package io.ghaylan.validata.constraint.validator.string.barcode

import io.ghaylan.validata.constraint.annotation.Barcode

/**
 * Normalization and checksum helpers for [BarcodeValidator].
 *
 * Strips spaces and hyphens; digits are required, with `X` allowed only as the final check
 * character for ISBN-10 and ISSN.
 *
 * @author Ghaylan Saada
 */
internal object BarcodeSupport {

	/**
	 * Strips separators and uppercases alphabetic check characters.
	 *
	 * Side effects: none.
	 *
	 * @param value Raw barcode text, possibly containing spaces and hyphens.
	 * @return Normalized payload, or `null` when illegal characters appear or `X` is not last.
	 */
	fun normalize(value: CharSequence): String? {
		// Common case: already-clean digit string (benchmark EANs / ISBNs) — no StringBuilder.
		if (value is String && isAlreadyNormalized(value)) return value

		val builder = StringBuilder(value.length)

		for (i in value.indices) {
			when (val ch = value[i]) {
				' ', '-' -> Unit
				in '0'..'9' -> builder.append(ch)
				'x', 'X' -> builder.append('X')
				else -> return null
			}
		}

		if (builder.indexOf('X') != -1 && builder.last() != 'X') return null
		if (builder.count { it == 'X' } > 1) return null
		return builder.toString()
	}

	/**
	 * Whether [value] needs no separator stripping (digits only, optional trailing `X`).
	 */
	private fun isAlreadyNormalized(value: String): Boolean {
		if (value.isEmpty()) return false
		var xCount = 0
		for (i in value.indices) {
			when (value[i]) {
				in '0'..'9' -> Unit
				'x', 'X' -> {
					if (i != value.lastIndex) return false
					xCount++
				}
				else -> return false
			}
		}
		return xCount <= 1
	}

	/**
	 * Checks an EAN-8 or EAN-13 payload.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Payload from [normalize].
	 * @return Outcome; [BarcodeValidationOutcome.FORMAT_INVALID] for any other length.
	 */
	fun validateEan(normalized: String): BarcodeValidationOutcome =
		when (normalized.length) {
			8, 13 -> validateGs1Digits(normalized)
			else -> BarcodeValidationOutcome.FORMAT_INVALID
		}

	/**
	 * Checks a UPC-A payload.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Payload from [normalize].
	 * @return Outcome; [BarcodeValidationOutcome.FORMAT_INVALID] unless the payload is 12 digits.
	 */
	fun validateUpc(normalized: String): BarcodeValidationOutcome =
		when {
			normalized.length == 12 && normalized.all(Char::isDigit) ->
				if (passesGs1Mod10Check(normalized)) {
					BarcodeValidationOutcome.VALID
				} else {
					BarcodeValidationOutcome.CHECKSUM_INVALID
				}
			else -> BarcodeValidationOutcome.FORMAT_INVALID
		}

	/**
	 * Checks a GTIN-8, GTIN-12, GTIN-13, or GTIN-14 payload.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Payload from [normalize].
	 * @return Outcome; [BarcodeValidationOutcome.FORMAT_INVALID] for any other length.
	 */
	fun validateGtin(normalized: String): BarcodeValidationOutcome =
		when (normalized.length) {
			8, 12, 13, 14 -> validateGs1Digits(normalized)
			else -> BarcodeValidationOutcome.FORMAT_INVALID
		}

	/**
	 * Checks an ISBN-10 or ISBN-13 payload; ISBN-13 additionally requires a `978`/`979` prefix.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Payload from [normalize]; ISBN-10 may end in `X`.
	 * @return Outcome; [BarcodeValidationOutcome.FORMAT_INVALID] for any other length.
	 */
	fun validateIsbn(normalized: String): BarcodeValidationOutcome =
		when (normalized.length) {
			10 -> if (passesIsbn10Check(normalized)) {
				BarcodeValidationOutcome.VALID
			} else {
				BarcodeValidationOutcome.CHECKSUM_INVALID
			}
			13 -> when {
				!normalized.startsWith("978") && !normalized.startsWith("979") ->
					BarcodeValidationOutcome.FORMAT_INVALID
				!normalized.all(Char::isDigit) -> BarcodeValidationOutcome.FORMAT_INVALID
				passesGs1Mod10Check(normalized) -> BarcodeValidationOutcome.VALID
				else -> BarcodeValidationOutcome.CHECKSUM_INVALID
			}
			else -> BarcodeValidationOutcome.FORMAT_INVALID
		}

	/**
	 * Checks an ISSN payload.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Payload from [normalize]; may end in `X`.
	 * @return Outcome; [BarcodeValidationOutcome.FORMAT_INVALID] unless the payload is 8 characters.
	 */
	fun validateIssn(normalized: String): BarcodeValidationOutcome =
		when {
			normalized.length != 8 -> BarcodeValidationOutcome.FORMAT_INVALID
			!passesIssnCheck(normalized) -> BarcodeValidationOutcome.CHECKSUM_INVALID
			else -> BarcodeValidationOutcome.VALID
		}

	/**
	 * Shared digits-only plus mod-10 check for the GS1 family.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Payload already accepted on length by the caller.
	 * @return Outcome for this payload.
	 */
	private fun validateGs1Digits(normalized: String): BarcodeValidationOutcome =
		when {
			!normalized.all(Char::isDigit) -> BarcodeValidationOutcome.FORMAT_INVALID
			passesGs1Mod10Check(normalized) -> BarcodeValidationOutcome.VALID
			else -> BarcodeValidationOutcome.CHECKSUM_INVALID
		}

	/**
	 * GS1 mod-10 check digit (EAN, UPC, GTIN, ISBN-13).
	 *
	 * Side effects: none.
	 *
	 * @param digits Digits-only payload whose last character is the check digit.
	 * @return `true` when the weighted sum plus the check digit is divisible by 10.
	 */
	fun passesGs1Mod10Check(digits: String): Boolean {
		var sum = 0
		var weightThree = true
		for (i in digits.lastIndex - 1 downTo 0) {
			val digit = digits[i] - '0'
			sum += if (weightThree) digit * 3 else digit
			weightThree = !weightThree
		}
		val checkDigit = digits.last() - '0'
		return (sum + checkDigit) % 10 == 0
	}

	/**
	 * ISBN-10 weighted mod-11 check; final character may be `0`–`9` or `X`.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Candidate payload; anything other than 10 characters fails.
	 * @return `true` when the weighted sum is divisible by 11.
	 */
	fun passesIsbn10Check(normalized: String): Boolean {
		if (normalized.length != 10) return false
		var sum = 0
		for (i in 0 until 9) {
			val ch = normalized[i]
			if (ch !in '0'..'9') return false
			sum += (ch - '0') * (10 - i)
		}
		val checkValue = when (val checkChar = normalized[9]) {
			in '0'..'9' -> checkChar - '0'
			'X' -> 10
			else -> return false
		}
		return (sum + checkValue) % 11 == 0
	}

	/**
	 * ISSN mod-11 check over seven data digits; check digit is `0`–`9` or `X`.
	 *
	 * Side effects: none.
	 *
	 * @param normalized Candidate payload; anything other than 8 characters fails.
	 * @return `true` when the eighth character matches the computed check character.
	 */
	fun passesIssnCheck(normalized: String): Boolean {
		if (normalized.length != 8) return false
		for (i in 0 until 7) {
			if (normalized[i] !in '0'..'9') return false
		}
		var sum = 0
		for (i in 0 until 7) {
			sum += (normalized[i] - '0') * (8 - i)
		}
		val expectedCheck = when (val checkDigit = 11 - (sum % 11)) {
			11 -> '0'
			10 -> 'X'
			else -> ('0'.code + checkDigit).toChar()
		}
		return normalized[7].uppercaseChar() == expectedCheck
	}

	/**
	 * Human-readable accepted layouts for [type], used in format error metadata.
	 *
	 * Side effects: none.
	 *
	 * @param type Declared barcode family.
	 * @return One label per accepted layout, for example `"EAN-13 (13 digits)"`.
	 */
	fun expectedFormats(type: Barcode.Type): List<String> =
		when (type) {
			Barcode.Type.EAN -> listOf("EAN-8 (8 digits)", "EAN-13 (13 digits)")
			Barcode.Type.UPC -> listOf("UPC-A (12 digits)")
			Barcode.Type.GTIN -> listOf(
				"GTIN-8 (8 digits)",
				"GTIN-12 (12 digits)",
				"GTIN-13 (13 digits)",
				"GTIN-14 (14 digits)")
			Barcode.Type.ISBN -> listOf("ISBN-10 (10 characters)", "ISBN-13 (13 digits, 978/979 prefix)")
			Barcode.Type.ISSN -> listOf("ISSN (8 characters, 7 digits + check)")
		}
}

/**
 * Result of one barcode family check.
 * 
 * @author Ghaylan Saada

 */
internal enum class BarcodeValidationOutcome {

	/**
	 * Layout and check character both pass.

	 */
	VALID,

	/**
	 * Length or character set does not match the declared family.

	 */
	FORMAT_INVALID,

	/**
	 * Layout is correct but the check character does not match.

	 */
	CHECKSUM_INVALID,
}
