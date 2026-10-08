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
package io.ghaylan.validata.constraint.validator.string.email

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.EmailConstraint
import io.ghaylan.validata.constraint.validator.string.PatternMatcherReuse
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator.pattern
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Checks email syntax (not deliverability) so obviously-invalid addresses never enter the domain.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). The pattern is compiled once at class load;
 * matching reuses a thread-local matcher via [PatternMatcherReuse] so the success path does not allocate
 * a [Matcher] per call.
 *
 * Error: [ConstraintErrorCode.VALUE_FORMAT_INVALID].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object EmailValidator : ConstraintValidator<CharSequence, EmailConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: EmailConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_FORMAT_INVALID)
	
	/**
	 * Built-in email syntax [Pattern], compiled once at class load.
	 */
	private val pattern = Pattern.compile("^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{1,6})$")
	
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
		constraint: EmailConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (isValid(value)) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
			message = "Must be a valid email address.",
			metadata = constraint,
		)
	}
	
	/**
	 * Whether [value] matches the built-in email syntax pattern.
	 *
	 * Side effects: none.
	 *
	 * @param value Non-null text under test.
	 * @return `true` when [pattern] matches.	 
	 */
	fun isValid(value: CharSequence): Boolean = PatternMatcherReuse.matches(pattern, value)
}
