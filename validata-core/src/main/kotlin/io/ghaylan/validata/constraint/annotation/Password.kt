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
import io.ghaylan.validata.constraint.validator.string.password.PasswordValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import kotlin.reflect.KClass

/**
 * Enforces a configurable password policy: length range, required character classes, and optional
 * sequential / repetitive pattern rejection.
 *
 * Checks run in a fixed order and stop at the first failure, so only one error is ever reported
 * per value: [minLength] → [maxLength] → [requireUppercase] → [requireLowercase] →
 * [requireDigit] → [requireSpecialChar] → [noSequentialChars] → [noRepetitivePatterns].
 * `null` values are skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Password(
 *     minLength = 10,
 *     requireUppercase = true,
 *     requireDigit = true,
 *     requireSpecialChar = true,
 *     noSequentialChars = true,
 *     noRepetitivePatterns = true,
 * )
 * val newPassword: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.TEXT_TOO_SHORT]
 * - [ConstraintErrorCode.TEXT_TOO_LONG]
 * - [ConstraintErrorCode.TEXT_PATTERN_MISMATCH] (missing character class, sequential /
 *   repetitive pattern rejection)
 *
 * @property minLength Minimum length in characters (inclusive). Defaults to `6`.
 * @property maxLength Maximum length in characters (inclusive). Defaults to `64`.
 * @property requireUppercase Requires at least one uppercase letter. Defaults to `false`.
 * @property requireLowercase Requires at least one lowercase letter. Defaults to `false`.
 * @property requireDigit Requires at least one digit. Defaults to `false`.
 * @property requireSpecialChar Requires at least one character from [allowedSpecialChars].
 *    Defaults to `false`.
 * @property allowedSpecialChars Character set counted for [requireSpecialChar].
 *    Defaults to `` !@#$%^&*()-_=+[{]};:,<.>/? ``.
 * @property noSequentialChars When `true`, reject ascending/descending letter or digit runs of
 *    length ≥ 3 (case-insensitive for letters), e.g. `"abc"`, `"321"`, `"AbCd"`. Defaults to `false`.
 * @property noRepetitivePatterns When `true`, reject the same character three or more times in a
 *    row (e.g. `"aaaa"`) and consecutive repeating blocks of length ≥ 2 (e.g. `"abcabc"`).
 *    Defaults to `false`.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [PasswordValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Password(
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val minLength: Int = 6,
	
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val maxLength: Int = 64,
	
	val requireUppercase: Boolean = false,
	
	val requireLowercase: Boolean = false,
	
	val requireDigit: Boolean = false,
	
	val requireSpecialChar: Boolean = false,
	
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val allowedSpecialChars: String = "!@#$%^&*()-_=+[{]};:,<.>/?",
	
	val noSequentialChars: Boolean = false,
	
	val noRepetitivePatterns: Boolean = false,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)
