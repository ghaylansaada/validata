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
package io.ghaylan.validata.constraint.validator.temporal.months

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MonthsConstraint
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.Month
import java.time.temporal.ChronoField
import java.time.temporal.Temporal

/**
 * Restricts a temporal to allowed calendar months.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Temporals without
 * [ChronoField.MONTH_OF_YEAR] skip silently.
 *
 * Error: [ConstraintErrorCode.TEMPORAL_MONTH_NOT_ALLOWED] — the failing constraint is attached.
 * Messages list the declared allow-list (policy literals).
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object MonthsValidator : ConstraintValidator<Temporal, MonthsConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure for subject [type].
	 *
	 * Empty when [type] cannot expose [ChronoField.MONTH_OF_YEAR] (the validator skips those subjects).
	 */
	override fun possibleErrorCodes(
		constraint: MonthsConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		if (SubjectTypes.supportsMonthOfYear(type)) {
			setOf(ConstraintErrorCode.TEMPORAL_MONTH_NOT_ALLOWED)
		} else {
			emptySet()
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
		value: Temporal,
		constraint: MonthsConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (!value.isSupported(ChronoField.MONTH_OF_YEAR)) return null
		val month = Month.of(value.get(ChronoField.MONTH_OF_YEAR))
		val member = constraint.months.contains(month)
		val ok = if (constraint.negated) !member else member
		if (ok) return null

		val list = constraint.months.joinToString(", ")
		return ConstraintError(
			code = ConstraintErrorCode.TEMPORAL_MONTH_NOT_ALLOWED,
			message = if (constraint.negated) {
				"Month must not be one of: $list."
			} else {
				"Month must be one of: $list."
			},
			metadata = constraint,
		)
	}
}
