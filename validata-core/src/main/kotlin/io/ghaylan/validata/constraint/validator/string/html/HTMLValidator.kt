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
package io.ghaylan.validata.constraint.validator.string.html

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.HtmlConstraint
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.internal.OptionalDependencies
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates Html-ish text against allowed tags, attributes, and protocols (Jsoup optional dependency).
 *
 * Null and blank values skip (presence is `@Required`). Policy parsing and DOM checks
 * delegate to [HtmlJsoupSupport] (allow-lists memoized per [HtmlConstraint] instance).
 *
 * Errors: [ConstraintErrorCode.VALUE_NOT_ALLOWED],
 * [ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH]. Failures attach the [HtmlConstraint].
 * Messages never echo the submitted Html body.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object HtmlValidator : ConstraintValidator<CharSequence, HtmlConstraint>() {

	/**
	 * Error codes this validator may emit for [constraint] allow-lists.
	 *
	 * With the default `"*"` wildcards on every list, markup is unconstrained and neither code
	 * can fire. Restricting any dimension enables [ConstraintErrorCode.VALUE_NOT_ALLOWED]; the
	 * sanitization equality check (and [ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH]) only
	 * runs when at least one list is restricted.
	 */
	override fun possibleErrorCodes(
		constraint: HtmlConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> {
		val unrestricted =
			constraint.allowedTags.contains("*") &&
				constraint.allowedAttrs.contains("*") &&
				constraint.allowedProtocols.contains("*")
		if (unrestricted) return emptySet()
		return setOf(
			ConstraintErrorCode.VALUE_NOT_ALLOWED,
			ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH,
		)
	}

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null and blank values are accepted (presence is enforced by `@Required`).
	 *
	 * Side effects: [OptionalDependencies.requireJsoup] fails fast when Jsoup is absent;
	 * successful calls may populate [ConstraintLiteralCache] for the constraint site.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached when applicable, or `null` when
	 *   valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: HtmlConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (value.isBlank()) return null
		OptionalDependencies.requireJsoup()
		return HtmlJsoupSupport.validate(value.toString(), constraint)
	}
}
