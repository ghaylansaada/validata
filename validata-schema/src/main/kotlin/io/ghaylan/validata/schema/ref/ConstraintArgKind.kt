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
package io.ghaylan.validata.schema.ref

/**
 * Tooling rule(s) that apply to a constraint-metadata constructor argument.
 *
 * Authors declare kinds via `ConstraintArg` (in **validata-core**). KSP and the IntelliJ plugin
 * discover kinds by reflecting the metadata class — not by hard-coding annotation simple names.
 * Enum entry names are part of the discovery contract (e.g. `ConstraintArgKind.NOT_BLANK`), the
 * same way as [PropertyRefCompatibilityKind].
 *
 * Multiple kinds may appear on one parameter; interpreters apply every kind and the first failure
 * wins for diagnostics.
 * 
 * @author Ghaylan Saada
 */
enum class ConstraintArgKind {
	
	/**
	 * Reject blank/empty strings when [ConstraintArgTarget.VALUE].
	 *
	 * For collection arguments, prefer [ConstraintArgTarget.ELEMENT] so blank elements are
	 * rejected while an empty collection remains allowed unless [NON_EMPTY] is also present on
	 * [ConstraintArgTarget.VALUE] (e.g. `@Distinct(by = [])` is valid).
	 */
	NOT_BLANK,
	
	/**
	 * Reject empty collections / arrays, or empty/blank scalars.
	 *
	 * Use with [ConstraintArgTarget.VALUE] (e.g. `@DaysOfWeek(days = …)`, `@In(values = …)`).
	 */
	NON_EMPTY,
	
	/**
	 * Each string (or array element) must parse as the direct annotated subject type
	 * (number with underscore separators, temporal ISO forms, enum constant name, …).
	 *
	 * For `String`/`CharSequence` subjects this kind is a no-op beyond [NOT_BLANK] when both apply.
	 */
	TYPED_LITERAL,
	
	/**
	 * Numeric metadata argument (`Int` / `Long`) must be `>= 0` (e.g. `@Size.min`).
	 */
	NON_NEGATIVE,
	
	/**
	 * Numeric metadata argument must be `> 0` (e.g. `@MultipleOf.factor` after parsing).
	 */
	POSITIVE,
	
	/**
	 * String argument must compile as a Java `java.util.regex.Pattern` (e.g. `@Regex.pattern`).
	 */
	REGEX,
}
