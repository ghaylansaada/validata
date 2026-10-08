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
package io.ghaylan.validata.constraint.validator.comparison.compare

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Compare
import io.ghaylan.validata.constraint.annotation.CompareConstraint
import io.ghaylan.validata.constraint.validator.comparison.ComparisonSupport
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Cross-field comparison: requires the annotated [Comparable] to satisfy
 * [CompareConstraint.operation] against sibling [CompareConstraint.ref].
 *
 * Null subjects are skipped by the engine (presence is `@Required`). A null sibling is
 * treated as unequal for [Compare.Operation.EQ] (fails) and as a no-op pass for the
 * other operations. Type-incompatible siblings yield
 * [ConstraintErrorCode.COMPARISON_NOT_ORDERABLE]; operation failures use the comparison
 * family codes from [possibleErrorCodes]. The failing [CompareConstraint] is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object CompareValidator : ConstraintValidator<Comparable<*>, CompareConstraint>() {

	/**
	 * Error codes this validator may emit for [CompareConstraint.operation].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata whose operation selects the failure code set.
	 * @return [ConstraintErrorCode.COMPARISON_NOT_ORDERABLE] plus the operation-specific code.
	 */
	override fun possibleErrorCodes(
		constraint: CompareConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>(ConstraintErrorCode.COMPARISON_NOT_ORDERABLE)
		codes += when (constraint.operation) {
			Compare.Operation.EQ -> ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL
			Compare.Operation.NE -> ConstraintErrorCode.COMPARISON_UNSATISFIED_EQUAL
			Compare.Operation.GT -> ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN
			Compare.Operation.GTE -> ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL
			Compare.Operation.LT -> ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN
			Compare.Operation.LTE -> ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL
		}
		return codes
	}

	/**
	 * Validates [value] against the sibling named by [CompareConstraint.ref].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying [constraint], or `null` when valid.
	 */
	override fun validate(
		value: Comparable<*>,
		constraint: CompareConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val ref = constraint.ref
		val other = getPropertyValue(
			name = ref,
			context = context,
		)
		return when (constraint.operation) {
			Compare.Operation.EQ -> validateEqual(
				value = value,
				other = other,
				ref = ref,
				constraint = constraint,
			)
			Compare.Operation.NE -> validateNotEqual(
				value = value,
				other = other,
				ref = ref,
				constraint = constraint,
			)
			Compare.Operation.GT -> validateGreater(
				value = value,
				other = other,
				ref = ref,
				inclusive = false,
				constraint = constraint,
			)
			Compare.Operation.GTE -> validateGreater(
				value = value,
				other = other,
				ref = ref,
				inclusive = true,
				constraint = constraint,
			)
			Compare.Operation.LT -> validateLower(
				value = value,
				other = other,
				ref = ref,
				inclusive = false,
				constraint = constraint,
			)
			Compare.Operation.LTE -> validateLower(
				value = value,
				other = other,
				ref = ref,
				inclusive = true,
				constraint = constraint,
			)
		}
	}

	/**
	 * Requires [value] to equal [other] (sibling [ref]).
	 *
	 * Side effects: none.
	 *
	 * @param value Annotated subject.
	 * @param other Sibling value; may be `null` (treated as not equal).
	 * @param ref Wire name of the referenced sibling.
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Violation, or `null` when equal and type-compatible.
	 */
	private fun validateEqual(
		value: Comparable<*>,
		other: Any?,
		ref: String,
		constraint: CompareConstraint,
	): ConstraintError<*>? {
		if (other != null) {
			ComparisonSupport.typeMismatchOrNull(
				value = value,
				other = other,
				refName = ref,
				constraint = constraint,
			)?.let { return it }
			if (ComparisonSupport.compare(value, other) == 0) return null
		}
		return ConstraintError(
			code = ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL,
			message = "Must match '$ref'.",
			metadata = constraint,
		)
	}

	/**
	 * Requires [value] to differ from [other] (sibling [ref]).
	 *
	 * A null sibling passes. Side effects: none.
	 *
	 * @param value Annotated subject.
	 * @param other Sibling value; may be `null`.
	 * @param ref Wire name of the referenced sibling.
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Violation, or `null` when unequal, null sibling, or type-incompatible handled.
	 */
	private fun validateNotEqual(
		value: Comparable<*>,
		other: Any?,
		ref: String,
		constraint: CompareConstraint,
	): ConstraintError<*>? {
		if (other == null) return null
		ComparisonSupport.typeMismatchOrNull(
			value = value,
			other = other,
			refName = ref,
			constraint = constraint,
		)?.let { return it }
		if (ComparisonSupport.compare(value, other) != 0) return null
		return ConstraintError(
			code = ConstraintErrorCode.COMPARISON_UNSATISFIED_EQUAL,
			message = "Must be different from '$ref'.",
			metadata = constraint,
		)
	}

	/**
	 * Requires [value] to be greater than (or equal to, when [inclusive]) sibling [other].
	 *
	 * A null sibling passes. Side effects: none.
	 *
	 * @param value Annotated subject.
	 * @param other Sibling value; may be `null`.
	 * @param ref Wire name of the referenced sibling.
	 * @param inclusive When `true`, equality is allowed (`GTE`).
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Violation, or `null` when the ordering holds or the sibling is null.
	 */
	private fun validateGreater(
		value: Comparable<*>,
		other: Any?,
		ref: String,
		inclusive: Boolean,
		constraint: CompareConstraint,
	): ConstraintError<*>? {
		if (other == null) return null
		ComparisonSupport.typeMismatchOrNull(
			value = value,
			other = other,
			refName = ref,
			constraint = constraint,
		)?.let { return it }
		val comparison = ComparisonSupport.compare(value, other)
		val violates = if (inclusive) comparison < 0 else comparison <= 0
		if (!violates) return null
		return ConstraintError(
			code = ComparisonSupport.greaterThanFailureCode(inclusive),
			message = ComparisonSupport.greaterThanMessage(ref, inclusive),
			metadata = constraint,
		)
	}

	/**
	 * Requires [value] to be lower than (or equal to, when [inclusive]) sibling [other].
	 *
	 * A null sibling passes. Side effects: none.
	 *
	 * @param value Annotated subject.
	 * @param other Sibling value; may be `null`.
	 * @param ref Wire name of the referenced sibling.
	 * @param inclusive When `true`, equality is allowed (`LTE`).
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Violation, or `null` when the ordering holds or the sibling is null.
	 */
	private fun validateLower(
		value: Comparable<*>,
		other: Any?,
		ref: String,
		inclusive: Boolean,
		constraint: CompareConstraint,
	): ConstraintError<*>? {
		if (other == null) return null
		ComparisonSupport.typeMismatchOrNull(
			value = value,
			other = other,
			refName = ref,
			constraint = constraint,
		)?.let { return it }
		val comparison = ComparisonSupport.compare(value, other)
		val violates = if (inclusive) comparison > 0 else comparison >= 0
		if (!violates) return null
		return ConstraintError(
			code = ComparisonSupport.lessThanFailureCode(inclusive),
			message = ComparisonSupport.lessThanMessage(ref, inclusive),
			metadata = constraint,
		)
	}
}
