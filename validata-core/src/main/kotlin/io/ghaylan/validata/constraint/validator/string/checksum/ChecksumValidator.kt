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

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Checksum
import io.ghaylan.validata.constraint.annotation.ChecksumConstraint
import io.ghaylan.validata.constraint.validator.string.financial.FinancialCodeSupport
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates a check digit / checksum over a configurable substring of a [CharSequence].
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Slice bounds come from
 * [ChecksumConstraint.startIndex] / [ChecksumConstraint.endIndex]; the check digit index is
 * relative to that slice.
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID], [ConstraintErrorCode.VALUE_CHECKSUM_INVALID].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object ChecksumValidator : ConstraintValidator<CharSequence, ChecksumConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: ChecksumConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_FORMAT_INVALID,
		ConstraintErrorCode.VALUE_CHECKSUM_INVALID)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: ChecksumConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val text = value.toString()
		val end = if (constraint.endIndex < 0) text.length else constraint.endIndex
		if (constraint.startIndex !in 0..end || end > text.length) {
			return formatError(
				constraint,
				"Checksum slice indices are out of range for the value.",
			)
		}

		val slice = text.substring(constraint.startIndex, end)
		if (slice.isEmpty()) {
			return formatError(
				constraint,
				"Checksum slice must not be empty.",
			)
		}

		val checkIdx = if (constraint.checkDigitIndex < 0) slice.lastIndex else constraint.checkDigitIndex
		if (checkIdx !in slice.indices) {
			return formatError(
				constraint,
				"Checksum check-digit index is out of range for the validated slice.",
			)
		}

		return when (constraint.algorithm) {
			Checksum.Algorithm.LUHN, Checksum.Algorithm.MOD10 ->
				validateLuhn(slice, checkIdx, constraint)
			Checksum.Algorithm.MOD11 ->
				validateMod11(slice, checkIdx, constraint)
			Checksum.Algorithm.VERHOEFF ->
				validateVerhoeff(slice, checkIdx, constraint)
			Checksum.Algorithm.DAMM ->
				validateDamm(slice, checkIdx, constraint)
			Checksum.Algorithm.MOD97_10 ->
				validateMod97(slice, constraint)
		}
	}

	private fun validateLuhn(
		slice: String,
		checkIdx: Int,
		constraint: ChecksumConstraint,
	): ConstraintError<*>? {
		val prepared = prepareDigits(slice, checkIdx, constraint.ignoreNonDigits, allowX = false)
			?: return formatError(
				constraint,
				"Value must contain only digits for Luhn / Mod-10 checksum.",
			)
		if (prepared.length < 2) {
			return formatError(
				constraint,
				"Value is too short for Luhn / Mod-10 checksum.",
			)
		}
		return if (passesLuhn(prepared)) null else checksumError(constraint)
	}

	private fun validateMod11(
		slice: String,
		checkIdx: Int,
		constraint: ChecksumConstraint,
	): ConstraintError<*>? {
		val prepared = prepareDigits(slice, checkIdx, constraint.ignoreNonDigits, allowX = true)
			?: return formatError(
				constraint,
				"Value must contain only digits (check digit may be X) for Mod-11 checksum.",
			)
		if (prepared.length < 2) {
			return formatError(
				constraint,
				"Value is too short for Mod-11 checksum.",
			)
		}
		return if (passesMod11(prepared)) null else checksumError(constraint)
	}

	private fun validateVerhoeff(
		slice: String,
		checkIdx: Int,
		constraint: ChecksumConstraint,
	): ConstraintError<*>? {
		val prepared = prepareDigits(slice, checkIdx, constraint.ignoreNonDigits, allowX = false)
			?: return formatError(
				constraint,
				"Value must contain only digits for Verhoeff checksum.",
			)
		if (prepared.isEmpty()) {
			return formatError(
				constraint,
				"Value is too short for Verhoeff checksum.",
			)
		}
		return if (passesVerhoeff(prepared)) null else checksumError(constraint)
	}

	private fun validateDamm(
		slice: String,
		checkIdx: Int,
		constraint: ChecksumConstraint,
	): ConstraintError<*>? {
		val prepared = prepareDigits(slice, checkIdx, constraint.ignoreNonDigits, allowX = false)
			?: return formatError(
				constraint,
				"Value must contain only digits for Damm checksum.",
			)
		if (prepared.isEmpty()) {
			return formatError(
				constraint,
				"Value is too short for Damm checksum.",
			)
		}
		return if (passesDamm(prepared)) null else checksumError(constraint)
	}

	private fun validateMod97(
		slice: String,
		constraint: ChecksumConstraint,
	): ConstraintError<*>? {
		val prepared = buildString {
			for (ch in slice.uppercase()) {
				when {
					ch.isDigit() || ch in 'A'..'Z' -> append(ch)
					constraint.ignoreNonDigits -> Unit
					else -> return formatError(
						constraint,
						"Value may contain only letters and digits for Mod-97-10 checksum.",
					)
				}
			}
		}
		if (prepared.length < 2) {
			return formatError(
				constraint,
				"Value is too short for Mod-97-10 checksum.",
			)
		}
		return if (FinancialCodeSupport.verifyMod97(prepared)) null else checksumError(constraint)
	}

	/**
	 * Builds a digit string with the check digit moved to the end.
	 *
	 * When [ignoreNonDigits] is `false`, every character must be a digit (or `X`/`x` at the check
	 * position when [allowX] is `true`). When `true`, non-digits are dropped except the check
	 * character itself (kept so its position remains meaningful until rearrangement).
	 */
	private fun prepareDigits(
		slice: String,
		checkIdx: Int,
		ignoreNonDigits: Boolean,
		allowX: Boolean,
	): String? {
		val checkChar = slice[checkIdx]
		val checkIsX = allowX && checkChar.uppercaseChar() == 'X'
		if (!checkChar.isDigit() && !checkIsX) {
			if (!ignoreNonDigits) return null
			// Check position itself must still be a digit (or X for MOD11).
			return null
		}

		val body = StringBuilder(slice.length)
		for (i in slice.indices) {
			if (i == checkIdx) continue
			val ch = slice[i]
			when {
				ch.isDigit() -> body.append(ch)
				ignoreNonDigits -> Unit
				else -> return null
			}
		}
		val checkOut = if (checkIsX) 'X' else checkChar
		return body.append(checkOut).toString()
	}

	/**
	 * Classic Luhn (Mod-10): process right-to-left, double every second digit, sum ≡ 0 (mod 10).
	 */
	internal fun passesLuhn(digits: String): Boolean {
		var sum = 0
		var doubleDigit = false
		for (i in digits.lastIndex downTo 0) {
			val c = digits[i]
			if (c !in '0'..'9') return false
			var d = c - '0'
			if (doubleDigit) {
				d *= 2
				if (d > 9) d -= 9
			}
			sum += d
			doubleDigit = !doubleDigit
		}
		return sum % 10 == 0
	}

	/**
	 * Weighted Mod-11 (ISBN-10 style): weights length..1; check digit may be `X` for 10.
	 */
	internal fun passesMod11(value: String): Boolean {
		val n = value.length
		var sum = 0
		for (i in 0 until n - 1) {
			val ch = value[i]
			if (ch !in '0'..'9') return false
			sum += (ch - '0') * (n - i)
		}
		val checkValue = when (val checkChar = value[n - 1].uppercaseChar()) {
			in '0'..'9' -> checkChar - '0'
			'X' -> 10
			else -> return false
		}
		return (sum + checkValue) % 11 == 0
	}

	/** Verhoeff dihedral-group algorithm; valid when the intermediate value ends at 0. */
	internal fun passesVerhoeff(digits: String): Boolean {
		var c = 0
		for (i in digits.indices) {
			val ch = digits[digits.lastIndex - i]
			if (ch !in '0'..'9') return false
			c = VERHOEFF_D[c][VERHOEFF_P[i % 8][ch - '0']]
		}
		return c == 0
	}

	/** Damm quasigroup algorithm; valid when the intermediate value ends at 0. */
	internal fun passesDamm(digits: String): Boolean {
		var interim = 0
		for (ch in digits) {
			if (ch !in '0'..'9') return false
			interim = DAMM_TABLE[interim][ch - '0']
		}
		return interim == 0
	}

	private fun formatError(
		constraint: ChecksumConstraint,
		message: String,
	) = ConstraintError(
		code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
		message = message,
		metadata = constraint,
	)

	private fun checksumError(
		constraint: ChecksumConstraint,
	) = ConstraintError(
		code = ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
		message = "Checksum is not valid; check the value for a typing error.",
		metadata = constraint,
	)

	// Verhoeff multiplication table (dihedral group D5).
	private val VERHOEFF_D = arrayOf(
		intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
		intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
		intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
		intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
		intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
		intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
		intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
		intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
		intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
		intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0))

	// Verhoeff permutation table.
	private val VERHOEFF_P = arrayOf(
		intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
		intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
		intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
		intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
		intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
		intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
		intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
		intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8))

	// Damm quasigroup operation table.
	private val DAMM_TABLE = arrayOf(
		intArrayOf(0, 3, 1, 7, 5, 9, 8, 6, 4, 2),
		intArrayOf(7, 0, 9, 2, 1, 5, 4, 8, 6, 3),
		intArrayOf(4, 2, 0, 6, 8, 7, 1, 3, 5, 9),
		intArrayOf(1, 7, 5, 0, 9, 8, 3, 4, 2, 6),
		intArrayOf(6, 1, 2, 3, 0, 4, 5, 9, 7, 8),
		intArrayOf(3, 6, 7, 4, 2, 0, 9, 5, 8, 1),
		intArrayOf(5, 8, 6, 9, 7, 2, 0, 1, 3, 4),
		intArrayOf(8, 9, 4, 5, 3, 6, 2, 0, 1, 7),
		intArrayOf(9, 4, 3, 8, 6, 1, 7, 2, 0, 5),
		intArrayOf(2, 5, 8, 1, 4, 3, 6, 7, 9, 0))
}
