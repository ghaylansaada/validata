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
import io.ghaylan.validata.constraint.validator.string.financial.FinancialCodeValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import kotlin.reflect.KClass

/**
 * Asserts that a [CharSequence] is a valid financial identifier of the given [type].
 *
 * Whitespace is stripped and the value is uppercased before checks. Each [Type] enforces its own
 * structure and checksum:
 *
 * - [Type.IBAN] — ISO 13616 IBAN: known country code, fixed country length, MOD-97-10 checksum.
 * - [Type.ISIN] — ISO 6166 ISIN: 12 characters (2-letter country + 9 alphanumeric + Luhn check).
 * - [Type.BIC] — BIC/SWIFT: 8 or 11 characters matching bank + country + location (+ optional branch).
 *
 * When [countries] is non-empty, the embedded ISO country code must be one of those values
 * (compared uppercase). Empty means any country is accepted (IBAN still requires a registry entry).
 *
 * `null` values are skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:FinancialCode(type = FinancialCode.Type.IBAN, countries = ["DE", "FR"])
 * val accountIban: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 * - [ConstraintErrorCode.VALUE_NOT_ALLOWED]
 * - [ConstraintErrorCode.VALUE_CHECKSUM_INVALID]
 *
 * @property type Financial identifier family to validate against.
 * @property countries Optional ISO 3166-1 alpha-2 country filter. Empty = any country.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [FinancialCodeValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class FinancialCode(
	val type: Type,

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val countries: Array<String> = [],

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {

	/**
	 * Supported financial identifier families.
	 */
	enum class Type {

		/**
		 * International Bank Account Number (ISO 13616) with MOD-97-10 checksum.
		 */
		IBAN,

		/**
		 * International Securities Identification Number (ISO 6166) with Luhn check digit.
		 */
		ISIN,

		/**
		 * Bank Identifier Code / SWIFT address (8 or 11 characters).
		 */
		BIC,
	}
}
