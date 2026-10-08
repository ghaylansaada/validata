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
package io.ghaylan.validata.constraint.validator.string.regex

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.RegexConstraint
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.constraint.validator.string.PatternMatcherReuse
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Matches text against a configured [RegexConstraint.pattern] for domain-specific formats
 * without a dedicated validator class.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Compiled [Pattern]s are memoized per constraint
 * instance — `Pattern.compile` is expensive and the pattern string is fixed for the life of
 * the generated metadata.
 *
 * ## Safety bounds
 *
 * User-supplied patterns and long inputs are ReDoS vectors. Before matching, this validator
 * rejects values longer than [MAX_INPUT_LENGTH] with [ConstraintErrorCode.TEXT_PATTERN_MISMATCH]
 * (fail closed — never hang the request thread). Matching reuses a thread-local [Matcher]
 * via [PatternMatcherReuse] so the success path does not allocate a matcher per call.
 *
 * Error: [ConstraintErrorCode.TEXT_PATTERN_MISMATCH]. Messages prefer [RegexConstraint.name]
 * over echoing a potentially huge pattern. Failures attach the [RegexConstraint].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared instance
 * without Spring wiring; override via a `@Component` of the same type when an application needs
 * different behavior.
 *
 * @author Ghaylan Saada
 */
object RegexValidator : ConstraintValidator<CharSequence, RegexConstraint>() {

	/**
	 * Maximum character length accepted before pattern matching.
	 *
	 * Longer inputs fail immediately with [ConstraintErrorCode.TEXT_PATTERN_MISMATCH] so a
	 * catastrophic backtracking pattern cannot pin a request thread on megabyte-sized payloads.
	 */
	const val MAX_INPUT_LENGTH: Int = 10_000

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: RegexConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
	)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: may compile and cache a [Pattern] for [constraint] via [ConstraintLiteralCache].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached, or `null` when valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: RegexConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val formatName = constraint.name

		if (value.length > MAX_INPUT_LENGTH) {
			return ConstraintError(
				code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
				message = "Must be at most $MAX_INPUT_LENGTH characters long to be checked against the '$formatName' format.",
				metadata = constraint,
			)
		}

		// Cache by constraint instance: one Pattern.compile per annotation site, not per request.
		val pattern = ConstraintLiteralCache.getOrParse(constraint) {
			runCatching { Pattern.compile(constraint.pattern) }.getOrNull()
		} ?: return ConstraintError(
			code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			message = "Cannot be checked: the '$formatName' format pattern is not a valid regular expression.",
			metadata = constraint,
		)

		if (PatternMatcherReuse.matches(pattern, value)) return null

		return ConstraintError(
			code = ConstraintErrorCode.TEXT_PATTERN_MISMATCH,
			message = "Must match the '$formatName' format pattern.",
			metadata = constraint,
		)
	}
}
