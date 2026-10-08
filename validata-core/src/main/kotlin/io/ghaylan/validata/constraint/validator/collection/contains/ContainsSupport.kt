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

import io.ghaylan.validata.constraint.annotation.Contains
import io.ghaylan.validata.constraint.annotation.ContainsConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode

/**
 * Shared membership checks for [CollectionContainsValidator] and [ArrayContainsValidator].
 *
 * Compares stringified elements to [ContainsConstraint.values] under [Contains.Mode]
 * (ANY / ALL / NONE). Missing required members use [ConstraintErrorCode.COLLECTION_ITEM_MISSING];
 * forbidden members use [ConstraintErrorCode.VALUE_NOT_ALLOWED]. Failures attach the
 * failing [ContainsConstraint]. Messages name the mode's policy values (annotation literals).
 *
 * @author Ghaylan Saada
 */
internal object ContainsSupport {

	/**
	 * Validates stringified [elements] against [constraint] membership mode.
	 *
	 * Side effects: none.
	 *
	 * @param elements Subject elements as strings (null elements become `""`).
	 * @param constraint Required values and [Contains.Mode].
	 * @return Violation for the active mode, or `null` when valid.
	 */
	fun validateElements(
		elements: List<String>,
		constraint: ContainsConstraint,
	): ConstraintError<*>? {
		val satisfied = when (constraint.mode) {
			Contains.Mode.ANY -> {
				constraint.values.any { expected ->
					elements.any { it == expected }
				}
			}

			Contains.Mode.ALL -> {
				constraint.values.all { expected ->
					elements.any { it == expected }
				}
			}

			Contains.Mode.NONE -> {
				constraint.values.none { expected ->
					elements.any { it == expected }
				}
			}
		}
		if (satisfied) return null

		val listed = constraint.values.joinToString(", ")
		val (code, message) = when (constraint.mode) {
			Contains.Mode.ANY -> ConstraintErrorCode.COLLECTION_ITEM_MISSING to
				"Must contain at least one of: $listed."
			Contains.Mode.ALL -> ConstraintErrorCode.COLLECTION_ITEM_MISSING to
				"Must contain all of: $listed."
			Contains.Mode.NONE -> ConstraintErrorCode.VALUE_NOT_ALLOWED to
				"Must not contain any of: $listed."
		}

		return ConstraintError(
			code = code,
			message = message,
			metadata = constraint,
		)
	}

	/**
	 * Stringifies JVM array elements, or `null` when [value] is not an array shape.
	 *
	 * Side effects: none.
	 *
	 * @param value Candidate array value.
	 * @return Element strings, or `null` when not an array.
	 */
	fun arrayElements(value: Any): List<String>? = when (value) {
		is Array<*> -> value.map { it?.toString() ?: "" }
		is ByteArray -> value.map { it.toString() }
		is ShortArray -> value.map { it.toString() }
		is IntArray -> value.map { it.toString() }
		is LongArray -> value.map { it.toString() }
		is FloatArray -> value.map { it.toString() }
		is DoubleArray -> value.map { it.toString() }
		is BooleanArray -> value.map { it.toString() }
		is CharArray -> value.map { it.toString() }
		else -> null
	}
}
