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
package io.ghaylan.validata.constraint.validator.collection.contains

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Contains
import io.ghaylan.validata.constraint.annotation.ContainsConstraint
import io.ghaylan.validata.constraint.validator.size.ArraySizeValidator
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * `@Contains` for JVM arrays (`Array<T>` and primitive arrays).
 *
 * Non-array [Cloneable] values skip silently (same pattern as [ArraySizeValidator]).
 *
 * @author Ghaylan Saada
 */
object ArrayContainsValidator : ConstraintValidator<Cloneable, ContainsConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 *
	 * @param constraint Contains metadata; [Contains.Mode] selects the code.
	 * @return [ConstraintErrorCode.COLLECTION_ITEM_MISSING] for ANY/ALL, or
	 *   [ConstraintErrorCode.VALUE_NOT_ALLOWED] for NONE.
	 */
	override fun possibleErrorCodes(
		constraint: ContainsConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		when (constraint.mode) {
			Contains.Mode.ANY,
			Contains.Mode.ALL -> ConstraintErrorCode.COLLECTION_ITEM_MISSING
			Contains.Mode.NONE -> ConstraintErrorCode.VALUE_NOT_ALLOWED
		},
	)

	/**
	 * Validates [value] against [constraint].
	 *
	 * @param value Subject under validation; never `null`.
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid or [value] is not an array.
	 */
	override fun validate(
		value: Cloneable,
		constraint: ContainsConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val elements = ContainsSupport.arrayElements(value) ?: return null
		return ContainsSupport.validateElements(
			elements = elements,
			constraint = constraint,
		)
	}
}
