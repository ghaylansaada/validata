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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.ext.isDeepNullOrEmpty
import io.ghaylan.validata.internal.SubjectTypes
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition

/**
 * Shared presence semantics for [RequiredValidator] and [RequiredWhenValidator].
 *
 * Both constraints use [Required.Mode] — one type, one implementation.
 *
 * [Required.Mode.NULL] fails only when the reference is `null`. [Required.Mode.STRICT]
 * delegates to [isDeepNullOrEmpty]. [Required.Mode.EMPTY] uses shallow emptiness checks.
 *
 * Violations use abstract presence codes; callers distinguish shapes via the code (and optional
 * host messaging), not domain-specific enums:
 * - `null` → [ConstraintErrorCode.VALUE_MISSING]
 * - blank [CharSequence] → [ConstraintErrorCode.TEXT_BLANK]
 * - empty container / array / deep-empty → [ConstraintErrorCode.VALUE_EMPTY]
 *
 * @author Ghaylan Saada
 */
internal object PresenceSupport {

	/**
	 * Error codes [RequiredValidator] / [RequiredWhenValidator] may emit for [mode] on [type].
	 *
	 * Side effects: none.
	 *
	 * @param mode Presence mode from the constraint.
	 * @param type Runtime subject class (may be coarse: `String`, `Collection`, [Any], …).
	 * @return Codes that [errorFor] can produce under [mode] for [type].
	 */
	fun possibleErrorCodes(
		mode: Required.Mode,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		if (mode == Required.Mode.NULL) {
			return setOf(ConstraintErrorCode.VALUE_MISSING)
		}
		val codes = linkedSetOf<ConstraintErrorDefinition>(ConstraintErrorCode.VALUE_MISSING)
		if (SubjectTypes.isUnknown(type)) {
			codes += ConstraintErrorCode.TEXT_BLANK
			codes += ConstraintErrorCode.VALUE_EMPTY
			return codes
		}
		if (SubjectTypes.isCharSequence(type)) {
			// Empty / blank text always maps to TEXT_BLANK in [errorFor] — never VALUE_EMPTY.
			codes += ConstraintErrorCode.TEXT_BLANK
			return codes
		}
		if (SubjectTypes.isCollectionLike(type) || SubjectTypes.isMap(type)) {
			codes += ConstraintErrorCode.VALUE_EMPTY
			return codes
		}
		if (SubjectTypes.isChar(type)) {
			// Whitespace / NUL char fails only under STRICT (shallow EMPTY leaves Char alone).
			if (mode == Required.Mode.STRICT) {
				codes += ConstraintErrorCode.VALUE_EMPTY
			}
			return codes
		}
		if (mode == Required.Mode.STRICT && SubjectTypes.mayBeDeepEmptyStructured(type)) {
			codes += ConstraintErrorCode.VALUE_EMPTY
		}
		return codes
	}

	/**
	 * Error codes [RequiredValidator] / [RequiredWhenValidator] may emit for [mode].
	 *
	 * Conservative overload when the subject class is unknown — includes blank and empty.
	 *
	 * @param mode Presence mode from the constraint.
	 * @return Codes that [errorFor] can produce under [mode].
	 */
	fun possibleErrorCodes(mode: Required.Mode): Set<ConstraintErrorDefinition> =
		possibleErrorCodes(mode, Any::class.java)

	/**
	 * Whether [value] fails the presence rule for [mode].
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under test; may be `null`.
	 * @param mode Presence mode from the constraint.
	 * @return `true` when the value is considered missing/empty under [mode].
	 */
	fun isMissing(
		value: Any?,
		mode: Required.Mode,
	): Boolean = when (mode) {
		Required.Mode.NULL -> value == null
		Required.Mode.EMPTY -> isNullOrShallowEmpty(value)
		Required.Mode.STRICT -> value.isDeepNullOrEmpty()
	}

	/**
	 * Builds the standard presence violation for [value].
	 *
	 * Side effects: none.
	 *
	 * @param value Subject that failed presence; may be `null`.
	 * @param constraint Constraint metadata that failed; attached when provided.
	 * @return [ConstraintError] with [ConstraintErrorCode.VALUE_MISSING],
	 *   [ConstraintErrorCode.TEXT_BLANK], or [ConstraintErrorCode.VALUE_EMPTY].
	 */
	fun errorFor(
		value: Any?,
		constraint: Any? = null,
	): ConstraintError<*> {
		val (code, message) = when (value) {
			null -> ConstraintErrorCode.VALUE_MISSING to "Must not be null."
			is CharSequence if value.isBlank() -> ConstraintErrorCode.TEXT_BLANK to "Must not be blank."
			else -> ConstraintErrorCode.VALUE_EMPTY to "Must not be empty."
		}
		return ConstraintError(
			code = code,
			message = message,
			metadata = constraint,
		)
	}

	/**
	 * Whether [value] is `null` or a shallow-empty CharSequence / collection / map / array.
	 *
	 * Side effects: none.
	 *
	 * @param value Subject under test; may be `null`.
	 * @return `true` when null or shallow-empty.
	 */
	private fun isNullOrShallowEmpty(value: Any?): Boolean {
		return value == null || when (value) {
			is CharSequence -> value.isEmpty()
			is Collection<*> -> value.isEmpty()
			is Map<*, *> -> value.isEmpty()
			is Array<*> -> value.isEmpty()
			is BooleanArray -> value.isEmpty()
			is ByteArray -> value.isEmpty()
			is CharArray -> value.isEmpty()
			is ShortArray -> value.isEmpty()
			is IntArray -> value.isEmpty()
			is LongArray -> value.isEmpty()
			is FloatArray -> value.isEmpty()
			is DoubleArray -> value.isEmpty()
			else -> false
		}
	}
}
