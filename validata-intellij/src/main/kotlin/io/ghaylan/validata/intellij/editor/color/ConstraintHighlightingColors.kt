/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.editor.color

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * Editor colors for constraint annotation arguments in the Validata IntelliJ plugin.
 *
 * ## What
 * Central registry of [TextAttributesKey]s used by annotators to recolor string / numeric
 * constraint arguments so they no longer look like ordinary Kotlin string literals:
 *
 * - [PROPERTY_REF] — IDE **INSTANCE_FIELD** color for navigable property paths (`@Compare(ref = "…", operation = Compare.Operation.EQ)`)
 *   — same look as a real field/property in source
 * - [ENUM] — platform **CONSTANT** color for catalog names (`@In(["PHONE"])`,
 *   `@ApiError` codes, `Min("MARCH")` on `Month`) — same look as `ContactType.PHONE` in source
 * - [UNRESOLVED] — full red text only when a name cannot be resolved (missing property /
 *   unknown enum constant / invalid typed literal). Semantic issues on **resolved** refs
 *   (type mismatch, self-ref, nested path) use error underline and keep ref color.
 * - [NUMBER] — IDE **NUMBER** color for numeric typed strings (`@Min("5")`) — same as `= 100`
 * - [TEMPORAL] — amber for non-enum temporal strings (`@Min("2020-01-01")`, `"P1Y"`)
 *
 * ## Why / EP role
 * Keys are applied via silent / error annotations in PropertyRefAnnotator,
 * ConstraintLiteralAnnotator, ValidatableAnnotator (subtype name literals), and
 * ApiErrorCatalogAnnotator (`@ApiError` codes).
 * Default look-and-feel comes from `colorSchemes/GhaylanValidation*.xml`, registered in
 * `plugin.xml` as `additionalTextAttributes` for Default and Darcula schemes — this object does
 * **not** register an EP itself; it only defines the key identities annotators share.
 *
 * ## What it is NOT
 * - Not a highlighter / lexer — Validata does not ship a custom language highlighter for
 *   constraint args; annotators paint ranges after PSI analysis.
 * - `@Regex(pattern)` uses RegExp language injection (`ConstraintRegexLanguageInjector`),
 *   not these keys, for multi-token regex coloring.
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintHighlightingColors {
	
	/**
	 * IDE instance-field color for navigable property names inside constraint strings
	 * (`@Compare(ref = "confirmSecret", operation = Compare.Operation.EQ)`) — same token as a real field/property in Kotlin source.
	 *
	 * Not overridden in Validata color schemes so the active IDE theme’s field color wins.
	 * Fallback base: [DefaultLanguageHighlighterColors.INSTANCE_FIELD].
	 */
	val PROPERTY_REF: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
		"GHAYLAN_CONSTRAINT_PROPERTY_REF",
		DefaultLanguageHighlighterColors.INSTANCE_FIELD,
	)
	
	/**
	 * Red / wrong-reference styling for unresolved property segments and invalid constraint
	 * literals (blank required strings, bad typed literals, empty non-empty collections, …).
	 *
	 * Fallback base: [CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES].
	 */
	val UNRESOLVED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
		"GHAYLAN_CONSTRAINT_UNRESOLVED",
		CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES,
	)
	
	/**
	 * IDE number color for decimal / integer string bounds (`"5"`, `"100.0"`, `"5.0"`) once
	 * validated — same token as a Kotlin numeric literal (`= 100`).
	 *
	 * Not overridden in Validata color schemes so the active IDE theme’s number color wins.
	 * Fallback base: [DefaultLanguageHighlighterColors.NUMBER].
	 */
	val NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
		"GHAYLAN_CONSTRAINT_NUMBER",
		DefaultLanguageHighlighterColors.NUMBER,
	)
	
	/**
	 * Amber for non-enum temporal string bounds (`"2020-01-01"`, `"P1Y"`, `"PT2H"`).
	 *
	 * Defaults come from `colorSchemes/GhaylanValidation*.xml` (not string-green / not teal).
	 * Fallback base: [DefaultLanguageHighlighterColors.KEYWORD] when scheme XML is absent.
	 */
	val TEMPORAL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
		"GHAYLAN_CONSTRAINT_TEMPORAL",
		DefaultLanguageHighlighterColors.KEYWORD,
	)
	
	/**
	 * Platform enum/constant color for enum and catalog name strings (`"MARCH"`, `"ADMIN"`,
	 * `@ApiError` codes) — matches how `SomeEnum.CONSTANT` looks in Kotlin source.
	 *
	 * Not overridden in Validata color schemes so the active IDE theme’s constant color wins.
	 * Fallback base: [DefaultLanguageHighlighterColors.CONSTANT].
	 */
	val ENUM: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
		"GHAYLAN_CONSTRAINT_ENUM",
		DefaultLanguageHighlighterColors.CONSTANT,
	)
}
