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
import io.ghaylan.validata.constraint.annotation.UrlConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.runtime.ValidationContext
import java.net.URI

/**
 * Validates [URI] subjects against [UrlConstraint] policy (same rules as [UrlValidator]).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Delegates to
 * [UrlSupport.validateParsed] after converting the URI to its string form.
 *
 * Errors: [ConstraintErrorCode.TEXT_TOO_LONG], [ConstraintErrorCode.VALUE_INVALID],
 * [ConstraintErrorCode.VALUE_NOT_ALLOWED]. Failures attach the [UrlConstraint].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object UriValidator : ConstraintValidator<URI, UrlConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint].
	 *
	 * Side effects: none.
	 *
	 * @param constraint URL policy metadata.
	 * @param type Subject runtime type (unused for URI).
	 * @return Codes [UrlSupport.possibleErrorCodes] reports for that policy (no parse format code).
	 */
	override fun possibleErrorCodes(
		constraint: UrlConstraint,
		type: Class<*>,
	) = UrlSupport.possibleErrorCodes(constraint, includeFormatInvalid = false)

	/**
	 * Validates [value] against [constraint] via [UrlSupport].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: may build and cache a policy for [constraint] via [UrlSupport].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached when applicable, or `null` when
	 *   valid.
	 */
	override fun validate(
		value: URI,
		constraint: UrlConstraint,
		context: ValidationContext,
	) = UrlSupport.validateParsed(value.toString(), value, constraint)
}
