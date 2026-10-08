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
package io.ghaylan.validata.constraint.validator.temporal.relativetonow

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.RelativeToNow
import io.ghaylan.validata.constraint.annotation.RelativeToNowConstraint
import io.ghaylan.validata.ext.isAfter
import io.ghaylan.validata.ext.isBefore
import io.ghaylan.validata.ext.isEqual
import io.ghaylan.validata.ext.nowMatching
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.time.temporal.Temporal

/**
 * Places a [Temporal] relative to “now” per [RelativeToNowConstraint.relation].
 *
 * Uses [nowMatching] so the clock matches the subject’s concrete temporal
 * type. [RelativeToNowConstraint.within] / [RelativeToNowConstraint.unit] narrow the allowed
 * window, but only when the window is finite ([hasFiniteWindow]); an unsupported unit / type
 * combination yields [ConstraintErrorCode.VALUE_UNSUPPORTED].
 *
 * Null subjects are skipped by the engine (presence is `@Required`).
 * See [possibleErrorCodes] for the relation / window-dependent code set; failures carry
 * the failing constraint metadata.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object RelativeToNowValidator : ConstraintValidator<Temporal, RelativeToNowConstraint>() {

	/**
	 * Error codes this validator may emit for [RelativeToNow.relation] / [RelativeToNow.within].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata selecting the code set.
	 * @param type Runtime subject class (unused; the relation and window decide the codes).
	 * @return Temporal relation / window codes that can fire for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: RelativeToNowConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>()
		when (constraint.relation) {
			RelativeToNow.Relation.EQ -> codes += ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL
			RelativeToNow.Relation.GT,
			RelativeToNow.Relation.GTE -> {
				codes += ConstraintErrorCode.TEMPORAL_NOT_IN_FUTURE
				if (constraint.hasFiniteWindow()) {
					codes += ConstraintErrorCode.TEMPORAL_TOO_LATE
					codes += ConstraintErrorCode.VALUE_UNSUPPORTED
				}
			}
			RelativeToNow.Relation.LT,
			RelativeToNow.Relation.LTE -> {
				codes += ConstraintErrorCode.TEMPORAL_NOT_IN_PAST
				if (constraint.hasFiniteWindow()) {
					codes += ConstraintErrorCode.TEMPORAL_TOO_EARLY
					codes += ConstraintErrorCode.VALUE_UNSUPPORTED
				}
			}
		}
		return codes
	}

	/**
	 * Validates [value] against [constraint] relative to the context clock.
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access, clock).
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.
	 */
	override fun validate(
		value: Temporal,
		constraint: RelativeToNowConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val now = context.nowMatching(value)
		when (constraint.relation) {
			RelativeToNow.Relation.EQ -> {
				if (value.isEqual(now)) return null
				return fail(
					code = ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL,
					message = "Must be equal to the present moment.",
					metadata = constraint)
			}
			RelativeToNow.Relation.GT,
			RelativeToNow.Relation.GTE -> return validateAfterNow(value, now, constraint)
			RelativeToNow.Relation.LT,
			RelativeToNow.Relation.LTE -> return validateBeforeNow(value, now, constraint)
		}
	}

	/**
	 * Whether a finite `now + within` / `now - within` window applies.
	 *
	 * `within in 1 until Int.MAX_VALUE`: the default [Int.MAX_VALUE] means “unbounded” (adding it
	 * would overflow most temporal types), and a non-positive value means “no window”. Either way
	 * only the relation is enforced.
	 *
	 * Side effects: none.
	 *
	 * @receiver Metadata for this annotation instance.
	 * @return `true` when the window bound should be computed and checked.
	 */
	private fun RelativeToNowConstraint.hasFiniteWindow(): Boolean = within in 1 until Int.MAX_VALUE

	/**
	 * Future / future-or-present check, optionally capped by the finite window.
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under validation.
	 * @param now Clock reading matching [value]'s concrete type.
	 * @param constraint Metadata for this annotation instance.
	 * @return Path-free violation, or `null` when valid.
	 */
	private fun validateAfterNow(
		value: Temporal,
		now: Temporal,
		constraint: RelativeToNowConstraint,
	): ConstraintError<*>? {
		val allowPresent = constraint.relation == RelativeToNow.Relation.GTE
		val tooEarly = if (allowPresent) value.isBefore(now) else value.isBefore(now) || value.isEqual(now)
		if (tooEarly) {
			val window = upperBound(now, constraint)
			val message = buildString {
				append(if (allowPresent) "Must be in the future or the present." else "Must be in the future.")
				if (window != null) {
					append(" Allowed window: from $now through $window")
					append(" (within ${constraint.within} ${constraint.unit}).")
				}
			}
			return fail(
				code = ConstraintErrorCode.TEMPORAL_NOT_IN_FUTURE,
				message = message,
				metadata = constraint)
		}
		if (constraint.hasFiniteWindow()) {
			val maxTemporal = upperBound(now, constraint) ?: return unsupportedWindow(constraint)
			if (value.isAfter(maxTemporal)) {
				return fail(
					code = ConstraintErrorCode.TEMPORAL_TOO_LATE,
					message = "Must be no later than $maxTemporal (within ${constraint.within} ${constraint.unit} of now).",
					metadata = constraint)
			}
		}
		return null
	}

	/**
	 * Past / past-or-present check, optionally floored by the finite window.
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under validation.
	 * @param now Clock reading matching [value]'s concrete type.
	 * @param constraint Metadata for this annotation instance.
	 * @return Path-free violation, or `null` when valid.
	 */
	private fun validateBeforeNow(
		value: Temporal,
		now: Temporal,
		constraint: RelativeToNowConstraint,
	): ConstraintError<*>? {
		val allowPresent = constraint.relation == RelativeToNow.Relation.LTE
		val tooLate = if (allowPresent) value.isAfter(now) else value.isAfter(now) || value.isEqual(now)
		if (tooLate) {
			val window = lowerBound(now, constraint)
			val message = buildString {
				append(if (allowPresent) "Must be in the past or the present." else "Must be in the past.")
				if (window != null) {
					append(" Allowed window: from $window through $now")
					append(" (within ${constraint.within} ${constraint.unit}).")
				}
			}
			return fail(
				code = ConstraintErrorCode.TEMPORAL_NOT_IN_PAST,
				message = message,
				metadata = constraint,
			)
		}
		if (constraint.hasFiniteWindow()) {
			val minTemporal = lowerBound(now, constraint) ?: return unsupportedWindow(constraint)
			if (value.isBefore(minTemporal)) {
				return fail(
					code = ConstraintErrorCode.TEMPORAL_TOO_EARLY,
					message = "Must be no earlier than $minTemporal (within ${constraint.within} ${constraint.unit} of now).",
					metadata = constraint)
			}
		}
		return null
	}

	/**
	 * `now + within` in [RelativeToNowConstraint.unit], or `null` without a finite window or when
	 * the subject type cannot add that unit.
	 */
	private fun upperBound(now: Temporal, constraint: RelativeToNowConstraint): Temporal? {
		if (!constraint.hasFiniteWindow()) return null
		return runCatching {
			now.plus(constraint.within.toLong(), constraint.unit)
		}.getOrNull()
	}

	/**
	 * `now - within` in [RelativeToNowConstraint.unit], or `null` without a finite window or when
	 * the subject type cannot subtract that unit.
	 */
	private fun lowerBound(now: Temporal, constraint: RelativeToNowConstraint): Temporal? {
		if (!constraint.hasFiniteWindow()) return null
		return runCatching {
			now.minus(constraint.within.toLong(), constraint.unit)
		}.getOrNull()
	}

	/**
	 * Violation for a window whose unit the subject's temporal type cannot apply.
	 */
	private fun unsupportedWindow(constraint: RelativeToNowConstraint) = ConstraintError(
		code = ConstraintErrorCode.VALUE_UNSUPPORTED,
		message = "Cannot be checked: a window of ${constraint.within} ${constraint.unit} does not apply to this date/time type.",
		metadata = constraint,
	)

	/**
	 * Builds a relation / window violation; policy details live in [message], and the failing
	 * constraint metadata is attached.
	 */
	private fun fail(
		code: ConstraintErrorCode,
		message: String,
		metadata: RelativeToNowConstraint,
	) = ConstraintError(
		code = code,
		message = message,
		metadata = metadata,
	)
}
