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
import io.ghaylan.validata.constraint.validator.temporal.relativetonow.RelativeToNowValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.reflect.KClass

/**
 * Places a [Temporal] relative to “now”, optionally capped by a finite [within] window.
 *
 * “Now” is read from the validation clock and materialized as the subject’s own concrete temporal
 * type, so a `LocalDate` is compared against today and an `Instant` against this instant.
 * [relation] picks the ordering; [within] plus [unit] optionally bound how far from now the value
 * may sit.
 *
 * The window is only applied when [within] is **positive and not** [Int.MAX_VALUE] — that is,
 * `within in 1 until Int.MAX_VALUE`. The default [Int.MAX_VALUE] therefore means “unbounded”:
 * only the [relation] is enforced, and no `now ± within` arithmetic is attempted (which would
 * overflow most temporal types anyway).
 *
 * `null` values are skipped; combine with [Required] when the field must also be present.
 *
 * ### Examples
 *
 * ```kotlin
 * // Any date in the past
 * @field:RelativeToNow(relation = RelativeToNow.Relation.LT)
 * val bornOn: LocalDate
 *
 * // Today or later
 * @field:RelativeToNow(relation = RelativeToNow.Relation.GTE)
 * val startsOn: LocalDate
 *
 * // In the future, but no more than 90 days out
 * @field:RelativeToNow(relation = RelativeToNow.Relation.GT, within = 90, unit = ChronoUnit.DAYS)
 * val appointmentOn: LocalDate
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.TEMPORAL_NOT_IN_FUTURE]
 * - [ConstraintErrorCode.TEMPORAL_NOT_IN_PAST]
 * - [ConstraintErrorCode.TEMPORAL_TOO_EARLY]
 * - [ConstraintErrorCode.TEMPORAL_TOO_LATE]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL] (`EQ`)
 * - [ConstraintErrorCode.VALUE_UNSUPPORTED] (window unit incompatible with the subject type)
 *
 * @property relation Required ordering of the annotated value against “now”.
 * @property within Size of the allowed window measured in [unit]. Defaults to [Int.MAX_VALUE],
 *    which (like any non-positive value) means no finite window — [relation] alone is enforced.
 * @property unit Unit paired with [within]. Defaults to [ChronoUnit.DAYS]. A unit the subject’s
 *    temporal type cannot add or subtract yields [ConstraintErrorCode.VALUE_UNSUPPORTED].
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [RelativeToNowValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class RelativeToNow(
	val relation: Relation,
	
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val within: Int = Int.MAX_VALUE,
	
	val unit: ChronoUnit = ChronoUnit.DAYS,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	
	/**
	 * How the annotated value must order against “now”.
	 *
	 * Names mirror the comparison vocabulary used by `@Compare` and `@RequiredWhen`: the
	 * annotated value is the left operand and “now” is the right one.
	 */
	enum class Relation {
		
		/**
		 * Must equal the present moment, at the subject type’s own resolution.
		 */
		EQ,
		
		/**
		 * Must be strictly in the future.
		 */
		GT,
		
		/**
		 * Must be strictly in the past.
		 */
		LT,
		
		/**
		 * Must be in the future or exactly now.
		 */
		GTE,
		
		/**
		 * Must be in the past or exactly now.
		 */
		LTE,
	}
}
