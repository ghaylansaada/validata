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

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.PhoneConstraint
import io.ghaylan.validata.internal.OptionalDependencies
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates phone numbers via libphonenumber (optional classpath dependency).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Parsing and type/country policy checks
 * delegate to [PhoneNumberSupport].
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID], [ConstraintErrorCode.VALUE_NOT_ALLOWED].
 * Failures attach the [PhoneConstraint]. Messages never echo the submitted phone number.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object PhoneValidator : ConstraintValidator<CharSequence, PhoneConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint] restrictions.
	 *
	 * [ConstraintErrorCode.VALUE_NOT_ALLOWED] only when type or country allow-lists are set.
	 */
	override fun possibleErrorCodes(
		constraint: PhoneConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		buildSet {
			add(ConstraintErrorCode.VALUE_FORMAT_INVALID)
			if (constraint.allowedTypes.isNotEmpty() || constraint.allowedCountries.isNotEmpty()) {
				add(ConstraintErrorCode.VALUE_NOT_ALLOWED)
			}
		}

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 *
	 * Side effects: [OptionalDependencies.requireLibphonenumber] fails fast when the library
	 * is absent; successful calls may lazy-load [PhoneNumberUtils].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached when restricted, or `null` when
	 *   valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: PhoneConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		OptionalDependencies.requireLibphonenumber()
		return PhoneNumberSupport.validate(value, constraint)
	}
}
