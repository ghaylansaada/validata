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
package io.ghaylan.validata.constraint.validator.number.numberparity

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.NumberParity
import io.ghaylan.validata.constraint.annotation.NumberParityConstraint
import io.ghaylan.validata.ext.toBigDecimalOrNull
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.math.BigDecimal

/**
 * Requires a number to be an even or odd whole integer per [NumberParityConstraint.value].
 *
 * Non-finite values fail with [ConstraintErrorCode.VALUE_PARSING_FAILED]; non-integers with
 * [ConstraintErrorCode.NUMBER_NOT_INTEGER]; wrong parity with
 * [ConstraintErrorCode.NUMBER_NOT_EVEN] or [ConstraintErrorCode.NUMBER_NOT_ODD].
 *
 * Null subjects are skipped by the engine (presence is `@Required`).
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object NumberParityValidator : ConstraintValidator<Number, NumberParityConstraint>() {

	private val TWO = BigDecimal("2")

	/**
	 * Error codes this validator may emit for [constraint.value].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata selecting even vs odd failure code.
	 * @return Parsing / integer / parity codes that can fire for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: NumberParityConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val parityCode = when (constraint.value) {
			NumberParity.Value.EVEN -> ConstraintErrorCode.NUMBER_NOT_EVEN
			NumberParity.Value.ODD -> ConstraintErrorCode.NUMBER_NOT_ODD
		}
		val codes = linkedSetOf<ConstraintErrorDefinition>(parityCode)
		if (!SubjectTypes.isStrictIntegralNumber(type)) {
			codes += ConstraintErrorCode.VALUE_PARSING_FAILED
			codes += ConstraintErrorCode.NUMBER_NOT_INTEGER
		}
		return codes
	}

	/**
	 * Validates [value] against [constraint] parity.
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid.
	 */
	override fun validate(
		value: Number,
		constraint: NumberParityConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val actual = value.toBigDecimalOrNull()
			?: return ConstraintError(
				code = ConstraintErrorCode.VALUE_PARSING_FAILED,
				message = "Must be a finite decimal number.",
				metadata = constraint,
			)
		if (actual.stripTrailingZeros().scale() != 0) {
			return ConstraintError(
				code = ConstraintErrorCode.NUMBER_NOT_INTEGER,
				message = "Must be a whole number.",
				metadata = constraint,
			)
		}
		val even = actual.remainder(TWO).compareTo(BigDecimal.ZERO) == 0
		val ok = when (constraint.value) {
			NumberParity.Value.EVEN -> even
			NumberParity.Value.ODD -> !even
		}
		if (ok) return null
		return when (constraint.value) {
			NumberParity.Value.EVEN -> ConstraintError(
				code = ConstraintErrorCode.NUMBER_NOT_EVEN,
				message = "Must be an even integer.",
				metadata = constraint,
			)
			NumberParity.Value.ODD -> ConstraintError(
				code = ConstraintErrorCode.NUMBER_NOT_ODD,
				message = "Must be an odd integer.",
				metadata = constraint,
			)
		}
	}
}
