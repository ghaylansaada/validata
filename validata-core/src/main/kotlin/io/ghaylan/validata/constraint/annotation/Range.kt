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
import io.ghaylan.validata.constraint.validator.bound.duration.DurationRangeValidator
import io.ghaylan.validata.constraint.validator.bound.month.MonthRangeValidator
import io.ghaylan.validata.constraint.validator.bound.monthday.MonthDayRangeValidator
import io.ghaylan.validata.constraint.validator.bound.period.PeriodRangeValidator
import io.ghaylan.validata.constraint.validator.bound.yearmonth.YearMonthRangeValidator
import io.ghaylan.validata.constraint.validator.number.range.NumberRangeValidator
import io.ghaylan.validata.constraint.validator.temporal.range.TemporalRangeValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.time.*
import kotlin.reflect.KClass

/**
 * Asserts that a value lies within a configured lower and upper boundary:
 * `[from] <= value <= [to]` when both bounds are inclusive (the defaults). When [negated] is
 * `true`, the value must lie **outside** that interval instead.
 *
 * Use [fromInclusive] / [toInclusive] for strict `>` / `<` on either end.
 * The lower bound is checked first; a violation reports the same error as [Min], and an upper
 * bound violation reports the same error as [Max]. When [negated] and the value is inside the
 * range, reports [ConstraintErrorCode.NUMBER_OUT_OF_RANGE] or
 * [ConstraintErrorCode.TEMPORAL_OUT_OF_RANGE] for the subject family.
 *
 * The subject's runtime type selects the matching validator and literal syntax for [from] and [to]:
 *
 * | Subject type | [from] / [to] literal syntax |
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
 * @field:Range(from = "18", to = "120")
 * val age: Int
 *
 * @field:Range(from = "2020-01-01", to = "2030-12-31")
 * val effectiveDate: LocalDate
 *
 * @field:Range(from = "PT30M", to = "PT8H", fromInclusive = false)
 * val sessionTimeout: Duration
 *
 * @field:Range(from = "0", to = "17", negated = true)
 * val adultAge: Int
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.NUMBER_TOO_SMALL] / [ConstraintErrorCode.NUMBER_TOO_LARGE]
 * - [ConstraintErrorCode.TEMPORAL_TOO_EARLY] / [ConstraintErrorCode.TEMPORAL_TOO_LATE]
 * - [ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT] / [ConstraintErrorCode.TEMPORAL_DURATION_TOO_LONG]
 * - [ConstraintErrorCode.NUMBER_OUT_OF_RANGE] / [ConstraintErrorCode.TEMPORAL_OUT_OF_RANGE] (when [negated] and inside)
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 *
 * @property from Lower bound as a string literal validated at compile time / in the IDE.
 * @property to Upper bound as a string literal validated at compile time / in the IDE.
 * @property fromInclusive When `true`, `>= [from]`; when `false`, strict `>`. Defaults to `true`.
 * @property toInclusive When `true`, `<= [to]`; when `false`, strict `<`. Defaults to `true`.
 * @property negated When `true`, the value must be **outside** `[from, to]`. Defaults to `false`.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 */
@MustBeDocumented
@Constraint(validatedBy = [
	NumberRangeValidator::class,
	TemporalRangeValidator::class,
	DurationRangeValidator::class,
	PeriodRangeValidator::class,
	YearMonthRangeValidator::class,
	MonthDayRangeValidator::class,
	MonthRangeValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.TYPE,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Range(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
	val from: String,
	
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
	val to: String,
	
	val fromInclusive: Boolean = true,
	
	val toInclusive: Boolean = true,

	val negated: Boolean = false,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
)
