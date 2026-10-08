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
package io.ghaylan.validata.constraint.validator.number.multiple

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MultipleOfConstraint
import io.ghaylan.validata.ext.toBigDecimalOrNull
import io.ghaylan.validata.ext.toConstraintNumber
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.math.BigDecimal

/**
 * Requires a number to be an exact multiple of a configured step (decimal string factor).
 *
 * Factor parse is memoized per [MultipleOfConstraint] instance. Non-positive factors are
 * treated as a no-op (pass) — misconfiguration should be caught at schema-build / KSP time.
 * Failures use [ConstraintErrorCode.NUMBER_NOT_MULTIPLE] or
 * [ConstraintErrorCode.VALUE_PARSING_FAILED] — the failing constraint is attached when applicable.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object MultipleOfValidator : ConstraintValidator<Number, MultipleOfConstraint>() {

	/**
	 * Error codes this validator may emit on failure for subject [type].
	 *
	 * [ConstraintErrorCode.VALUE_PARSING_FAILED] is omitted for strict integral leaves — they
	 * never take the subject NaN path, and KSP typed-literal checks make a bad factor unreachable
	 * for generated schemas.
	 */
	override fun possibleErrorCodes(
		constraint: MultipleOfConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>(ConstraintErrorCode.NUMBER_NOT_MULTIPLE)
		if (!SubjectTypes.isStrictIntegralNumber(type)) {
			codes += ConstraintErrorCode.VALUE_PARSING_FAILED
		}
		return codes
	}

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: may memoize a parsed literal in [ConstraintLiteralCache] on miss.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint when applicable, or `null`
	 *   when valid.
	 */
	override fun validate(
		value: Number,
		constraint: MultipleOfConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {

		val factor = ConstraintLiteralCache.getOrParse(constraint) {
			constraint.factor.toConstraintNumber()
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse multiple-of factor '${constraint.factor}' as a decimal number.",
			metadata = constraint,
		)

		// Non-positive step: skip rather than divide-by-zero / nonsense remainders.
		if (factor.compareTo(BigDecimal.ZERO) <= 0) return null

		val actual = value.toBigDecimalOrNull() ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Must be a finite decimal number.",
			metadata = constraint,
		)

		if (actual.remainder(factor).compareTo(BigDecimal.ZERO) == 0) return null

		return ConstraintError(
			code = ConstraintErrorCode.NUMBER_NOT_MULTIPLE,
			message = "Must be an exact multiple of ${constraint.factor}.",
			metadata = constraint,
		)
	}
}
