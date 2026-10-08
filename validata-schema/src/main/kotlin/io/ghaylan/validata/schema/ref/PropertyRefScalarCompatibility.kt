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

import io.ghaylan.validata.schema.shape.ScalarKind

/**
 * Pure [ScalarKind] compatibility matrix for cross-field `@PropertyRef` checks (KSP and IntelliJ plugin).
 *
 * [PropertyRefCompatibilityKind.COMPARABLE_FAMILY] (`@Compare`) accepts any orderable scalar:
 * numerics mix with numerics, temporals with temporals, and `STRING` / `CHAR` / `ENUM` / `UUID`
 * only with their own kind. Boolean and opaque subjects are rejected outright.
 *
 * Not a value comparator or path resolver — see `PropertyPath` in the parent package.*
 * 
 * @author Ghaylan Saada
 */
object PropertyRefScalarCompatibility {
	
	/**
	 * Whether [annotatedKind] and [referencedKind] are compatible under [kind].
	 *
	 * No side effects.
	 *
	 * @param kind rule from metadata `@PropertyRef.compatibility`
	 * @param annotatedKind [ScalarKind] name of the property carrying the constraint
	 * @param referencedKind [ScalarKind] name at the end of the referenced path
	 * @return `true` when compatible, or when [kind] is [PropertyRefCompatibilityKind.NONE]	 
	 */
	fun isCompatible(
		kind: PropertyRefCompatibilityKind,
		annotatedKind: String,
		referencedKind: String,
	): Boolean = when (kind) {
		PropertyRefCompatibilityKind.NONE -> true
		PropertyRefCompatibilityKind.COMPARABLE_FAMILY -> sameComparableFamily(annotatedKind, referencedKind)
		PropertyRefCompatibilityKind.SAME_SCALAR_KIND -> sameScalarKind(annotatedKind, referencedKind)
	}
	
	/**
	 * Typed overload — prefers [ScalarKind] when both sides already have IR kinds.
	 *
	 * No side effects.
	 *
	 * @param kind rule from metadata `@PropertyRef.compatibility`
	 * @param annotatedKind kind of the property carrying the constraint
	 * @param referencedKind kind at the end of the referenced path
	 * @return same result as the string overload of [isCompatible]	 
	 */
	fun isCompatible(
		kind: PropertyRefCompatibilityKind,
		annotatedKind: ScalarKind,
		referencedKind: ScalarKind,
	): Boolean = isCompatible(kind, annotatedKind.name, referencedKind.name)
	
	/**
	 * Whether the annotated subject may carry a constraint with [kind] (before checking the referenced path).
	 *
	 * No side effects.
	 *
	 * @param kind rule from metadata `@PropertyRef.compatibility`
	 * @param subjectKind [ScalarKind] name of the annotated property
	 * @return `true` when the subject family is legal for [kind]	 
	 */
	fun isSubjectCompatible(
		kind: PropertyRefCompatibilityKind,
		subjectKind: String,
	): Boolean = when (kind) {
		PropertyRefCompatibilityKind.NONE -> true
		PropertyRefCompatibilityKind.COMPARABLE_FAMILY -> isOrderable(subjectKind)
		PropertyRefCompatibilityKind.SAME_SCALAR_KIND -> subjectKind != ScalarKind.OTHER.name
	}
	
	/**
	 * Typed overload for [isSubjectCompatible].
	 *
	 * No side effects.
	 *
	 * @param kind rule from metadata `@PropertyRef.compatibility`
	 * @param subjectKind kind of the annotated property
	 * @return same result as the string overload of [isSubjectCompatible]	 
	 */
	fun isSubjectCompatible(
		kind: PropertyRefCompatibilityKind,
		subjectKind: ScalarKind,
	): Boolean = isSubjectCompatible(kind, subjectKind.name)
	
	/**
	 * Human-readable reason when [isCompatible] is false — for KSP / IDE diagnostics.
	 *
	 * No side effects.
	 *
	 * @param kind rule that failed
	 * @param annotatedKind subject kind name
	 * @param referencedKind referenced kind name
	 * @return empty string for [PropertyRefCompatibilityKind.NONE] (never a mismatch)	 
	 */
	fun mismatchMessage(
		kind: PropertyRefCompatibilityKind,
		annotatedKind: String,
		referencedKind: String,
	): String = when (kind) {
		PropertyRefCompatibilityKind.NONE -> ""
		PropertyRefCompatibilityKind.SAME_SCALAR_KIND -> "$annotatedKind cannot equal $referencedKind — use the same scalar kind (or both numeric)"
		PropertyRefCompatibilityKind.COMPARABLE_FAMILY -> "$annotatedKind cannot compare with $referencedKind — both must be numeric, both temporal, or the same scalar kind"
	}
	
	/**
	 * Typed overload — prefers [ScalarKind] when both sides already have IR kinds.
	 */
	fun mismatchMessage(
		kind: PropertyRefCompatibilityKind,
		annotatedKind: ScalarKind,
		referencedKind: ScalarKind,
	): String = mismatchMessage(kind, annotatedKind.name, referencedKind.name)
	
	/**
	 * Message when [isSubjectCompatible] is false.
	 *
	 * No side effects.
	 *
	 * @param annotationSimpleName annotation simple name for the diagnostic (e.g. `"Compare"`)
	 * @param kind rule that failed
	 * @param subjectKind subject kind name
	 * @return empty string for [PropertyRefCompatibilityKind.NONE]	 
	 */
	fun subjectMismatchMessage(
		annotationSimpleName: String,
		kind: PropertyRefCompatibilityKind,
		subjectKind: String,
	): String = when (kind) {
		PropertyRefCompatibilityKind.NONE -> ""
		PropertyRefCompatibilityKind.COMPARABLE_FAMILY -> "@$annotationSimpleName cannot apply to $subjectKind — use an orderable scalar property (not boolean or an opaque type)"
		PropertyRefCompatibilityKind.SAME_SCALAR_KIND -> "@$annotationSimpleName cannot apply to $subjectKind — use a concrete scalar Comparable type"
	}
	
	/**
	 * Typed overload for [subjectMismatchMessage].
	 */
	fun subjectMismatchMessage(
		annotationSimpleName: String,
		kind: PropertyRefCompatibilityKind,
		subjectKind: ScalarKind,
	): String = subjectMismatchMessage(annotationSimpleName, kind, subjectKind.name)
	
	/**
	 * Whether both kinds share a comparable family for `@Compare`.
	 *
	 * Numeric kinds mix freely (an `Int` compares with a `BigDecimal`) and so do temporals. Every
	 * other orderable leaf — `STRING`, `CHAR`, `ENUM`, `UUID` — only compares with its own kind,
	 * since their orderings are unrelated. [ScalarKind.BOOLEAN] and [ScalarKind.OTHER] are never
	 * comparable.
	 *
	 * No side effects.
	 *
	 * @param a first kind name
	 * @param b second kind name
	 * @return `true` when both are numeric, both temporal, or the same orderable kind	 
	 */
	private fun sameComparableFamily(
		a: String,
		b: String
	): Boolean {
		if (isNumeric(a) && isNumeric(b)) return true
		if (!isOrderable(a) || !isOrderable(b)) return false
		return a == b
	}
	
	/**
	 * Whether kinds match for `@Compare` (identical or both numeric).
	 *
	 * No side effects.
	 *
	 * @param a first kind name
	 * @param b second kind name
	 * @return `true` when kinds match or both are numeric	 
	 */
	private fun sameScalarKind(
		a: String,
		b: String
	): Boolean {
		if (a == b) return true
		if (isNumeric(a) && isNumeric(b)) return true
		return false
	}
	
	/**
	 * Whether [kind] is a numeric [ScalarKind] name.
	 *
	 * No side effects.
	 *
	 * @param kind scalar kind name
	 * @return `true` for [ScalarKind.INTEGRAL] or [ScalarKind.DECIMAL]	 
	 */
	private fun isNumeric(kind: String): Boolean = kind == ScalarKind.INTEGRAL.name || kind == ScalarKind.DECIMAL.name
	
	/**
	 * Whether [kind] has a meaningful natural ordering for `@Compare`.
	 *
	 * Everything except [ScalarKind.BOOLEAN] (two values, no useful ordering) and
	 * [ScalarKind.OTHER] (not provably `Comparable`) qualifies.
	 *
	 * No side effects.
	 *
	 * @param kind scalar kind name
	 * @return `true` when the kind can carry or be referenced by an ordering comparison	 
	 */
	private fun isOrderable(kind: String): Boolean =
		kind != ScalarKind.BOOLEAN.name && kind != ScalarKind.OTHER.name
}
