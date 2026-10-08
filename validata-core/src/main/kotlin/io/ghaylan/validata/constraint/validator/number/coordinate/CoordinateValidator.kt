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
package io.ghaylan.validata.constraint.validator.number.coordinate

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Coordinate
import io.ghaylan.validata.constraint.annotation.CoordinateConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Requires a [Double] to lie on a geographic axis range for [CoordinateConstraint.axis].
 *
 * Latitude must be in `[-90, 90]`; longitude in `[-180, 180]` (inclusive).
 * Null subjects are skipped by the engine (presence is `@Required`).
 *
 * Error: [ConstraintErrorCode.NUMBER_OUT_OF_RANGE] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object CoordinateValidator : ConstraintValidator<Double, CoordinateConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata for this annotation instance (unused; codes are fixed).
	 * @return Always [ConstraintErrorCode.NUMBER_OUT_OF_RANGE].
	 */
	override fun possibleErrorCodes(
		constraint: CoordinateConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.NUMBER_OUT_OF_RANGE)

	/**
	 * Validates [value] against the axis range in [constraint].
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
		value: Double,
		constraint: CoordinateConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val (min, max, label) = when (constraint.axis) {
			Coordinate.Axis.LATITUDE -> Triple(-90.0, 90.0, "latitude")
			Coordinate.Axis.LONGITUDE -> Triple(-180.0, 180.0, "longitude")
		}
		if (value in min..max) return null
		return ConstraintError(
			code = ConstraintErrorCode.NUMBER_OUT_OF_RANGE,
			message = "Must be a $label between $min and $max degrees.",
			metadata = constraint,
		)
	}
}
