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
package io.ghaylan.validata.constraint.validator.string.ipaddress

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.IpAddress
import io.ghaylan.validata.constraint.annotation.IpAddressConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates IPv4 and IPv6 address literals without DNS lookups.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Failure uses
 * [ConstraintErrorCode.VALUE_FORMAT_INVALID] and attaches the [IpAddressConstraint].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object IpAddressValidator : ConstraintValidator<CharSequence, IpAddressConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: IpAddressConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_FORMAT_INVALID,
	)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached, or `null` when valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: IpAddressConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (IpAddressSupport.isValid(value, constraint.type)) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
			message = when (constraint.type) {
				IpAddress.Type.V4 -> "Must be a valid IPv4 address."
				IpAddress.Type.V6 -> "Must be a valid IPv6 address."
				IpAddress.Type.ANY -> "Must be a valid IPv4 or IPv6 address."
			},
			metadata = constraint,
		)
	}
}
