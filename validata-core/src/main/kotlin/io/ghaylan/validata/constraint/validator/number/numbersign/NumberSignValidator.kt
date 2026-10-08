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
package io.ghaylan.validata.constraint.validator.number.numbersign

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.NumberSign
import io.ghaylan.validata.constraint.annotation.NumberSignConstraint
import io.ghaylan.validata.ext.toBigDecimalOrNull
import io.ghaylan.validata.ext.toIntegralLongOrNull
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.math.BigDecimal

/**
 * Requires a number to be positive or negative per [NumberSignConstraint.sign].
 *
 * When [NumberSignConstraint.allowZero] is `false`, zero fails with
 * [ConstraintErrorCode.NUMBER_ZERO_NOT_ALLOWED]. Integral values use a fast long path;
 * other numerics convert via [toBigDecimalOrNull] (non-finite →
 * [ConstraintErrorCode.VALUE_PARSING_FAILED]).
 *
 * Null subjects are skipped by the engine (presence is `@Required`).
 * See [possibleErrorCodes] for the sign / allowZero-dependent code set; failures attach
 * the failing constraint.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object NumberSignValidator : ConstraintValidator<Number, NumberSignConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint.sign] / [constraint.allowZero].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata selecting the code set.
	 * @return Parsing failure plus sign / zero codes that can fire for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: NumberSignConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>()
		// Integral leaves use the long fast-path; only fractional / coarse Number can fail parse.
		if (!SubjectTypes.isStrictIntegralNumber(type)) {
			codes += ConstraintErrorCode.VALUE_PARSING_FAILED
		}
		when (constraint.sign) {
			NumberSign.Sign.POSITIVE -> {
				codes += ConstraintErrorCode.NUMBER_NOT_POSITIVE
				if (!constraint.allowZero) codes += ConstraintErrorCode.NUMBER_ZERO_NOT_ALLOWED
			}
			NumberSign.Sign.NEGATIVE -> {
				codes += ConstraintErrorCode.NUMBER_NOT_NEGATIVE
				if (!constraint.allowZero) codes += ConstraintErrorCode.NUMBER_ZERO_NOT_ALLOWED
			}
		}
		return codes
	}

	/**
	 * Validates [value] against [constraint] sign policy.
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint when applicable, or `null`
	 *   when valid.
	 */
	override fun validate(
		value: Number,
		constraint: NumberSignConstraint,
		context: ValidationContext,
	): ConstraintError<*>? = when (constraint.sign) {
		NumberSign.Sign.POSITIVE -> validatePositive(value, constraint)
		NumberSign.Sign.NEGATIVE -> validateNegative(value, constraint)
	}

	/**
	 * Requires [value] ≥ 0 (or > 0 when [NumberSignConstraint.allowZero] is `false`).
	 *
	 * Side effects: none.
	 */
	private fun validatePositive(
		value: Number,
		constraint: NumberSignConstraint,
	): ConstraintError<*>? {
		val allowZero = constraint.allowZero
		val integral = value.toIntegralLongOrNull()
		if (integral != null) {
			if (integral < 0L) {
				return positiveFail(constraint, "Must be a positive number.")
			}
			if (integral == 0L && !allowZero) {
				return zeroFailPositive(constraint)
			}
			return null
		}
		val actual = value.toBigDecimalOrNull()
			?: return parseFail(constraint)
		val cmp = actual.compareTo(BigDecimal.ZERO)
		if (cmp < 0) return positiveFail(constraint, "Must be a positive number.")
		if (cmp == 0 && !allowZero) return zeroFailPositive(constraint)
		return null
	}

	/**
	 * Requires [value] ≤ 0 (or < 0 when [NumberSignConstraint.allowZero] is `false`).
	 *
	 * Side effects: none.
	 */
	private fun validateNegative(
		value: Number,
		constraint: NumberSignConstraint,
	): ConstraintError<*>? {
		val allowZero = constraint.allowZero
		val integral = value.toIntegralLongOrNull()
		if (integral != null) {
			if (integral > 0L) {
				return negativeFail(constraint, "Must be a negative number.")
			}
			if (integral == 0L && !allowZero) {
				return zeroFailNegative(constraint)
			}
			return null
		}
		val actual = value.toBigDecimalOrNull()
			?: return parseFail(constraint)
		val cmp = actual.compareTo(BigDecimal.ZERO)
		if (cmp > 0) return negativeFail(constraint, "Must be a negative number.")
		if (cmp == 0 && !allowZero) return zeroFailNegative(constraint)
		return null
	}

	private fun parseFail(
		constraint: NumberSignConstraint,
	) = ConstraintError(
		code = ConstraintErrorCode.VALUE_PARSING_FAILED,
		message = "Must be a finite decimal number.",
		metadata = constraint,
	)

	private fun positiveFail(
		constraint: NumberSignConstraint,
		message: String,
	) = ConstraintError(
		code = ConstraintErrorCode.NUMBER_NOT_POSITIVE,
		message = message,
		metadata = constraint,
	)

	private fun negativeFail(
		constraint: NumberSignConstraint,
		message: String,
	) = ConstraintError(
		code = ConstraintErrorCode.NUMBER_NOT_NEGATIVE,
		message = message,
		metadata = constraint,
	)

	private fun zeroFailPositive(
		constraint: NumberSignConstraint,
	) = ConstraintError(
		code = ConstraintErrorCode.NUMBER_ZERO_NOT_ALLOWED,
		message = "Must be greater than zero.",
		metadata = constraint,
	)

	private fun zeroFailNegative(
		constraint: NumberSignConstraint,
	) = ConstraintError(
		code = ConstraintErrorCode.NUMBER_ZERO_NOT_ALLOWED,
		message = "Must be less than zero.",
		metadata = constraint,
	)
}
