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
package io.ghaylan.validata.constraint.validator.comparison.membership

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.InConstraint
import io.ghaylan.validata.internal.ReflectionUtils
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Restricts a scalar subject to the configured allow-list on [InConstraint].
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Non-scalar values
 * (containers, beans, etc.) are skipped at runtime — membership is only meaningful for
 * scalars. Membership uses [Any.toString] against [InConstraint.values].
 *
 * Error: [ConstraintErrorCode.VALUE_NOT_ALLOWED]; the failing [InConstraint] is attached.
 * The message lists the permitted values (annotation policy, not submitted secrets).
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object InValidator : ConstraintValidator<Any, InConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata for this annotation instance (unused; codes are fixed).
	 * @param type Subject runtime type (unused).
	 * @return Always [ConstraintErrorCode.VALUE_NOT_ALLOWED].
	 */
	override fun possibleErrorCodes(
		constraint: InConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_NOT_ALLOWED,
	)

	/**
	 * Validates [value] is one of [constraint] values.
	 *
	 * Non-scalars return `null` without checking. Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying [constraint], or `null` when valid or skipped.
	 */
	override fun validate(
		value: Any,
		constraint: InConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (!ReflectionUtils.isScalar(value)) return null
		if (value.toString() in constraint.values) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = "Must be one of: ${constraint.values.joinToString(", ")}.",
			metadata = constraint,
		)
	}
}
