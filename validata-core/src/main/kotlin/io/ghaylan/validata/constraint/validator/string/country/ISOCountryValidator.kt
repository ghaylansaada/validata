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
package io.ghaylan.validata.constraint.validator.string.country

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.IsoCountryConstraint
import io.ghaylan.validata.constraint.validator.string.country.IsoCountryValidator.validCountryCodes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.*

/**
 * Accepts ISO 3166 alpha-2 country codes — rejects free-text country names at the API edge.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Valid codes come from [Locale.getISOCountries]
 * and are cached in a lazy [Set] for O(1) lookups.
 *
 * Error: [ConstraintErrorCode.VALUE_NOT_ALLOWED].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object IsoCountryValidator : ConstraintValidator<CharSequence, IsoCountryConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: IsoCountryConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_NOT_ALLOWED)
	
	/**
	 * ISO 3166 alpha-2 codes from [Locale.getISOCountries], populated once on first use.
	 */
	private val validCountryCodes: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
		Locale.getISOCountries()
			.toSet()
	}
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: first call may populate [validCountryCodes] from [Locale.getISOCountries].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid.	 
	 */
	override fun validate(
		value: CharSequence,
		constraint: IsoCountryConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (validCountryCodes.contains(value.toString())) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = "Must be a recognized ISO 3166-1 alpha-2 country code.",
			metadata = constraint,
		)
	}
}
