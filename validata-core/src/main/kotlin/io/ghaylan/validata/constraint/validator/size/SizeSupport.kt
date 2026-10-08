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
package io.ghaylan.validata.constraint.validator.size

import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition

/**
 * Shared `@Size` measurement and bound checks for the split size validators.
 *
 * Error-code selection stays aligned with the historical monolithic `SizeValidator`
 * (`TEXT_*` for strings, `COLLECTION_*` for lists/arrays, `OBJECT_*` for maps).
 *
 * Metadata is built **only** on the failure path — successful size checks must not allocate.
 *
 * Messages state the declared bound and the unit implied by the error code; the observed size is
 * not echoed (avoids leaking text-length hints). The failing [SizeConstraint] is attached instead.
 *
 * @author Ghaylan Saada
 */
internal object SizeSupport {

	/**
	 * Validates [size] against [constraint] bounds.
	 *
	 * Side effects: none.
	 *
	 * @param size Observed size to check.
	 * @param constraint Size bounds from the annotation.
	 * @param underMinimumCode Code when [size] is below [SizeConstraint.min].
	 * @param exceedsMaximumCode Code when [size] is above [SizeConstraint.max].
	 * @return Violation carrying [constraint], or `null` when within bounds.
	 */
	fun validateBounds(
		size: Int,
		constraint: SizeConstraint,
		underMinimumCode: ConstraintErrorCode,
		exceedsMaximumCode: ConstraintErrorCode,
	): ConstraintError<*>? {
		if (size < constraint.min) {
			return ConstraintError(
				code = underMinimumCode,
				message = "Must contain at least ${constraint.min} ${unitFor(underMinimumCode, constraint.min)}.",
				metadata = constraint,
			)
		}
		if (size > constraint.max) {
			return ConstraintError(
				code = exceedsMaximumCode,
				message = "Must contain at most ${constraint.max} ${unitFor(exceedsMaximumCode, constraint.max)}.",
				metadata = constraint,
			)
		}
		return null
	}

	/**
	 * Error codes a size validator may emit for [constraint] bounds.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Size bounds (`min` default `0` / `max` default [Int.MAX_VALUE] = unbounded).
	 * @param underMinimumCode Code when below min.
	 * @param exceedsMaximumCode Code when above max.
	 * @return Only the bound codes that can fire for this configuration.
	 */
	fun possibleErrorCodes(
		constraint: SizeConstraint,
		underMinimumCode: ConstraintErrorCode,
		exceedsMaximumCode: ConstraintErrorCode,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>()
		if (constraint.min > 0) codes += underMinimumCode
		if (constraint.max < Int.MAX_VALUE) codes += exceedsMaximumCode
		return codes
	}

	/**
	 * Picks the unit noun the message should use, pluralized for [count].
	 *
	 * Side effects: none.
	 *
	 * @param code Error code whose family identifies the measured subject.
	 * @param count Bound the noun agrees with; `1` selects the singular form.
	 * @return Unit noun such as `"characters"`, `"items"`, or `"entries"`.
	 */
	private fun unitFor(
		code: ConstraintErrorCode,
		count: Int,
	): String = when (code) {
		ConstraintErrorCode.TEXT_TOO_SHORT, ConstraintErrorCode.TEXT_TOO_LONG ->
			if (count == 1) "character" else "characters"
		ConstraintErrorCode.OBJECT_TOO_SMALL, ConstraintErrorCode.OBJECT_TOO_LARGE ->
			if (count == 1) "entry" else "entries"
		else ->
			if (count == 1) "item" else "items"
	}

	/**
	 * Element count for JVM arrays (`Array<*>` and primitive arrays), or `null` when [value]
	 * is not an array shape.
	 *
	 * Side effects: none.
	 *
	 * @param value Candidate array value.
	 * @return Array length, or `null` when not an array.
	 */
	fun arrayLength(value: Any): Int? = when (value) {
		is Array<*> -> value.size
		is ByteArray -> value.size
		is ShortArray -> value.size
		is IntArray -> value.size
		is LongArray -> value.size
		is FloatArray -> value.size
		is DoubleArray -> value.size
		is BooleanArray -> value.size
		is CharArray -> value.size
		else -> null
	}
}
