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
package io.ghaylan.validata.constraint.validator.temporal.min

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.ext.isAfter
import io.ghaylan.validata.ext.isEqual
import io.ghaylan.validata.ext.toTemporal
import io.ghaylan.validata.constraint.validator.BoundCheck
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.temporal.Temporal

/**
 * Floors a temporal value at a configured minimum (inclusive/exclusive per metadata).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). The bound is parsed into
 * the concrete [Temporal] type of the annotated field and memoized per [MinConstraint] instance
 * — each schema site binds one field type for the life of the JVM.
 *
 * Bound-literal parse failures use [ConstraintErrorCode.VALUE_PARSING_FAILED]; bound violations
 * use [ConstraintErrorCode.TEMPORAL_TOO_EARLY] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object TemporalMinValidator : ConstraintValidator<Temporal, MinConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: MinConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_PARSING_FAILED,
		ConstraintErrorCode.TEMPORAL_TOO_EARLY,
	)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: may parse and cache the bound for [constraint] via [ConstraintLiteralCache].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.
	 */
	override fun validate(
		value: Temporal,
		constraint: MinConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		// Cache by constraint: one annotation site ⇒ one field type ⇒ one parsed bound.
		val minValue = ConstraintLiteralCache.getOrParse(constraint) {
			runCatching { constraint.value.toTemporal(value::class) }.getOrNull()
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse temporal minimum bound '${constraint.value}'.",
			metadata = constraint,
		)

		val isValid = if (constraint.inclusive) {
			value.isAfter(minValue) || value.isEqual(minValue)
		} else {
			value.isAfter(minValue)
		}
		if (isValid) return null

		return BoundCheck.minError(
			metadata = constraint,
			min = minValue,
			inclusive = constraint.inclusive,
			code = ConstraintErrorCode.TEMPORAL_TOO_EARLY,
		)
	}
}
