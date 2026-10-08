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
import io.ghaylan.validata.constraint.validator.string.regex.RegexValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import java.util.regex.Pattern
import kotlin.reflect.KClass

/**
 * Asserts that a [CharSequence] fully matches [pattern] (entire input, not a substring). Escape hatch
 * for domain formats that lack a dedicated annotation.
 *
 * [pattern] must be a valid [Pattern] string; invalid patterns are rejected via [ConstraintArgKind.REGEX].
 * [name] is opaque metadata on the attached [RegexConstraint] so clients can tell which named pattern
 * failed without parsing the regex — it does not affect matching. `null` is skipped; combine with
 * [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Regex(pattern = "\\d{5}(-\\d{4})?", name = "US_ZIP_CODE")
 * val zipCode: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.TEXT_PATTERN_MISMATCH]
 *
 * @property pattern Regular expression the value must fully match.
 * @property name Human-readable identifier for the expected format (e.g. `"POSTAL_CODE"`),
 *    preferred in error messages instead of echoing a long raw pattern.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [RegexValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Regex(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.REGEX)
	val pattern: String,
	
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val name: String,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)