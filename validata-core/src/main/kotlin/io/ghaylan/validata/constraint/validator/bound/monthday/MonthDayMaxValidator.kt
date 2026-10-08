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
package io.ghaylan.validata.constraint.validator.bound.monthday

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MaxConstraint
import io.ghaylan.validata.constraint.validator.BoundCheck
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.MonthDay

/**
 * Caps a [MonthDay] at a configured maximum (ISO-8601 `"--MM-dd"` literal).
 *
 * Bound parse is memoized per [MaxConstraint] instance. Bound failures go through [BoundCheck]
 * with [ConstraintErrorCode.TEMPORAL_TOO_LATE] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object MonthDayMaxValidator : ConstraintValidator<MonthDay, MaxConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: MaxConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_PARSING_FAILED,
		ConstraintErrorCode.TEMPORAL_TOO_LATE,
	)
	
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
		value: MonthDay,
		constraint: MaxConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val max = ConstraintLiteralCache.getOrParse(constraint) {
			runCatching { MonthDay.parse(constraint.value.trim()) }.getOrNull()
		} ?: return ConstraintError(
			code = ConstraintErrorCode.VALUE_PARSING_FAILED,
			message = "Cannot parse MonthDay maximum bound '${constraint.value}' as --MM-dd.",
			metadata = constraint,
		)
		
		if (!BoundCheck.violatesMax(value, max, constraint.inclusive)) return null
		
		return BoundCheck.maxError(
			metadata = constraint,
			max = max,
			inclusive = constraint.inclusive,
			code = ConstraintErrorCode.TEMPORAL_TOO_LATE,
		)
	}
}
