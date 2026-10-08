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
import io.ghaylan.validata.constraint.validator.comparison.membership.NotInValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import kotlin.reflect.KClass

/**
 * Restricts a scalar subject out of the literal deny-list [values].
 *
 * Membership compares the subject's `toString()` against each entry, so the set works for
 * strings, numbers, enums, and `UUID`s alike. Non-scalar subjects (collections, maps, beans) are
 * skipped at runtime — membership is only meaningful for a single value. Each [values] entry is
 * compile-time / IDE-checked against the subject type via [ConstraintArgKind.TYPED_LITERAL].
 *
 * `null` values are skipped; combine with [Required] when the field must also be present.
 * For an allow-list, use [In].
 *
 * ### Example
 *
 * ```kotlin
 * @field:NotIn(values = ["root", "admin"])
 * val username: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_NOT_ALLOWED]
 *
 * @property values Literal options the subject must not match (non-empty).
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [NotInValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class NotIn(
	@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
	val values: Array<String>,

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
)
