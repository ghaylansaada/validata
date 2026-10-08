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
import io.ghaylan.validata.constraint.validator.number.numbersign.NumberSignValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * Requires a [Number] to be strictly positive or strictly negative, per [sign].
 *
 * Use for quantities that must stay above or below zero — balances, deltas, rates, inventory
 * adjustments. Set [allowZero] to `true` when zero is also acceptable (Jakarta Bean Validation’s
 * `@PositiveOrZero` / `@NegativeOrZero` equivalents).
 *
 * Sign is decided with exact decimal comparison: integral subjects use a long fast path; other
 * numerics convert via `toString()` to [BigDecimal]. Non-finite floating values
 * (`NaN`, ±∞) fail as unparseable. `null` is skipped; combine with [Required] when the field
 * must also be present.
 *
 * ### Examples
 *
 * ```kotlin
 * // 1, 0.01 pass; 0 and -1 fail
 * @field:NumberSign(sign = NumberSign.Sign.POSITIVE)
 * val unitPrice: BigDecimal
 *
 * // 0, 1, 2.5 pass; -0.01 fails
 * @field:NumberSign(sign = NumberSign.Sign.POSITIVE, allowZero = true)
 * val quantity: Int
 *
 * // -1, -0.5 pass; 0 and 1 fail
 * @field:NumberSign(sign = NumberSign.Sign.NEGATIVE)
 * val loss: Double
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 * - [ConstraintErrorCode.NUMBER_NOT_POSITIVE]
 * - [ConstraintErrorCode.NUMBER_NOT_NEGATIVE]
 * - [ConstraintErrorCode.NUMBER_ZERO_NOT_ALLOWED]
 *
 * @property sign Required side of zero for the annotated number.
 * @property allowZero When `true`, zero also passes. Defaults to `false` (strict sign).
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [NumberSignValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class NumberSign(
	val sign: Sign,
	
	val allowZero: Boolean = false,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	
	/**
	 * Required side of zero for the annotated number.
	 */
	enum class Sign {
		
		/**
		 * Value must be greater than zero, or ≥ 0 when [NumberSign.allowZero] is `true`.
		 */
		POSITIVE,
		
		/**
		 * Value must be less than zero, or ≤ 0 when [NumberSign.allowZero] is `true`.
		 */
		NEGATIVE,
	}
}
