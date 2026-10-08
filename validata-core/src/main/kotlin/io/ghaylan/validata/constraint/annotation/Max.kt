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
import io.ghaylan.validata.constraint.validator.bound.duration.DurationMaxValidator
import io.ghaylan.validata.constraint.validator.bound.month.MonthMaxValidator
import io.ghaylan.validata.constraint.validator.bound.monthday.MonthDayMaxValidator
import io.ghaylan.validata.constraint.validator.bound.period.PeriodMaxValidator
import io.ghaylan.validata.constraint.validator.bound.yearmonth.YearMonthMaxValidator
import io.ghaylan.validata.constraint.validator.number.max.NumberMaxValidator
import io.ghaylan.validata.constraint.validator.temporal.max.TemporalMaxValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.time.*
import kotlin.reflect.KClass

/**
 * Asserts that a value is at most a specified maximum boundary: `value <= [value]` (or `<` when
 * [inclusive] is `false`).
 *
 * The subject's runtime type selects the matching validator and literal syntax for [value]:
 *
 * | Subject type | [value] literal syntax |
 * |---|---|
 * | [Number] (`Int`, `Long`, `Double`, `BigDecimal`, …) | decimal string, e.g. `"100"`, `"1_000_000"` |
 * | ISO temporals ([LocalDate], [Instant], …), [Year] | type-specific ISO-8601 literal |
 * | [Duration] | ISO-8601 duration, e.g. `"PT1H30M"` |
 * | [Period] | ISO-8601 period, e.g. `"P2Y"` |
 * | [YearMonth] | `"uuuu-MM"` |
 * | [MonthDay] | `"--MM-dd"` |
 * | [Month] | `"DECEMBER"` or `"1"`…`"12"` |
 *
 * Any other subject type is rejected at compile time / in the IDE — there is no catch-all `Any`
 * validator. `null` values are skipped; combine with [Required] when the field must also be
 * present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Max("120")
 * val age: Int
 *
 * @field:Max("P2Y")
 * val subscriptionLength: Period
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.NUMBER_TOO_LARGE]
 * - [ConstraintErrorCode.TEMPORAL_TOO_LATE]
 * - [ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG]
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 *
 * @property value Maximum bound as a string literal validated at compile time / in the IDE.
 * @property inclusive When `true`, `<=`; when `false`, strict `<`. Defaults to `true`.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [
	NumberMaxValidator::class,
	TemporalMaxValidator::class,
	DurationMaxValidator::class,
	PeriodMaxValidator::class,
	YearMonthMaxValidator::class,
	MonthDayMaxValidator::class,
	MonthMaxValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.TYPE,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Max(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
	val value: String,
	
	val inclusive: Boolean = true,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)
