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
package io.ghaylan.validata.constraint.validator.string.filepath

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.FilePathConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates relative file paths: rejects null bytes, blank values, `..` segments,
 * Windows-forbidden characters, and absolute / drive-prefixed / UNC paths.
 *
 * When [FilePathConstraint.requireExtension] is set, the last segment must also carry a non-empty
 * extension after its last `.`, with at least one character before the dot so dotfiles such as
 * `.gitignore` do not count as extensions.
 *
 * Null subjects are skipped by the engine (presence is `@Required`).
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID], [ConstraintErrorCode.VALUE_NOT_ALLOWED].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object FilePathValidator : ConstraintValidator<CharSequence, FilePathConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: FilePathConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_FORMAT_INVALID,
		ConstraintErrorCode.VALUE_NOT_ALLOWED)

	/**
	 * Characters forbidden in a single Windows-compatible path segment.
	 */
	private val invalidSegmentChars = charArrayOf('<', '>', ':', '"', '|', '?', '*')

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: FilePathConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val raw = value.toString()
		if (raw.indexOf('\u0000') >= 0) {
			return formatError(constraint)
		}
		val trimmed = raw.trim()
		if (trimmed.isEmpty()) {
			return formatError(constraint)
		}

		if (isAbsoluteOrDrivePrefixed(trimmed)) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
				message = "File name must not be an absolute path.",
				metadata = constraint,
			)
		}

		val segments = splitSegments(trimmed)
		for (segment in segments) {
			if (segment == "..") {
				return formatError(constraint)
			}
			if (segment.indexOfAny(invalidSegmentChars) >= 0) {
				return formatError(constraint)
			}
		}

		if (constraint.requireExtension && !hasExtension(segments.lastOrNull())) {
			return formatError(constraint)
		}

		return null
	}

	/**
	 * Whether [segment] names a file with a non-empty extension.
	 *
	 * The last `.` must sit after at least one character (so `.gitignore` is a dotfile, not an
	 * extension) and be followed by at least one character.
	 *
	 * @param segment Final path segment, or `null` when the path has none.
	 * @return `true` when an extension is present.
	 */
	private fun hasExtension(segment: String?): Boolean {
		if (segment.isNullOrEmpty()) return false
		val dot = segment.lastIndexOf('.')
		return dot > 0 && dot < segment.length - 1
	}

	/**
	 * Splits [path] on `/` and `\` into non-empty segments.
	 */
	private fun splitSegments(path: String): List<String> {
		val segments = mutableListOf<String>()
		var start = 0
		for (i in path.indices) {
			val ch = path[i]
			if (ch == '/' || ch == '\\') {
				if (i > start) {
					segments += path.substring(start, i)
				}
				start = i + 1
			}
		}
		if (start < path.length) {
			segments += path.substring(start)
		}
		return segments
	}

	/**
	 * Whether [path] is absolute Unix, UNC, or drive-letter prefixed.
	 */
	private fun isAbsoluteOrDrivePrefixed(path: String): Boolean {
		if (path.startsWith("/")) return true
		if (path.startsWith("\\\\")) return true
		if (path.length >= 2 && path[1] == ':' && path[0].isLetter()) return true
		return false
	}

	private fun formatError(
		constraint: FilePathConstraint,
	): ConstraintError<*> = ConstraintError(
		code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
		message = "Must be a valid relative file path: no null bytes, blank segments, '..' segments, or reserved characters.",
		metadata = constraint,
	)
}
