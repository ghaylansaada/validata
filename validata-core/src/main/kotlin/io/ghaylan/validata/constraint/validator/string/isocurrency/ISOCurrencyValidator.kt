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
package io.ghaylan.validata.constraint.validator.string.isocurrency

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.IsoCurrencyConstraint
import io.ghaylan.validata.constraint.validator.string.isocurrency.IsoCurrencyValidator.currencies
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.*

/**
 * Accepts ISO 4217 currency codes so money fields use a closed, interoperable vocabulary.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Codes are held in a lazy [Set] (O(1) lookup) —
 * a sorted [List] would re-scan on every request.
 *
 * Error: [ConstraintErrorCode.VALUE_NOT_ALLOWED].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object IsoCurrencyValidator : ConstraintValidator<CharSequence, IsoCurrencyConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: IsoCurrencyConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_NOT_ALLOWED)
	
	/**
	 * ISO 4217 codes from [Currency.getAvailableCurrencies], populated once on first use.
	 */
	private val currencies: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
		Currency.getAvailableCurrencies()
			.mapTo(HashSet()) { it.currencyCode }
	}
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: first call may populate [currencies] from [Currency.getAvailableCurrencies].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid.	 
	 */
	override fun validate(
		value: CharSequence,
		constraint: IsoCurrencyConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (currencies.contains(value.toString())) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = "Must be a recognized ISO 4217 currency code.",
			metadata = constraint,
		)
	}
}
