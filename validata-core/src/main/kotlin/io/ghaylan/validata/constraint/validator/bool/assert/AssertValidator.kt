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
package io.ghaylan.validata.constraint.validator.bool.assert

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.AssertConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Requires a boolean to equal [AssertConstraint.value].
 *
 * Null subjects are skipped by the engine (presence is `@Required`).
 * Failure uses [ConstraintErrorCode.VALUE_NOT_ALLOWED] — the failing constraint is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object AssertValidator : ConstraintValidator<Boolean, AssertConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata for this annotation instance (unused; codes are fixed).
	 * @return Always [ConstraintErrorCode.VALUE_NOT_ALLOWED].
	 */
	override fun possibleErrorCodes(
		constraint: AssertConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_NOT_ALLOWED)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing constraint, or `null` when valid.
	 */
	override fun validate(
		value: Boolean,
		constraint: AssertConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (value == constraint.value) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = if (constraint.value) "Must be true." else "Must be false.",
			metadata = constraint,
		)
	}
}
