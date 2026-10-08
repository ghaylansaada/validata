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
package io.ghaylan.validata.constraint.validator.comparison

import io.ghaylan.validata.constraint.annotation.CompareConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode

/**
 * Shared type-guard and compare helpers for cross-field comparison validators (`@Compare`).
 *
 * Keeps the strict `isInstance` check and unchecked [Comparable.compareTo] cast in one place so
 * the four validators stay thin policy objects. Domain detail (sibling name, inclusive) belongs
 * in messages and the attached [CompareConstraint] — codes stay abstract
 * ([ConstraintErrorCode] comparison family).
 *
 * Messages name the referenced sibling property but never echo either compared value.
 *
 * @author Ghaylan Saada
 */
internal object ComparisonSupport {

	/**
	 * Returns a [ConstraintErrorCode.COMPARISON_NOT_ORDERABLE] error when [other] is not an
	 * instance of [value]'s runtime class; otherwise `null`.
	 *
	 * Side effects: none.
	 *
	 * @param value Annotated subject (defines the expected type).
	 * @param other Sibling value being compared.
	 * @param refName Wire name of the referenced sibling property, used in the message.
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Type-mismatch violation, or `null` when compatible.
	 */
	fun typeMismatchOrNull(
		value: Any,
		other: Any,
		refName: String,
		constraint: CompareConstraint,
	): ConstraintError<*>? {
		if (value.javaClass.isInstance(other)) return null
		return ConstraintError(
			code = ConstraintErrorCode.COMPARISON_NOT_ORDERABLE,
			message = "Cannot be compared with '$refName': the two fields hold different types.",
			metadata = constraint,
		)
	}

	/**
	 * Builds the `@Compare` failure message.
	 *
	 * Side effects: none.
	 *
	 * @param refName Wire name of the referenced sibling property.
	 * @param inclusive Whether the constraint allows equality (`>=`).
	 * @return Rule sentence naming [refName].
	 */
	fun greaterThanMessage(
		refName: String,
		inclusive: Boolean,
	): String = if (inclusive) "Must be greater than or equal to '$refName'."
	else "Must be greater than '$refName'."

	/**
	 * Builds the `@Compare` failure message.
	 *
	 * Side effects: none.
	 *
	 * @param refName Wire name of the referenced sibling property.
	 * @param inclusive Whether the constraint allows equality (`<=`).
	 * @return Rule sentence naming [refName].
	 */
	fun lessThanMessage(
		refName: String,
		inclusive: Boolean,
	): String = if (inclusive) "Must be less than or equal to '$refName'."
	else "Must be less than '$refName'."

	/**
	 * Compares [value] to [other] after [typeMismatchOrNull] has succeeded.
	 *
	 * Side effects: none.
	 *
	 * @param value Annotated comparable subject.
	 * @param other Sibling value already type-checked against [value].
	 * @return Negative / zero / positive per [Comparable.compareTo].
	 */
	@Suppress("UNCHECKED_CAST")
	fun compare(
		value: Comparable<*>,
		other: Any,
	): Int = (value as Comparable<Any>).compareTo(other)

	/**
	 * Error code when `@Compare` fails (annotated / left value too small relative to the
	 * sibling).
	 *
	 * Side effects: none.
	 *
	 * @param inclusive Whether the constraint allows equality (`>=`).
	 * @return [ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN] or
	 *   [ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL].
	 */
	fun greaterThanFailureCode(inclusive: Boolean): ConstraintErrorCode = if (inclusive) ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN
	else ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL

	/**
	 * Error code when `@Compare` fails (annotated / left value too large relative to the
	 * sibling).
	 *
	 * Side effects: none.
	 *
	 * @param inclusive Whether the constraint allows equality (`<=`).
	 * @return [ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN] or
	 *   [ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL].
	 */
	fun lessThanFailureCode(inclusive: Boolean): ConstraintErrorCode = if (inclusive) ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN
	else ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL
}
