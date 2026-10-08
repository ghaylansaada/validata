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
package io.ghaylan.validata.constraint.validator.string.isolanguage

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.IsoLanguageConstraint
import io.ghaylan.validata.constraint.validator.string.isolanguage.IsoLanguageValidator.fetchAllLanguages
import io.ghaylan.validata.constraint.validator.string.isolanguage.IsoLanguageValidator.validLanguageTags
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.util.*

/**
 * Accepts BCP 47 language tags in `xx` or `xx-YY` form so locale fields stay machine-readable.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Supported tags are built once from
 * [Locale.getAvailableLocales] and cached in a lazy [Set] for O(1) lookups.
 *
 * Error: [ConstraintErrorCode.VALUE_FORMAT_INVALID].
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object IsoLanguageValidator : ConstraintValidator<CharSequence, IsoLanguageConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: IsoLanguageConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_FORMAT_INVALID)
	
	/**
	 * Allowed BCP 47 tags (`xx` / `xx-YY`), populated once on first use via [fetchAllLanguages].
	 */
	private val validLanguageTags by lazy(LazyThreadSafetyMode.PUBLICATION) { fetchAllLanguages() }
	
	/**
	 * Lowercase language subtag with an optional uppercase region subtag (`en`, `en-US`).
	 */
	private val structuralFormat = Regex("^[a-z]{2}(-[A-Z]{2})?$")
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: first call may populate [validLanguageTags] from [Locale.getAvailableLocales].
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation, or `null` when valid.	 
	 */
	override fun validate(
		value: CharSequence,
		constraint: IsoLanguageConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val tag = if (value is String) value else value.toString()
		if (validLanguageTags.contains(tag)) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
			message = "Must be a valid BCP 47 language tag.",
			metadata = constraint,
		)
	}
	
	/**
	 * Builds the allowed-tag set from [Locale.getAvailableLocales], keeping only `xx` / `xx-YY`.
	 *
	 * Side effects: none beyond reading JVM locales.
	 *
	 * @return Immutable [Set] of language tags for membership checks.	 
	 */
	private fun fetchAllLanguages(): Set<String> {
		return Locale.getAvailableLocales()
			.map(Locale::toLanguageTag)
			.filter(structuralFormat::matches)
			.toSet()
	}
}
