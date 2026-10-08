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
package io.ghaylan.validata.constraint.validator.string.financial

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.FinancialCode
import io.ghaylan.validata.constraint.annotation.FinancialCodeConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates financial identifiers (IBAN, ISIN, BIC/SWIFT) before downstream finance calls.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Input is whitespace-stripped
 * and uppercased. Dispatches on [FinancialCode.Type].
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID], [ConstraintErrorCode.VALUE_NOT_ALLOWED],
 * [ConstraintErrorCode.VALUE_CHECKSUM_INVALID]. Failures attach the [FinancialCodeConstraint].
 *
 * Messages name the country code and its required length but never echo the submitted identifier.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object FinancialCodeValidator : ConstraintValidator<CharSequence, FinancialCodeConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint.type] / country filter.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Identifier type and optional country allow-list.
	 * @param type Subject runtime type (unused for financial codes).
	 * @return Codes that can fire for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: FinancialCodeConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>(ConstraintErrorCode.VALUE_FORMAT_INVALID)
		when (constraint.type) {
			FinancialCode.Type.IBAN -> {
				codes += ConstraintErrorCode.VALUE_NOT_ALLOWED
				codes += ConstraintErrorCode.VALUE_CHECKSUM_INVALID
			}
			FinancialCode.Type.BIC -> {
				if (constraint.countries.isNotEmpty()) {
					codes += ConstraintErrorCode.VALUE_NOT_ALLOWED
				}
			}
			FinancialCode.Type.ISIN -> {
				codes += ConstraintErrorCode.VALUE_CHECKSUM_INVALID
				if (constraint.countries.isNotEmpty()) {
					codes += ConstraintErrorCode.VALUE_NOT_ALLOWED
				}
			}
		}
		return codes
	}

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached when applicable, or `null` when
	 *   valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: FinancialCodeConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val clean = FinancialCodeSupport.normalize(value)
		return when (constraint.type) {
			FinancialCode.Type.IBAN -> FinancialCodeSupport.validateIban(clean, constraint)
			FinancialCode.Type.BIC -> FinancialCodeSupport.validateBic(clean, constraint)
			FinancialCode.Type.ISIN -> FinancialCodeSupport.validateIsin(clean, constraint)
		}
	}
}
