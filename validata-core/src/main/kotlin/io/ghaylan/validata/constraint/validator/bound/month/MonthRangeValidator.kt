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
package io.ghaylan.validata.constraint.validator.bound.month

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.RangeConstraint
import io.ghaylan.validata.ext.toConstraintMonth
import io.ghaylan.validata.constraint.validator.BoundCheck
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.Month

/**
 * Constrains a [Month] to a configured inclusive or exclusive range (enum name or 1–12 literals).
 *
 * Bound parse is memoized per [RangeConstraint] instance. Bound failures go through [BoundCheck]
 * — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object MonthRangeValidator : ConstraintValidator<Month, RangeConstraint>() {

	/**
	 * Memoized range endpoints as [Month].
	 *
	 * @property from Parsed lower bound.
	 * @property to Parsed upper bound.
	 */
	private data class Parsed(
		val from: Month,
		val to: Month,
	)

	/**
	 * Error codes this validator may emit for [constraint].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Range metadata; when [RangeConstraint.negated] only
	 *   [ConstraintErrorCode.TEMPORAL_OUT_OF_RANGE].
	 * @return Parse plus bound or negated codes for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: RangeConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>(ConstraintErrorCode.VALUE_PARSING_FAILED)
		if (constraint.negated) {
			codes += ConstraintErrorCode.TEMPORAL_OUT_OF_RANGE
		} else {
			codes += ConstraintErrorCode.TEMPORAL_TOO_EARLY
			codes += ConstraintErrorCode.TEMPORAL_TOO_LATE
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
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.
	 */
	override fun validate(
		value: Month,
		constraint: RangeConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val bounds = ConstraintLiteralCache.getOrParse(constraint) {
			val from = constraint.from.toConstraintMonth() ?: return@getOrParse null
			val to = constraint.to.toConstraintMonth() ?: return@getOrParse null
			Parsed(from, to)
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse Month range bounds '${constraint.from}' / '${constraint.to}' (enum name or 1–12).",
			metadata = constraint,
		)

		val inRangeError: ConstraintError<*>? = when {
			BoundCheck.violatesMin(value, bounds.from, constraint.fromInclusive) -> BoundCheck.minError(
				metadata = constraint,
				min = bounds.from,
				inclusive = constraint.fromInclusive,
				code = ConstraintErrorCode.TEMPORAL_TOO_EARLY,
			)
			BoundCheck.violatesMax(value, bounds.to, constraint.toInclusive) -> BoundCheck.maxError(
				metadata = constraint,
				max = bounds.to,
				inclusive = constraint.toInclusive,
				code = ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
			else -> null
		}
		return BoundCheck.applyNegation(constraint.negated, inRangeError) {
			BoundCheck.negatedRangeError(
				metadata = constraint,
				from = constraint.from,
				to = constraint.to,
				code = ConstraintErrorCode.TEMPORAL_OUT_OF_RANGE,
			)
		}
	}
}
