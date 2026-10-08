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

package io.ghaylan.validata.intellij.analysis.compat

import io.ghaylan.validata.intellij.typing.ValidatorTypeView
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames
import io.ghaylan.validata.schema.types.TypeTables

/**
 * IDE twin of KSP `ValidatorCompatibility.scoreFit` — ranks how well a validator’s `V`
 * (`ConstraintValidator<V, C>`) fits the annotated subject type.
 *
 * **What.** Pure ranking over [ValidatorTypeView] pairs. Lower rank wins among compatible
 * validators; `null` means incompatible (subject must not use that validator).
 *
 * **Why.** When several validators are registered for one constraint annotation, the IDE
 * (and KSP) must pick / accept the same fits so “no applicable validator” and best-match
 * messaging stay consistent.
 *
 * **How it fits.** Called after [SubjectTypeViews] builds subject and validator `V` views.
 * [anyFits] is the boolean gate used by inspections; [scoreFit] is the full matrix.
 * Helpers ([canonicalize], [isNumeric], …) are shared with [sameConcrete] /
 * [isAssignableFrom] and [SubjectTypeViews].
 *
 * **Not.** Not PSI resolve. Not scalar-kind `@PropertyRef` compatibility — that is
 * schema `PropertyRefScalarCompatibility`. Does not instantiate validators or read configs.
 *
 * **KSP / runtime parity.** Keep aligned with
 * `validata-processor/.../ValidatorCompatibility.kt` (validator fit is processor-owned, not
 * schema IR). Rank meanings:
 * - `0` exact concrete (+ type args)
 * - `1` primitive / boxed match
 * - `2` numeric ↔ `Number`
 * - `3` comparable-numeric ↔ `Comparable`
 * - `4` assignable (non-`Any`) with matching args
 * - `5` map-like with all-wildcard validator args
 * - `6` array element soft match
 * - `7` array ↔ `Any` element or `Cloneable` carrier
 * - `8` collection-like with all-wildcard validator args
 * - `9` validator `V` is `Any`
 * - `null` incompatible*
 * 
 * @author Ghaylan Saada
 */
internal object ValidatorTypeCompatibility {

	/**
	 * Whether both sides are concrete (non-wildcard) and share the same canonical FQCN.
	 */
	fun ValidatorTypeView.sameConcrete(other: ValidatorTypeView): Boolean =
		!isWildcard && !other.isWildcard &&
			canonicalize(qualifiedName) == canonicalize(other.qualifiedName)

	/**
	 * Coarse assignability: could a value of [other] be passed where `this` is expected?
	 */
	fun ValidatorTypeView.isAssignableFrom(other: ValidatorTypeView): Boolean {
		if (sameConcrete(other)) return true
		val need = canonicalize(qualifiedName)
		if (need in other.assignableSupertypes.map { canonicalize(it) }) {
			return true
		}
		if (isCharSequence(need) && isCharSequenceLike(other.qualifiedName)) {
			return true
		}
		if (isNumber(need) && isNumeric(other.qualifiedName)) {
			return true
		}
		if (isComparable(need) &&
			(isComparableNumeric(other.qualifiedName) || isTemporal(other.qualifiedName))
		) {
			return true
		}
		if (isTemporal(need) && isTemporal(other.qualifiedName)) {
			return true
		}
		if (isCollectionCarrier(need) && isCollectionLike(other)) {
			return true
		}
		return false
	}

	/**
	 * How well [validator] (`V`) fits [value] (subject).
	 *
	 * Evaluates rules in priority order; returns the first matching rank. Array vs non-array
	 * mismatches fall through until family / `Any` / `Cloneable` rules or `null`.
	 *
	 * @param value annotated subject type view
	 * @param validator constraint validator’s `V` type view
	 * @return rank `0..9` (lower is better), or `null` when incompatible
	 */
	fun scoreFit(value: ValidatorTypeView, validator: ValidatorTypeView): Int? {
		if (value.isArray || validator.isArray) {
			if (value.isArray && validator.isArray) {
				val ve = value.arrayElement
				val va = validator.arrayElement
				if (ve != null && va != null &&
					ve.sameConcrete(va) &&
					typeArgsMatch(ve.typeArguments, va.typeArguments)
				) {
					return 0
				}
			}
		} else if (value.sameConcrete(validator) && typeArgsMatch(value.typeArguments, validator.typeArguments)) {
			return 0
		}
		if (!value.isArray && !validator.isArray &&
			primitiveOrBoxedMatch(value.qualifiedName, validator.qualifiedName) &&
			typeArgsMatch(value.typeArguments, validator.typeArguments)
		) {
			return 1
		}
		if (isNumeric(value.qualifiedName) && isNumber(validator.qualifiedName)) return 2
		if (isNumber(value.qualifiedName) && isNumeric(validator.qualifiedName)) return 2
		if (isComparableNumeric(value.qualifiedName) && isComparable(validator.qualifiedName)) return 3
		if (!value.isArray && !validator.isArray &&
			!isAny(validator.qualifiedName) &&
			!(isCloneable(validator.qualifiedName) && isCharSequenceLike(value.qualifiedName)) &&
			validator.isAssignableFrom(value) &&
			typeArgsMatch(value.typeArguments, validator.typeArguments)
		) {
			return 4
		}
		if (isMapLike(value) && isMapLike(validator) &&
			validator.typeArguments.isNotEmpty() &&
			validator.typeArguments.all { it.isWildcard }
		) {
			return 5
		}
		if (value.isArray && validator.isArray) {
			val ve = value.arrayElement
			val va = validator.arrayElement
			if (ve != null && va != null) {
				if (primitiveOrBoxedMatch(ve.qualifiedName, va.qualifiedName) || ve.sameConcrete(va)) {
					return 6
				}
				if (isNumeric(ve.qualifiedName) && isNumber(va.qualifiedName)) return 6
			}
			if (va != null && isAny(va.qualifiedName)) return 7
			return null
		}
		// Keep aligned with processor ValidatorCompatibility — array vs Cloneable carrier.
		if (value.isArray && isCloneable(validator.qualifiedName)) return 7
		// Collection<*> / Collection carriers soft-fit any collection subject (List/Set/…).
		// Empty args cover light-test stubs where `*` does not resolve as a wildcard.
		if (isCollectionLike(value) && isCollectionLike(validator) &&
			(isCollectionCarrier(canonicalize(validator.qualifiedName)) ||
				validator.typeArguments.isEmpty() ||
				validator.typeArguments.all { it.isWildcard })
		) {
			return 8
		}
		if (isAny(validator.qualifiedName)) return 9
		return null
	}

	/**
	 * Whether at least one validator `V` in [validators] accepts [subject].
	 *
	 * @param subject annotated subject type
	 * @param validators candidate `V` views for a constraint
	 * @return `true` when [scoreFit] is non-null for any pair; `false` when [validators] is
	 *   empty or every candidate is incompatible
	 */
	fun anyFits(subject: ValidatorTypeView, validators: List<ValidatorTypeView>): Boolean =
		validators.any { scoreFit(subject, it) != null }

	/**
	 * Whether [actual] type arguments satisfy [expected] (validator) arguments.
	 *
	 * Empty [expected] always matches (validator declared no args to check). Empty [actual]
	 * matches only when every expected arg is a wildcard. Sizes must otherwise equal; each pair
	 * matches if expected is wildcard or both are same-concrete with recursive arg match.
	 *
	 * @param actual subject-side type arguments
	 * @param expected validator-side type arguments
	 * @return `true` when args are compatible under the rules above; `false` on size mismatch
	 *   or a concrete expected arg that does not match
	 */
	fun typeArgsMatch(actual: List<ValidatorTypeView>, expected: List<ValidatorTypeView>): Boolean {
		if (expected.isEmpty()) return true
		if (actual.isEmpty()) return expected.all { it.isWildcard }
		if (actual.size != expected.size) return false
		return actual.zip(expected).all { (act, exp) ->
			exp.isWildcard ||
				(act.sameConcrete(exp) && typeArgsMatch(act.typeArguments, exp.typeArguments))
		}
	}


	/**
	 * Maps common Java boxed / JDK names onto Kotlin stdlib FQCNs — schema [KnownTypes].
	 */
	internal fun canonicalize(q: String): String =
		KnownTypes.canonicalize(q)

	/**
	 * Whether [a] and [b] name the same primitive / boxed pair — schema [KnownTypes].
	 */
	internal fun primitiveOrBoxedMatch(a: String, b: String): Boolean =
		KnownTypes.primitiveOrBoxedMatch(a, b)

	/** Whether [q] is in the numeric family — schema [KnownTypes]. */
	internal fun isNumeric(q: String): Boolean =
		KnownTypes.isNumeric(q)

	/** Whether [q] is exactly `kotlin.Number` — schema [KnownTypes]. */
	internal fun isNumber(q: String): Boolean =
		KnownTypes.isNumber(q)

	/** Whether [q] is `Cloneable` — schema [KnownTypes]. */
	internal fun isCloneable(q: String): Boolean =
		KnownTypes.isCloneable(q)

	/** Whether [q] is exactly `kotlin.Comparable` — schema [KnownTypes]. */
	internal fun isComparable(q: String): Boolean =
		KnownTypes.isComparable(q)

	/** Numeric types excluding the abstract `Number` carrier — schema [KnownTypes]. */
	internal fun isComparableNumeric(q: String): Boolean =
		KnownTypes.isComparableNumeric(q)

	/** Whether [q] is `kotlin.Any` — schema [KnownTypes]. */
	internal fun isAny(q: String): Boolean =
		KnownTypes.isAny(q)

	/** Whether [q] is exactly `kotlin.CharSequence` — schema [KnownTypes]. */
	internal fun isCharSequence(q: String): Boolean =
		KnownTypes.isCharSequence(q)

	/** Whether [q] is String or CharSequence — schema [KnownTypes]. */
	internal fun isCharSequenceLike(q: String): Boolean =
		KnownTypes.isCharSequenceLike(q)

	/**
	 * Whether [q] is a Temporal implementor used for validator ranking — schema [KnownTypes]
	 * (narrower than every `java.time.*` name).
	 */
	internal fun isTemporal(q: String): Boolean =
		KnownTypes.isTemporal(q)

	/**
	 * Whether [t] looks like a Map for wildcard-arg soft fit (rank 5).
	 */
	internal fun isMapLike(t: ValidatorTypeView): Boolean {
		val c = KnownTypes.canonicalize(t.qualifiedName)
		return c == TypeNames.MAP_KOTLIN ||
			c == TypeNames.MUTABLE_MAP_KOTLIN ||
			t.qualifiedName.startsWith(TypeNames.MAP_JAVA) ||
			TypeNames.MAP_KOTLIN in t.assignableSupertypes ||
			TypeNames.MAP_JAVA in t.assignableSupertypes
	}

	private fun isCollectionCarrier(q: String): Boolean {
		val c = KnownTypes.canonicalize(q)
		return c == TypeNames.COLLECTION_KOTLIN ||
			c == TypeNames.MUTABLE_COLLECTION_KOTLIN ||
			c.startsWith(TypeNames.COLLECTION_JAVA)
	}

	/**
	 * Whether [t] looks like a Collection / List / Set for wildcard-arg soft fit (rank 8).
	 */
	internal fun isCollectionLike(t: ValidatorTypeView): Boolean {
		if (t.isArray) return false
		val c = KnownTypes.canonicalize(t.qualifiedName)
		return c in TypeTables.COLLECTION_FQCNS ||
			t.qualifiedName.startsWith(TypeNames.LIST_JAVA) ||
			t.qualifiedName.startsWith(TypeNames.SET_JAVA) ||
			t.qualifiedName.startsWith(TypeNames.COLLECTION_JAVA) ||
			TypeNames.COLLECTION_KOTLIN in t.assignableSupertypes ||
			TypeNames.COLLECTION_JAVA in t.assignableSupertypes
	}
}
