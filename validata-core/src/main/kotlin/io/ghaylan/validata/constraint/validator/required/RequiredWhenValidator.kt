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
import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.constraint.annotation.RequiredWhenConstraint
import io.ghaylan.validata.ext.toConstraintNumber
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Conditional presence: enforces [RequiredWhenConstraint.mode] only when a sibling property
 * satisfies the configured gate ([RequiredWhen.Condition]).
 *
 * Gate comparisons use `toString()` on non-null sibling values; the ordering conditions
 * ([RequiredWhen.Condition.GT] and friends) compare numerically when both the gate and the
 * configured literal parse as decimals, and lexicographically otherwise. When the gate does not
 * match, the annotated value is not checked (including when it is `null`).
 *
 * Null subjects are handled by [validateNull]; empty / blank / deep-empty by [validate].
 *
 * Uses `V = Any` deliberately: presence applies to every subject type. Violation codes mirror
 * [RequiredValidator]; the failing [RequiredWhenConstraint] is attached (ref, condition, literals).
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object RequiredWhenValidator : ConstraintValidator<Any, RequiredWhenConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint.mode] on subject [type] when the gate matches.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata whose [RequiredWhenConstraint.mode] selects the code set.
	 * @param type Runtime subject class (e.g. `String`, `List`, [Any]).
	 * @return Codes [PresenceSupport.possibleErrorCodes] reports for that mode and type.
	 */
	override fun possibleErrorCodes(
		constraint: RequiredWhenConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		PresenceSupport.possibleErrorCodes(constraint.mode, type)

	/**
	 * When the sibling gate matches, fails if the subject is `null` under
	 * [RequiredWhenConstraint.mode].
	 *
	 * Side effects: none.
	 *
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached, or `null` when untriggered
	 *   or null is acceptable.
	 */
	override fun validateNull(
		constraint: RequiredWhenConstraint,
		context: ValidationContext,
	): ConstraintError<*>? = presenceError(
		value = null,
		constraint = constraint,
		context = context,
	)

	/**
	 * Enforces [RequiredWhenConstraint.mode] on a non-null [value] only when the sibling gate
	 * matches.
	 *
	 * When the gate does not match, returns `null` without checking [value].
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null`.
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached, or `null` when valid or
	 *   untriggered.
	 */
	override fun validate(
		value: Any,
		constraint: RequiredWhenConstraint,
		context: ValidationContext,
	): ConstraintError<*>? = presenceError(
		value = value,
		constraint = constraint,
		context = context,
	)

	/**
	 * Shared presence check after the sibling gate matches.
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under validation; may be `null` (from [validateNull]).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor for sibling reads.
	 * @return Path-free violation, or `null` when untriggered or present.
	 */
	private fun presenceError(
		value: Any?,
		constraint: RequiredWhenConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val gate = getPropertyValue(
			name = constraint.ref,
			context = context,
		)
		if (!gateMatches(gate, constraint)) return null
		if (!PresenceSupport.isMissing(value, constraint.mode)) return null
		val base = PresenceSupport.errorFor(
			value = value,
			constraint = constraint,
		)
		return ConstraintError(
			code = base.code,
			message = "${triggerMessage(constraint)} ${base.message}",
			metadata = constraint,
		)
	}

	/**
	 * Whether the sibling [gate] satisfies [RequiredWhenConstraint.condition].
	 *
	 * Side effects: none.
	 *
	 * @param gate Sibling property value; may be `null`.
	 * @param constraint Gate condition and comparison literals.
	 * @return `true` when the annotated field must be present.
	 */
	private fun gateMatches(
		gate: Any?,
		constraint: RequiredWhenConstraint,
	): Boolean = when (constraint.condition) {
		RequiredWhen.Condition.MISSING -> PresenceSupport.isMissing(gate, constraint.mode)
		RequiredWhen.Condition.PRESENT -> !PresenceSupport.isMissing(gate, constraint.mode)
		RequiredWhen.Condition.EQ -> gate != null && gate.toString() == constraint.value
		RequiredWhen.Condition.NE -> gate != null && gate.toString() != constraint.value
		RequiredWhen.Condition.IN -> gate != null && gate.toString() in constraint.values
		RequiredWhen.Condition.NIN -> gate != null && gate.toString() !in constraint.values
		RequiredWhen.Condition.GT -> orderMatches(gate, constraint.value) { it > 0 }
		RequiredWhen.Condition.LT -> orderMatches(gate, constraint.value) { it < 0 }
		RequiredWhen.Condition.GTE -> orderMatches(gate, constraint.value) { it >= 0 }
		RequiredWhen.Condition.LTE -> orderMatches(gate, constraint.value) { it <= 0 }
	}

	/**
	 * Orders the [gate]'s string form against [literal] and tests the sign with [accept].
	 *
	 * Numbers are compared as decimals when both sides parse; anything else (text, dates in ISO
	 * form, enums) falls back to lexicographic order, which keeps ISO-8601 temporal literals
	 * chronological. A `null` gate never matches. Side effects: none.
	 *
	 * @param gate Sibling property value; may be `null`.
	 * @param literal Configured `@RequiredWhen.value` literal.
	 * @param accept Predicate over the comparison sign.
	 * @return `true` when the gate is non-null and [accept] holds for the comparison.
	 */
	private inline fun orderMatches(
		gate: Any?,
		literal: String,
		accept: (Int) -> Boolean,
	): Boolean {
		val text = gate?.toString() ?: return false
		val gateNumber = text.toConstraintNumber()
		val literalNumber = literal.toConstraintNumber()
		val comparison = if (gateNumber != null && literalNumber != null) {
			gateNumber.compareTo(literalNumber)
		} else {
			text.compareTo(literal)
		}
		return accept(comparison)
	}

	/**
	 * Sentence stating why the field is required, prefixed to the presence message.
	 *
	 * Names the referenced sibling and the configured literals — both come from the annotation,
	 * never from submitted data.
	 * Side effects: none.
	 *
	 * @param constraint Gate metadata used in the message text.
	 * @return Complete sentence ending in a period.
	 */
	private fun triggerMessage(constraint: RequiredWhenConstraint): String {
		val ref = constraint.ref
		return when (constraint.condition) {
			RequiredWhen.Condition.MISSING -> "Required when '$ref' is not provided."
			RequiredWhen.Condition.PRESENT -> "Required when '$ref' is provided."
			RequiredWhen.Condition.EQ -> "Required when '$ref' is '${constraint.value}'."
			RequiredWhen.Condition.NE -> "Required when '$ref' is not '${constraint.value}'."
			RequiredWhen.Condition.IN -> "Required when '$ref' is one of: ${constraint.values.joinToString(", ")}."
			RequiredWhen.Condition.NIN -> "Required when '$ref' is not one of: ${constraint.values.joinToString(", ")}."
			RequiredWhen.Condition.GT -> "Required when '$ref' is greater than '${constraint.value}'."
			RequiredWhen.Condition.LT -> "Required when '$ref' is less than '${constraint.value}'."
			RequiredWhen.Condition.GTE -> "Required when '$ref' is at least '${constraint.value}'."
			RequiredWhen.Condition.LTE -> "Required when '$ref' is at most '${constraint.value}'."
		}
	}
}
