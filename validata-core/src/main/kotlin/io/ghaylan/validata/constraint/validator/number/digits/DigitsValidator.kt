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
package io.ghaylan.validata.constraint.validator.number.digits

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.DigitsConstraint
import io.ghaylan.validata.ext.toBigDecimalOrNull
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Limits integer and fractional digit counts on a numeric value (Bean Validation `@Digits`
 * semantics).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Failures use
 * [ConstraintErrorCode.NUMBER_PRECISION_EXCEEDED], [ConstraintErrorCode.NUMBER_SCALE_EXCEEDED], or
 * [ConstraintErrorCode.VALUE_PARSING_FAILED] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object DigitsValidator : ConstraintValidator<Number, DigitsConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure for subject [type].
	 *
	 * Integral leaves never take the finite-decimal parse path and never have a fractional
	 * scale, so [ConstraintErrorCode.VALUE_PARSING_FAILED] and
	 * [ConstraintErrorCode.NUMBER_SCALE_EXCEEDED] are omitted for those types.
	 */
	override fun possibleErrorCodes(
		constraint: DigitsConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>(
			ConstraintErrorCode.NUMBER_PRECISION_EXCEEDED)
		if (!SubjectTypes.isStrictIntegralNumber(type)) {
			codes += ConstraintErrorCode.VALUE_PARSING_FAILED
			codes += ConstraintErrorCode.NUMBER_SCALE_EXCEEDED
		}
		return codes
	}
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.	 
	 */
	override fun validate(
		value: Number,
		constraint: DigitsConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val actual = value.toBigDecimalOrNull() ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Must be a finite decimal number.",
			metadata = constraint,
		)
		val abs = actual.abs()
		val scale = abs.scale()
		val actualInteger = (abs.precision() - scale).coerceAtLeast(0)
		val actualFraction = scale.coerceAtLeast(0)
		
		if (actualInteger > constraint.integer) {
			return ConstraintError(
				code = ConstraintErrorCode.NUMBER_PRECISION_EXCEEDED,
				message = "Must have at most ${constraint.integer} digits before the decimal point.",
				metadata = constraint,
			)
		}
		
		if (actualFraction > constraint.fraction) {
			return ConstraintError(
				code = ConstraintErrorCode.NUMBER_SCALE_EXCEEDED,
				message = "Must have at most ${constraint.fraction} digits after the decimal point.",
				metadata = constraint,
			)
		}
		
		return null
	}
}
