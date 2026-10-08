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
package io.ghaylan.validata.constraint.validator.temporal.daysofweek

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.DaysOfWeekConstraint
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.DayOfWeek
import java.time.temporal.Temporal

/**
 * Restricts a temporal to allowed days-of-week (business calendars, delivery windows).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Temporals that do not
 * expose [DayOfWeek] via [DayOfWeek.from] skip silently rather than failing closed.
 *
 * Error: [ConstraintErrorCode.TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED] — the failing constraint is attached.
 * Messages list the declared allow-list (policy literals).
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object DaysOfWeekValidator : ConstraintValidator<Temporal, DaysOfWeekConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure for subject [type].
	 *
	 * Empty when [type] cannot expose a [DayOfWeek] (the validator skips those subjects).
	 */
	override fun possibleErrorCodes(
		constraint: DaysOfWeekConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		if (SubjectTypes.supportsDayOfWeek(type)) {
			setOf(ConstraintErrorCode.TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED)
		} else {
			emptySet()
		}
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`). Temporals that cannot
	 * resolve a [DayOfWeek] also skip.
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.	 
	 */
	override fun validate(
		value: Temporal,
		constraint: DaysOfWeekConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val dayOfWeek = runCatching { DayOfWeek.from(value) }.getOrNull()
			?: return null

		val member = constraint.days.contains(dayOfWeek)
		val ok = if (constraint.negated) !member else member
		if (ok) return null

		val list = constraint.days.joinToString(", ")
		return ConstraintError(
			code = ConstraintErrorCode.TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED,
			message = if (constraint.negated) {
				"Day of week must not be one of: $list."
			} else {
				"Day of week must be one of: $list."
			},
			metadata = constraint,
		)
	}
}
