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
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import kotlin.reflect.KClass

/**
 * Checks that a text field looks like an email address: `local-part@domain.tld`.
 *
 * Syntax-only — does not verify deliverability, MX records, or domain existence. Local part and
 * each domain label allow letters, digits, `-` and `_` separated by single dots; final TLD is 1–6
 * letters. Empty string fails the syntax check. `null` is skipped; combine with [Required] when
 * the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Email
 * val contactEmail: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 *
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [EmailValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Email(
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)