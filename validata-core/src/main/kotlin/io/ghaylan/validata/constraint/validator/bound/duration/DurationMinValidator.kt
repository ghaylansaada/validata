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
package io.ghaylan.validata.constraint.validator.bound.duration

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.constraint.validator.BoundCheck
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.Duration

/**
 * Floors a [Duration] at a configured minimum (ISO-8601 duration literal, e.g. `"PT1H"`).
 *
 * Bound parse is memoized per [MinConstraint] instance. Bound failures go through [BoundCheck]
 * with [ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object DurationMinValidator : ConstraintValidator<Duration, MinConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure for subject [type].
	 */
	override fun possibleErrorCodes(
		constraint: MinConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		buildSet {
			add(ConstraintErrorCode.VALUE_PARSING_FAILED)
			if (SubjectTypes.isDurationCompatible(type)) {
				add(ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT)
			}
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
		value: Duration,
		constraint: MinConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		
		val min = ConstraintLiteralCache.getOrParse(constraint) {
			runCatching {
				Duration.parse(constraint.value.trim())
			}.getOrNull()
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse Duration minimum bound '${constraint.value}' as ISO-8601.",
			metadata = constraint)
		
		if (!BoundCheck.violatesMin(value, min, constraint.inclusive)) return null
		
		return BoundCheck.minError(
			metadata = constraint,
			min = min,
			inclusive = constraint.inclusive,
			code = ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
		)
	}
}
