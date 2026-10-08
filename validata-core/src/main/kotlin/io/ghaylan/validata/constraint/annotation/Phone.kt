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

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.string.phone.PhoneValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import kotlin.reflect.KClass

/**
 * Validates that a [CharSequence] is a valid international phone number, using
 * [Google's libphonenumber](https://github.com/google/libphonenumber).
 *
 * The value must contain **digits only** — no `+`, spaces, dashes, or parentheses — and must
 * include the full international dial prefix (e.g. `"14155552671"` for `+1 415-555-2671`).
 * Formatted input fails immediately with [ConstraintErrorCode.VALUE_FORMAT_INVALID]; strip
 * formatting before assigning the field, or normalize with `PhoneNumberUtils.getCleanNumber`.
 *
 * Requires `com.googlecode.libphonenumber:libphonenumber` on the runtime classpath — startup throws
 * if it is missing and a [Phone] field is validated. `null` is skipped; combine with [Required] when
 * the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Phone(allowedTypes = [PhoneNumberType.MOBILE], allowedCountries = ["US", "CA"])
 * val contactNumber: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 * - [ConstraintErrorCode.VALUE_NOT_ALLOWED]
 *
 * @property allowedTypes Permitted [PhoneNumberType] values from libphonenumber.
 *    Empty (default) = any type allowed.
 * @property allowedCountries Permitted ISO 3166-1 alpha-2 country codes, derived from the
 *    number's dial code. Empty (default) = any country allowed.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [PhoneValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Phone(
	val allowedTypes: Array<PhoneNumberType> = [],
	
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedCountries: Array<String> = [],
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)
