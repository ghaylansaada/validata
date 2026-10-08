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
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.temporal.daysofmonth.DaysOfMonthValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoField
import java.time.temporal.Temporal
import kotlin.reflect.KClass

/**
 * Requires the calendar day-of-month of a date-aware temporal to be one of [days] (1–31),
 * or **not** one of [days] when [negated] is `true`.
 *
 * Applies to any [Temporal] that supports [ChronoField.DAY_OF_MONTH] (e.g. [LocalDate]).
 * Time-only temporals (e.g. [LocalTime]) skip. `null` values are skipped; combine with [Required]
 * when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:DaysOfMonth([1, 15])
 * val billingDate: LocalDate
 *
 * @field:DaysOfMonth(days = [13], negated = true)
 * val avoidUnlucky: LocalDate
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED]
 *
 * @property days Day-of-month values in `1..31` (non-empty). Allow-list when [negated] is `false`;
 *    deny-list when [negated] is `true`.
 * @property negated When `true`, the day must **not** be in [days]. Defaults to `false`.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [DaysOfMonthValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class DaysOfMonth(
	@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
	val days: IntArray,

	val negated: Boolean = false,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)