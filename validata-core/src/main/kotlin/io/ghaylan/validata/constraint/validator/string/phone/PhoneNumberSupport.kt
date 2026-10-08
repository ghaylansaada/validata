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
package io.ghaylan.validata.constraint.validator.string.phone

import io.ghaylan.validata.constraint.annotation.PhoneConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode

/**
 * libphonenumber-backed checks shared by [PhoneValidator].
 *
 * Requires digits-only input before libphonenumber parsing — formatted numbers with spaces
 * or punctuation fail fast as [ConstraintErrorCode.VALUE_FORMAT_INVALID].
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID], [ConstraintErrorCode.VALUE_NOT_ALLOWED].
 * Failures attach the [PhoneConstraint].
 *
 * Messages name the allowed types and countries but never echo the submitted number.
 *
 * @author Ghaylan Saada
 */
internal object PhoneNumberSupport {

	/**
	 * Validates [value] as a digits-only phone number under [constraint] type/country filters.
	 *
	 * Fail-closed on non-digit input and libphonenumber parse failure
	 * ([ConstraintErrorCode.VALUE_FORMAT_INVALID]). Empty allow-lists mean “any type/country”.
	 * Side effects: may lazy-load [PhoneNumberUtils] / libphonenumber.
	 *
	 * @param value Digits-only number string (no spaces or punctuation).
	 * @param constraint Phone constraint arguments.
	 * @return Violation with [constraint] attached when restricted, or `null` when valid.
	 */
	fun validate(
		value: CharSequence,
		constraint: PhoneConstraint,
	): ConstraintError<*>? {
		if (!value.all(Char::isDigit) || !PhoneNumberUtils.isValidNumber(value)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "Must be a valid digits-only phone number.",
				metadata = constraint,
			)
		}
		val allowedTypes = constraint.allowedTypes
		if (allowedTypes.isNotEmpty()) {
			val actualType = PhoneNumberUtils.getNumberType(value)
			if (actualType == null || actualType !in allowedTypes) {
				return ConstraintError(
					code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
					message = "Phone number type is not accepted; allowed types: ${allowedTypes.joinToString(", ")}.",
					metadata = constraint,
				)
			}
		}

		if (constraint.allowedCountries.isNotEmpty()) {
			val actualCountry = PhoneNumberUtils.getCountryISOCode(value)
			if (actualCountry == null || actualCountry !in constraint.allowedCountries) {
				return ConstraintError(
					code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
					message = "Phone number country is not accepted; allowed countries: ${constraint.allowedCountries.joinToString(", ")}.",
					metadata = constraint,
				)
			}
		}

		return null
	}
}
