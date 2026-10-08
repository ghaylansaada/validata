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
package io.ghaylan.validata.constraint.validator.string.creditcard

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.CreditCardConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Runs Luhn / card-number structure checks so obviously invalid PANs are rejected at validation.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Digits are scanned in place — no intermediate
 * [String] / [List] of boxed digits — so the success path stays allocation-light aside from
 * the inevitable CharSequence walk.
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID],
 * [ConstraintErrorCode.VALUE_CHECKSUM_INVALID].
 *
 * Messages state the accepted card-number format only. No part of the submitted PAN — not even
 * its digit count — reaches the message or an error context.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object CreditCardValidator : ConstraintValidator<CharSequence, CreditCardConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: CreditCardConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
		ConstraintErrorCode.VALUE_FORMAT_INVALID)

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
		constraint: CreditCardConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {

		var digitCount = 0
		for (i in value.indices) {
			when (value[i]) {
				in '0'..'9' -> digitCount++
				' ', '-' -> Unit
				else -> return ConstraintError(
					code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
					message = "Card number may contain only digits, spaces, and dashes.",
					metadata = constraint,
				)
			}
		}
		// Major networks: 13–19 digits (ISO/IEC 7812).
		if (digitCount !in 13..19) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "Card number must contain between 13 and 19 digits.",
				metadata = constraint,
			)
		}

		if (!passesLuhn(value)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
				message = "Card number is not valid; check it for a typing error.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Luhn checksum over digit characters only (skips spaces/dashes).
	 *
	 * Processes right-to-left without allocating digit lists. Side effects: none.
	 *
	 * @param value PAN text (digits with optional spaces/dashes).
	 * @return `true` when the Luhn sum is congruent to 0 mod 10.
	 */
	private fun passesLuhn(value: CharSequence): Boolean {
		var sum = 0
		var doubleDigit = false
		for (i in value.lastIndex downTo 0) {
			val c = value[i]
			if (c !in '0'..'9') continue
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
}
