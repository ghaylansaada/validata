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
package io.ghaylan.validata.constraint.validator.required

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Unconditional presence via [RequiredConstraint.mode].
 *
 * Uses `V = Any` deliberately: presence applies to every subject type. Mode chooses null-only
 * ([Required.Mode.NULL]), shallow empty ([Required.Mode.EMPTY]), or deep emptiness
 * ([Required.Mode.STRICT]).
 *
 * Null subjects are handled by [validateNull]; empty / blank / deep-empty by [validate].
 * Failures carry no typed error context — the code alone identifies the presence shape.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object RequiredValidator : ConstraintValidator<Any, RequiredConstraint>() {
	
	/**
	 * Error codes this validator may emit for [RequiredConstraint.mode] on subject [type].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata whose [RequiredConstraint.mode] selects the code set.
	 * @param type Runtime subject class (e.g. `String`, `List`, [Any]).
	 * @return Codes [PresenceSupport.possibleErrorCodes] reports for that mode and type.
	 */
	override fun possibleErrorCodes(
		constraint: RequiredConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		PresenceSupport.possibleErrorCodes(constraint.mode, type)
	
	/**
	 * Fails when the subject is `null` under [RequiredConstraint.mode].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free [ConstraintError] with [ConstraintErrorCode.VALUE_MISSING] (or
	 *   mode-specific code), or `null` when null is acceptable for the mode.	 
	 */
	override fun validateNull(
		constraint: RequiredConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (!PresenceSupport.isMissing(null, constraint.mode)) return null
		return PresenceSupport.errorFor(
			value = null,
			constraint = constraint,
		)
	}

	/**
	 * Enforces [RequiredConstraint.mode] presence on a non-null [value] (empty / blank /
	 * deep-empty).
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null`.
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free [ConstraintError] with [ConstraintErrorCode.TEXT_BLANK] or
	 *   [ConstraintErrorCode.VALUE_EMPTY], or `null` when present.
	 */
	override fun validate(
		value: Any,
		constraint: RequiredConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (!PresenceSupport.isMissing(value, constraint.mode)) return null
		return PresenceSupport.errorFor(
			value = value,
			constraint = constraint,
		)
	}
}
