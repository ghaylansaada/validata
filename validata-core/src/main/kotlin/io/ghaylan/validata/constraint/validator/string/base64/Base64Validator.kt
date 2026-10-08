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
package io.ghaylan.validata.constraint.validator.string.base64

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Base64Constraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.Base64

/**
 * Validates Base64-encoded text (standard or URL-safe alphabet).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Failure uses
 * [ConstraintErrorCode.VALUE_FORMAT_INVALID] and attaches the [Base64Constraint].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object Base64Validator : ConstraintValidator<CharSequence, Base64Constraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: Base64Constraint,
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
		constraint: Base64Constraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (isValid(value.toString(), constraint.urlSafe, constraint.requirePadding)) return null
		val alphabetNote = if (constraint.urlSafe) " (URL-safe alphabet)" else ""
		val paddingNote = if (constraint.requirePadding) " with padding" else ""
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
			message = "Must be valid Base64$alphabetNote$paddingNote.",
			metadata = constraint,
		)
	}

	/**
	 * Whether [value] decodes under the requested alphabet and padding rule.
	 *
	 * Pads the payload itself when [requirePadding] is `false`, so unpadded input still decodes.
	 * Side effects: none.
	 *
	 * @param value Candidate Base64 text; empty input always fails.
	 * @param urlSafe When `true`, use the URL and filename safe alphabet (`-` and `_`).
	 * @param requirePadding When `true`, the length must already be a multiple of four.
	 * @return `true` when the payload decodes without error.
	 */
	private fun isValid(
		value: String,
		urlSafe: Boolean,
		requirePadding: Boolean,
	): Boolean {
		if (value.isEmpty()) return false
		if (value.length % 4 == 1) return false
		if (requirePadding && value.length % 4 != 0) return false
		val payload = if (requirePadding) {
			value
		} else {
			value + "=".repeat((4 - value.length % 4) % 4)
		}

		return try {
			val decoder = if (urlSafe) Base64.getUrlDecoder() else Base64.getDecoder()
			decoder.decode(payload)
			true
		} catch (_: IllegalArgumentException) {
			false
		}
	}
}
