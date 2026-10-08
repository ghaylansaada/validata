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
package io.ghaylan.validata.constraint.validator.string.url

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Url
import io.ghaylan.validata.constraint.annotation.UrlConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.net.URI
import java.net.URISyntaxException

/**
 * Validates URL text ([CharSequence]) using structural policy from [UrlConstraint] metadata.
 *
 * Checks stop at the first failure:
 * length → URI syntax → host ([Url.Type.WEBSITE] only) → protocol → port → query → extension.
 *
 * Unlike most format validators, **blank** (as well as `null`) skips — presence is `@Required`.
 *
 * Errors: [ConstraintErrorCode.TEXT_TOO_LONG], [ConstraintErrorCode.VALUE_FORMAT_INVALID],
 * [ConstraintErrorCode.VALUE_INVALID], [ConstraintErrorCode.VALUE_NOT_ALLOWED].
 * Failures attach the [UrlConstraint] on [ConstraintError.metadata].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object UrlValidator : ConstraintValidator<CharSequence, UrlConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint].
	 *
	 * Side effects: none.
	 *
	 * @param constraint URL policy metadata.
	 * @param type Subject runtime type (unused for URL).
	 * @return Codes [UrlSupport.possibleErrorCodes] reports (including format for parse failures).
	 */
	override fun possibleErrorCodes(
		constraint: UrlConstraint,
		type: Class<*>,
	) = UrlSupport.possibleErrorCodes(constraint, includeFormatInvalid = true)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null and blank values are accepted (presence is enforced by `@Required`).
	 *
	 * Side effects: may build and cache a policy for [constraint] via [UrlSupport].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached when applicable, or `null` when
	 *   valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: UrlConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (value.isBlank()) return null
		val rawUrl = if (value is String) value else value.toString()

		val uri = try {
			URI(rawUrl)
		} catch (_: URISyntaxException) {
			return ConstraintError(
				code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
				message = "Must be a valid URL.",
				metadata = constraint,
			)
		}

		return UrlSupport.validateParsed(rawUrl, uri, constraint)
	}
}
