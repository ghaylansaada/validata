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
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import kotlin.reflect.KClass

/**
 * Unconditional presence constraint with **type-aware** defaults.
 *
 * Use this for everyday “field must be present” rules. Prefer [RequiredWhen] when presence
 * depends on a sibling field.
 *
 * ### Default ([Mode.STRICT])
 *
 * Fails when the value is `null` or every nested collection/map/array/string is empty
 * (deep emptiness — useful for nested DTOs that look “filled” but contain only blanks).
 *
 * Override with [mode] when you need a shallower rule (e.g. [Mode.NULL] for null-only checks
 * on scalars, or [Mode.EMPTY] for shallow containers / empty text).
 *
 * ### Example
 *
 * ```kotlin
 * @field:Required
 * val email: String?
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_MISSING]
 * - [ConstraintErrorCode.TEXT_BLANK]
 * - [ConstraintErrorCode.VALUE_EMPTY]
 *
 * @property mode Presence strictness. Defaults to [Mode.STRICT].
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@Repeatable
@MustBeDocumented
@Constraint(validatedBy = [RequiredValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Required(
	val mode: Mode = Mode.STRICT,

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
) {

	/**
	 * How [Required] / [RequiredWhen] decide that a value is missing.
	 */
	enum class Mode {

		/**
		 * Fail only when the reference is `null`. Non-null values always pass (including blank
		 * strings and empty containers).
		 *
		 * Annotation arguments must backtick-escape the entry name (write Mode followed by
		 * backtick-NULL-backtick), because bare NULL is not resolved in that position.
		 */
		NULL,

		/**
		 * Fail when `null` or structurally empty: empty [CharSequence] (`length == 0`, whitespace OK),
		 * empty [Collection] / [Map] / array.
		 */
		EMPTY,

		/**
		 * Fail when the value is `null` or every nested collection/map/array/string is empty
		 * (deep emptiness — useful for nested DTOs that look “filled” but contain only blanks).
		 * Blank [CharSequence] values fail as well.
		 */
		STRICT,
	}
}
