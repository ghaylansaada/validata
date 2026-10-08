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
import io.ghaylan.validata.constraint.validator.bound.duration.DurationMinValidator
import io.ghaylan.validata.constraint.validator.bound.month.MonthMinValidator
import io.ghaylan.validata.constraint.validator.bound.monthday.MonthDayMinValidator
import io.ghaylan.validata.constraint.validator.bound.period.PeriodMinValidator
import io.ghaylan.validata.constraint.validator.bound.yearmonth.YearMonthMinValidator
import io.ghaylan.validata.constraint.validator.number.min.NumberMinValidator
import io.ghaylan.validata.constraint.validator.temporal.min.TemporalMinValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.time.*
import kotlin.reflect.KClass

/**
 * Asserts that a value is at least a specified minimum boundary: `value >= [value]` (or `>` when
 * [inclusive] is `false`).
 *
 * The subject's runtime type selects the matching validator and literal syntax for [value]:
 *
 * | Subject type | [value] literal syntax |
 * |---|---|
 * | [Number] (`Int`, `Long`, `Double`, `BigDecimal`, …) | decimal string, e.g. `"18"`, `"1_000"` |
 * | ISO temporals ([LocalDate], [Instant], …), [Year] | type-specific ISO-8601 literal |
 * | [Duration] | ISO-8601 duration, e.g. `"PT1H30M"` |
 * | [Period] | ISO-8601 period, e.g. `"P1Y2M"` |
 * | [YearMonth] | `"uuuu-MM"` |
 * | [MonthDay] | `"--MM-dd"` |
 * | [Month] | `"JANUARY"` or `"1"`…`"12"` |
 *
 * Any other subject type is rejected at compile time / in the IDE — there is no catch-all `Any`
 * validator. `null` values are skipped; combine with [Required] when the field must also be
 * present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Min("18")
 * val age: Int
 *
 * @field:Min("PT30M", inclusive = false)
 * val sessionTimeout: Duration
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.NUMBER_TOO_SMALL]
 * - [ConstraintErrorCode.TEMPORAL_TOO_EARLY]
 * - [ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT]
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 *
 * @property value Minimum bound as a string literal validated at compile time / in the IDE.
 * @property inclusive When `true`, `>=`; when `false`, strict `>`. Defaults to `true`.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [
	NumberMinValidator::class,
	TemporalMinValidator::class,
	DurationMinValidator::class,
	PeriodMinValidator::class,
	YearMonthMinValidator::class,
	MonthDayMinValidator::class,
	MonthMinValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.TYPE,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Min(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
	val value: String,
	
	val inclusive: Boolean = true,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)
