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
package io.ghaylan.validata.constraint.validator

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode

/**
 * Inclusive/exclusive bound comparisons and failure payloads shared by numeric and temporal
 * `@Min` / `@Max` / `@Range` validators.
 *
 * Metadata is built **only** on the failure path so successful checks stay allocation-free.
 * Messages name the bound (policy); the failing [metadata] is attached for clients.
 *
 * @author Ghaylan Saada
 */
internal object BoundCheck {

	/**
	 * Whether [value] violates a minimum of [bound] under [inclusive].
	 *
	 * Side effects: none.
	 *
	 * @param T Comparable subject/bound type.
	 * @param value Subject under test.
	 * @param bound Minimum bound.
	 * @param inclusive When `true`, equality is allowed.
	 * @return `true` when the minimum is violated.
	 */
	fun <T: Comparable<T>> violatesMin(
		value: T,
		bound: T,
		inclusive: Boolean,
	): Boolean = if (inclusive) value < bound else value <= bound

	/**
	 * Whether [value] violates a maximum of [bound] under [inclusive].
	 *
	 * Side effects: none.
	 *
	 * @param T Comparable subject/bound type.
	 * @param value Subject under test.
	 * @param bound Maximum bound.
	 * @param inclusive When `true`, equality is allowed.
	 * @return `true` when the maximum is violated.
	 */
	fun <T: Comparable<T>> violatesMax(
		value: T,
		bound: T,
		inclusive: Boolean,
	): Boolean = if (inclusive) value > bound else value >= bound

	/**
	 * Builds the error for a failed minimum check.
	 *
	 * Side effects: none.
	 *
	 * @param T Bound type rendered into the message.
	 * @param metadata Constraint metadata that failed.
	 * @param min Parsed lower bound.
	 * @param inclusive Whether equality was allowed.
	 * @param code Usually [ConstraintErrorCode.NUMBER_TOO_SMALL] or [ConstraintErrorCode.TEMPORAL_TOO_EARLY].
	 * @return Violation carrying [metadata] and a precise rule message.
	 */
	fun <T> minError(
		metadata: Any,
		min: T,
		inclusive: Boolean,
		code: ConstraintErrorCode,
	): ConstraintError<*> = ConstraintError(
		code = code,
		message = minMessage(
			min = min,
			inclusive = inclusive,
			code = code,
		),
		metadata = metadata,
	)

	/**
	 * Builds the error for a failed maximum check.
	 *
	 * Side effects: none.
	 *
	 * @param T Bound type rendered into the message.
	 * @param metadata Constraint metadata that failed.
	 * @param max Parsed upper bound.
	 * @param inclusive Whether equality was allowed.
	 * @param code Usually [ConstraintErrorCode.NUMBER_TOO_LARGE] or [ConstraintErrorCode.TEMPORAL_TOO_LATE].
	 * @return Violation carrying [metadata] and a precise rule message.
	 */
	fun <T> maxError(
		metadata: Any,
		max: T,
		inclusive: Boolean,
		code: ConstraintErrorCode,
	): ConstraintError<*> = ConstraintError(
		code = code,
		message = maxMessage(
			max = max,
			inclusive = inclusive,
			code = code,
		),
		metadata = metadata,
	)

	/**
	 * Message for a failed minimum check, worded for the failing [code] family.
	 *
	 * Side effects: none.
	 *
	 * @param T Bound type rendered into the text.
	 * @param min Parsed lower bound.
	 * @param inclusive Whether equality was allowed.
	 * @param code Failure code selecting duration, temporal, or numeric wording.
	 * @return Rule sentence naming [min].
	 */
	private fun <T> minMessage(
		min: T,
		inclusive: Boolean,
		code: ConstraintErrorCode,
	): String = when (code) {
		ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT ->
			if (inclusive) "Duration must be at least $min." else "Duration must be longer than $min."
		ConstraintErrorCode.TEMPORAL_TOO_EARLY ->
			if (inclusive) "Must be on or after $min." else "Must be after $min."
		else ->
			if (inclusive) "Must be at least $min." else "Must be greater than $min."
	}

	/**
	 * Message for a failed maximum check, worded for the failing [code] family.
	 *
	 * Side effects: none.
	 *
	 * @param T Bound type rendered into the text.
	 * @param max Parsed upper bound.
	 * @param inclusive Whether equality was allowed.
	 * @param code Failure code selecting duration, temporal, or numeric wording.
	 * @return Rule sentence naming [max].
	 */
	private fun <T> maxMessage(
		max: T,
		inclusive: Boolean,
		code: ConstraintErrorCode,
	): String = when (code) {
		ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG ->
			if (inclusive) "Duration must be at most $max." else "Duration must be shorter than $max."
		ConstraintErrorCode.TEMPORAL_TOO_LATE ->
			if (inclusive) "Must be on or before $max." else "Must be before $max."
		else ->
			if (inclusive) "Must be at most $max." else "Must be less than $max."
	}

	/**
	 * Applies `@Range(negated = …)` semantics to a normal in-range check result.
	 *
	 * [inRangeError] is `null` when the value lies inside the configured interval (passed the
	 * non-negated check) and non-null when it lies outside. When [negated] is `true`, that sense
	 * is inverted and an inside value yields [outsideError].
	 *
	 * Side effects: none (unless [outsideError] allocates).
	 */
	fun applyNegation(
		negated: Boolean,
		inRangeError: ConstraintError<*>?,
		outsideError: () -> ConstraintError<*>,
	): ConstraintError<*>? {
		return if (negated) {
			if (inRangeError == null) outsideError() else null
		} else {
			inRangeError
		}
	}

	/**
	 * Shared "must not be in range" violation for negated `@Range`.
	 */
	fun negatedRangeError(
		metadata: Any,
		from: String,
		to: String,
		code: ConstraintErrorCode,
	): ConstraintError<*> = ConstraintError(
		code = code,
		message = "Must not be between $from and $to.",
		metadata = metadata,
	)
}
