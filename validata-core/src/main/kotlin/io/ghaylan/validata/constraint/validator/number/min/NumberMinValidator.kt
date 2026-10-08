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
package io.ghaylan.validata.constraint.validator.number.min

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.constraint.validator.BoundCheck
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.constraint.validator.number.max.NumberMaxValidator
import io.ghaylan.validata.ext.integralLongOrNull
import io.ghaylan.validata.ext.toBigDecimalOrNull
import io.ghaylan.validata.ext.toConstraintNumber
import io.ghaylan.validata.ext.toIntegralLongOrNull
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.math.BigDecimal

/**
 * Floors a numeric value at a configured minimum (decimal string bound).
 *
 * The bound is memoized as [BigDecimal] per [MinConstraint] instance. When both the subject and
 * bound are integral, comparison uses [Long] (no per-call [BigDecimal] for the subject). Bound
 * failures go through [BoundCheck] with [ConstraintErrorCode.NUMBER_TOO_SMALL] — the failing
 * constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object NumberMinValidator : ConstraintValidator<Number, MinConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure for subject [type].
	 *
	 * [ConstraintErrorCode.VALUE_PARSING_FAILED] is omitted for strict integral leaves — they
	 * never take the subject NaN path, and KSP typed-literal checks make a bad bound unreachable
	 * for generated schemas (same policy as [NumberMaxValidator]).
	 */
	override fun possibleErrorCodes(
		constraint: MinConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>(ConstraintErrorCode.NUMBER_TOO_SMALL)
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
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.
	 */
	override fun validate(
		value: Number,
		constraint: MinConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val min = ConstraintLiteralCache.getOrParse(constraint) {
			constraint.value.toConstraintNumber()
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse minimum bound '${constraint.value}' as a decimal number.",
			metadata = constraint,
		)
		
		val actualLong = value.toIntegralLongOrNull()
		val minLong = min.integralLongOrNull()
		if (actualLong != null && minLong != null) {
			if (!BoundCheck.violatesMin(actualLong, minLong, constraint.inclusive)) return null
			return BoundCheck.minError(
				metadata = constraint,
				min = min,
				inclusive = constraint.inclusive,
				code = ConstraintErrorCode.NUMBER_TOO_SMALL)
		}
		
		val actual = value.toBigDecimalOrNull() ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Must be a finite decimal number.",
			metadata = constraint,
		)
		
		if (!BoundCheck.violatesMin(actual, min, constraint.inclusive)) return null
		
		return BoundCheck.minError(
			metadata = constraint,
			min = min,
			inclusive = constraint.inclusive,
			code = ConstraintErrorCode.NUMBER_TOO_SMALL)
	}
}
