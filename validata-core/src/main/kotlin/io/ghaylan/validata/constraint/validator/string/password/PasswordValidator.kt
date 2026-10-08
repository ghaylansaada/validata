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

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.PasswordConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Enforces password complexity declared on [PasswordConstraint]: length bounds, optional
 * character-class requirements, sequential-run rejection, and repetitive-pattern rejection.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Checks run in fixed order and
 * return the first failing rule — metadata is allocated only on the failure path:
 * length → uppercase → lowercase → digit → special → sequential → repetitive.
 *
 * Errors: [ConstraintErrorCode.TEXT_TOO_SHORT] / [ConstraintErrorCode.TEXT_TOO_LONG],
 * [ConstraintErrorCode.TEXT_PATTERN_MISMATCH] for missing character classes and for
 * sequential / repetitive pattern rejection.
 * Failures attach the [PasswordConstraint] on [ConstraintError.metadata].
 *
 * Messages state the rule only. The submitted password and its length never appear in message
 * text, so echoed errors cannot narrow a secret for an attacker.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object PasswordValidator : ConstraintValidator<CharSequence, PasswordConstraint>() {

	/** Minimum run length for ascending/descending letter or digit sequences. */
	private const val MIN_SEQUENTIAL_RUN = 3

	/** Minimum same-character or repeating-block length treated as repetitive. */
	private const val MIN_REPETITIVE_RUN = 3

	/**
	 * Error codes this validator may emit for [constraint] length, character-class, and pattern rules.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Password policy; omits length/pattern codes that cannot fire.
	 * @param type Subject runtime type (unused for password).
	 * @return Codes for the active rules.
	 */
	override fun possibleErrorCodes(
		constraint: PasswordConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>()
		if (constraint.minLength > 0) codes += ConstraintErrorCode.TEXT_TOO_SHORT
		if (constraint.maxLength < Int.MAX_VALUE) codes += ConstraintErrorCode.TEXT_TOO_LONG
		if (constraint.requireUppercase ||
			constraint.requireLowercase ||
			constraint.requireDigit ||
			constraint.requireSpecialChar ||
			constraint.noSequentialChars ||
			constraint.noRepetitivePatterns
		) {
			codes += ConstraintErrorCode.TEXT_PATTERN_MISMATCH
		}
		return codes
	}

	/**
	 * Validates [value] against [constraint].
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached, or `null` when valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: PasswordConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		lengthError(value, constraint)?.let { return it }
		characterClassError(value, constraint)?.let { return it }
		if (constraint.noSequentialChars && hasSequentialRun(value)) {
			return ConstraintError(
				code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				message = "Password must not contain sequential characters.",
				metadata = constraint,
			)
		}
		if (constraint.noRepetitivePatterns && hasRepetitivePattern(value)) {
			return ConstraintError(
				code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				message = "Password must not contain repetitive patterns.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Checks [value] against the declared length bounds.
	 *
	 * States the required bound only; the submitted length never reaches the message.
	 * Side effects: none.
	 *
	 * @param value Password text under test.
	 * @param constraint Declared length bounds.
	 * @return Length violation, or `null` when within bounds.
	 */
	private fun lengthError(
		value: CharSequence,
		constraint: PasswordConstraint,
	): ConstraintError<*>? {
		if (value.length < constraint.minLength) {
			return ConstraintError(
				code = ConstraintErrorCode.TEXT_TOO_SHORT,
				message = "Password must be at least ${constraint.minLength} characters long.",
				metadata = constraint,
			)
		}
		if (value.length > constraint.maxLength) {
			return ConstraintError(
				code = ConstraintErrorCode.TEXT_TOO_LONG,
				message = "Password must be at most ${constraint.maxLength} characters long.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Checks [value] for each character class [constraint] requires, in fixed order.
	 *
	 * Side effects: none.
	 *
	 * @param value Password text under test.
	 * @param constraint Declared character-class requirements and special-character set.
	 * @return First missing-class violation, or `null` when every required class is present.
	 */
	private fun characterClassError(
		value: CharSequence,
		constraint: PasswordConstraint,
	): ConstraintError<*>? {
		if (constraint.requireUppercase && value.none(Char::isUpperCase)) {
			return missingClass(constraint, "an uppercase letter")
		}
		if (constraint.requireLowercase && value.none(Char::isLowerCase)) {
			return missingClass(constraint, "a lowercase letter")
		}
		if (constraint.requireDigit && value.none(Char::isDigit)) {
			return missingClass(constraint, "a digit")
		}
		if (constraint.requireSpecialChar && value.none { constraint.allowedSpecialChars.contains(it) }) {
			return ConstraintError(
				code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				message = "Password must contain a special character from the allowed set.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Builds a missing-character-class violation.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Password policy that required the class.
	 * @param label Lowercase noun phrase for the message, including its article
	 *   (for example `"an uppercase letter"`).
	 * @return Violation with [ConstraintErrorCode.TEXT_PATTERN_MISMATCH].
	 */
	private fun missingClass(
		constraint: PasswordConstraint,
		label: String,
	): ConstraintError<*> = ConstraintError(
		code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
		message = "Password must contain $label.",
		metadata = constraint,
	)

	/**
	 * Whether [value] contains [MIN_SEQUENTIAL_RUN] or more letters or digits in ascending or
	 * descending alphabet/digit order (case-insensitive for letters).
	 *
	 * Side effects: none.
	 *
	 * @param value Password text under test.
	 * @return `true` when a sequential run is present.
	 */
	private fun hasSequentialRun(value: CharSequence): Boolean {
		if (value.length < MIN_SEQUENTIAL_RUN) return false
		var run = 1
		var direction = 0 // +1 ascending, -1 descending, 0 unset
		for (i in 1 until value.length) {
			val prev = sequenceKey(value[i - 1])
			val curr = sequenceKey(value[i])
			val step = if (prev != null && curr != null && prev.family == curr.family) {
				curr.ord - prev.ord
			} else {
				null
			}
			when (step) {
				1, -1 -> {
					if (direction == 0 || direction == step) {
						direction = step
						run++
						if (run >= MIN_SEQUENTIAL_RUN) return true
					} else {
						direction = step
						run = 2
					}
				}
				else -> {
					run = 1
					direction = 0
				}
			}
		}
		return false
	}

	/**
	 * Digit or letter identity for sequential-run detection (`null` when not sequenced).
	 *
	 * @property family `0` = digit, `1` = letter (case-insensitive).
	 * @property ord Ordinal within that family (`0`–`9` or `0`–`25`).
	 */
	private data class SequenceKey(
		val family: Int,
		val ord: Int,
	)

	/**
	 * Maps [c] to a [SequenceKey], or `null` when not a digit/letter.
	 *
	 * Side effects: none.
	 */
	private fun sequenceKey(c: Char): SequenceKey? = when {
		c.isDigit() -> SequenceKey(0, c - '0')
		c.isLetter() -> SequenceKey(1, c.lowercaseChar() - 'a')
		else -> null
	}

	/**
	 * Whether [value] contains [MIN_REPETITIVE_RUN] identical characters in a row, or a block of
	 * length ≥ 2 repeated consecutively at least twice (e.g. `"abcabc"`).
	 *
	 * Side effects: none.
	 *
	 * @param value Password text under test.
	 * @return `true` when a repetitive pattern is present.
	 */
	private fun hasRepetitivePattern(value: CharSequence): Boolean {
		if (value.length < MIN_REPETITIVE_RUN) return false
		var sameRun = 1
		for (i in 1 until value.length) {
			if (value[i] == value[i - 1]) {
				sameRun++
				if (sameRun >= MIN_REPETITIVE_RUN) return true
			} else {
				sameRun = 1
			}
		}
		val n = value.length
		val maxPeriod = n / 2
		for (period in 2..maxPeriod) {
			var start = 0
			while (start + period * 2 <= n) {
				var repeats = 1
				while (start + period * (repeats + 1) <= n &&
					equalRange(value, start, start + period * repeats, period)
				) {
					repeats++
					if (repeats >= 2) return true
				}
				start++
			}
		}
		return false
	}

	/**
	 * Whether the [period]-length slice at [left] equals the slice at [right].
	 *
	 * Side effects: none.
	 */
	private fun equalRange(
		value: CharSequence,
		left: Int,
		right: Int,
		period: Int,
	): Boolean {
		for (i in 0 until period) {
			if (value[left + i] != value[right + i]) return false
		}
		return true
	}
}
