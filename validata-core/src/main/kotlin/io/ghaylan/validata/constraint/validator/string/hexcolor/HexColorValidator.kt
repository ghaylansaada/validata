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
package io.ghaylan.validata.constraint.validator.string.hexcolor

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.HexColorConstraint
import io.ghaylan.validata.constraint.validator.string.hexcolor.HexColorValidator.pattern
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.regex.Pattern

/**
 * Accepts `#RGB` / `#RRGGBB` hex literals for UI color fields.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). The pattern is compiled once at class load.
 *
 * Error: [ConstraintErrorCode.VALUE_FORMAT_INVALID].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object HexColorValidator : ConstraintValidator<CharSequence, HexColorConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: HexColorConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_FORMAT_INVALID)
	
	/**
	 * `#RGB` / `#RRGGBB` syntax [Pattern], compiled once at class load.
	 */
	private val pattern = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
	
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
		constraint: HexColorConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (isValid(value)) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
			message = "Must be a valid hex color (#RGB or #RRGGBB).",
			metadata = constraint,
		)
	}
	
	/**
	 * Whether [value] matches `#RGB` or `#RRGGBB` hex color syntax.
	 *
	 * Side effects: none.
	 *
	 * @param value Non-null text under test.
	 * @return `true` when [pattern] matches.	 
	 */
	fun isValid(value: CharSequence): Boolean = pattern.matcher(value)
		.matches()
}
