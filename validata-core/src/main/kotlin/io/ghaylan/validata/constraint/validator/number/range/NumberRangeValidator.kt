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
package io.ghaylan.validata.constraint.validator.number.range

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.constraint.validator.BoundCheck
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
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
 * Constrains a numeric value to a configured inclusive or exclusive range (decimal string bounds).
 *
 * Parsed bounds are memoized per [RangeConstraint] instance. When the subject and both bounds
 * are integral, comparison uses [Long] (no per-call [BigDecimal] for the subject). Bound failures
 * go through [BoundCheck] with [ConstraintErrorCode.NUMBER_TOO_SMALL] /
 * [ConstraintErrorCode.NUMBER_TOO_LARGE] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object NumberRangeValidator : ConstraintValidator<Number, RangeConstraint>() {
	
	/**
	 * Memoized range endpoints as [BigDecimal], with optional integral long forms for the
	 * success-path fast compare.
	 *
	 * @property from Parsed lower bound.
	 * @property to Parsed upper bound.
	 * @property fromLong [from] as [Long] when integral; otherwise `null`.
	 * @property toLong [to] as [Long] when integral; otherwise `null`.
	 */
	private data class Parsed(
		val from: BigDecimal,
		val to: BigDecimal,
		val fromLong: Long?,
		val toLong: Long?)
	
	/**
	 * Error codes this validator may emit for [constraint] and subject [type].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Range metadata; when [RangeConstraint.negated] only the out-of-range code applies.
	 * @param type Runtime subject class — strict integral leaves omit parse failures.
	 * @return Parsing plus bound or negated codes for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: RangeConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>()
		if (!SubjectTypes.isStrictIntegralNumber(type)) {
			codes += ConstraintErrorCode.VALUE_PARSING_FAILED
		}
		if (constraint.negated) {
			codes += ConstraintErrorCode.NUMBER_OUT_OF_RANGE
		} else {
			codes += ConstraintErrorCode.NUMBER_TOO_SMALL
			codes += ConstraintErrorCode.NUMBER_TOO_LARGE
		}
		return codes
	}
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: may memoize parsed literals in [ConstraintLiteralCache] on miss.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint, or `null`
	 *   when valid.
	 */
	override fun validate(
		value: Number,
		constraint: RangeConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val bounds = ConstraintLiteralCache.getOrParse(constraint) {
			val from = constraint.from.toConstraintNumber() ?: return@getOrParse null
			val to = constraint.to.toConstraintNumber() ?: return@getOrParse null
			Parsed(
				from = from,
				to = to,
				fromLong = from.integralLongOrNull(),
				toLong = to.integralLongOrNull())
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse range bounds '${constraint.from}' / '${constraint.to}' as decimal numbers.",
			metadata = constraint,
		)
		
		val actualLong = value.toIntegralLongOrNull()
		val fromLong = bounds.fromLong
		val toLong = bounds.toLong
		val inRangeError: ConstraintError<*>? = if (actualLong != null && fromLong != null && toLong != null) {
			when {
				BoundCheck.violatesMin(actualLong, fromLong, constraint.fromInclusive) ->
					BoundCheck.minError(
						metadata = constraint,
						min = bounds.from,
						inclusive = constraint.fromInclusive,
						code = ConstraintErrorCode.NUMBER_TOO_SMALL)
				BoundCheck.violatesMax(actualLong, toLong, constraint.toInclusive) ->
					BoundCheck.maxError(
						metadata = constraint,
						max = bounds.to,
						inclusive = constraint.toInclusive,
						code = ConstraintErrorCode.NUMBER_TOO_LARGE)
				else -> null
			}
		} else {
			val actual = value.toBigDecimalOrNull() ?: return ConstraintError(
				code = ConstraintErrorCode.VALUE_PARSING_FAILED,
				message = "Must be a finite decimal number.",
				metadata = constraint,
			)
			when {
				BoundCheck.violatesMin(actual, bounds.from, constraint.fromInclusive) ->
					BoundCheck.minError(
						metadata = constraint,
						min = bounds.from,
						inclusive = constraint.fromInclusive,
						code = ConstraintErrorCode.NUMBER_TOO_SMALL)
				BoundCheck.violatesMax(actual, bounds.to, constraint.toInclusive) ->
					BoundCheck.maxError(
						metadata = constraint,
						max = bounds.to,
						inclusive = constraint.toInclusive,
						code = ConstraintErrorCode.NUMBER_TOO_LARGE)
				else -> null
			}
		}
		return BoundCheck.applyNegation(constraint.negated, inRangeError) {
			BoundCheck.negatedRangeError(
				metadata = constraint,
				from = constraint.from,
				to = constraint.to,
				code = ConstraintErrorCode.NUMBER_OUT_OF_RANGE,
			)
		}
	}
}
