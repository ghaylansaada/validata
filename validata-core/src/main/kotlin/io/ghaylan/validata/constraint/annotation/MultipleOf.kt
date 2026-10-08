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
import io.ghaylan.validata.constraint.validator.number.multiple.MultipleOfValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * Asserts that a [Number] is an exact multiple of [factor]: `value % factor == 0`, computed in
 * exact decimal arithmetic (via [BigDecimal]), not floating point.
 *
 * [factor] is a decimal string (e.g. `"5"`, `"0.25"`, `"1_000"`) validated at compile time / in
 * the IDE via [ConstraintArgKind.NOT_BLANK] and [ConstraintArgKind.TYPED_LITERAL]; underscores are
 * allowed as digit separators. A [factor] of `"0"` (or negative) disables the check at runtime —
 * every value passes. `null` values are skipped; combine with [Required] when the field must also
 * be present.
 *
 * ### Example
 *
 * ```kotlin
 * // Price must be a multiple of 5 cents
 * @field:MultipleOf("0.05")
 * val price: BigDecimal
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.NUMBER_NOT_MULTIPLE]
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 *
 * @property factor Divisor as a decimal string. `"0"` or negative disables the check.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@Repeatable
@MustBeDocumented
@Constraint(validatedBy = [MultipleOfValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class MultipleOf(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
	val factor: String,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)