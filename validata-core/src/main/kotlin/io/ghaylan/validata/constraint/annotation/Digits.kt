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
import io.ghaylan.validata.constraint.validator.number.digits.DigitsValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * Asserts that a [Number] does not exceed configured integer and fractional digit counts.
 *
 * Digit counts follow Bean Validation semantics on the [BigDecimal] representation of
 * the value (sign ignored): integer digits = `max(0, precision - scale)`, fraction digits =
 * `max(0, scale)`. Trailing zeros in the fractional part count toward the scale as written
 * (e.g. `"10.50"` has two fraction digits).
 *
 * `null` values are skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Digits(integer = 3, fraction = 2)
 * val amount: BigDecimal
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.NUMBER_PRECISION_EXCEEDED]
 * - [ConstraintErrorCode.NUMBER_SCALE_EXCEEDED]
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 *
 * @property integer Maximum number of digits in the integer part (inclusive bound).
 * @property fraction Maximum number of digits in the fractional part (inclusive bound).
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [DigitsValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Digits(
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val integer: Int,
	
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val fraction: Int,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)
