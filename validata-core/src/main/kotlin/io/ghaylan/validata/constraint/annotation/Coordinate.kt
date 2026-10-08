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
package io.ghaylan.validata.constraint.annotation

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.number.coordinate.CoordinateValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import kotlin.reflect.KClass

/**
 * Asserts that a [Double] is a valid geographic coordinate for [axis].
 *
 * [Axis.LATITUDE] accepts `-90.0..90.0`; [Axis.LONGITUDE] accepts `-180.0..180.0`. `null` values
 * are skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Coordinate(axis = Coordinate.Axis.LATITUDE)
 * val lat: Double
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.NUMBER_OUT_OF_RANGE]
 */
@MustBeDocumented
@Constraint(validatedBy = [CoordinateValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Coordinate(
	val axis: Axis,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	enum class Axis {
		LATITUDE,
		LONGITUDE,
	}
}
