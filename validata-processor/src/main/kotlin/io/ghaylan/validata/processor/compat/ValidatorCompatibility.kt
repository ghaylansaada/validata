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
package io.ghaylan.validata.processor.compat

import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Compile-time ranking of how well a validator's value type `V` fits a property's type.
 *
 * **This is the only validator-selection algorithm in the framework. Do not add a second one.**
 *
 * Operates on [TypeView] (pure) or [KSType] (via [TypeView.from]).
 *
 * Lower rank wins; `null` means incompatible. When two candidates share a rank, callers must break
 * ties by validator FQCN (lexicographic) so selection never depends on `Set`/`Map` iteration order.
 *
 * ## Rank tiers
 * | Rank | Meaning |
 * |---|---|
 * | 0 | Exact concrete type (+ matching type args) |
 * | 1 | Primitive ↔ boxed pair |
 * | 2 | Numeric ↔ `Number` widening |
 * | 3 | Comparable-numeric ↔ `Comparable` |
 * | 4 | Validator type is assignable from the value type |
 * | 5 | Map-like value vs map-like validator with all-wildcard args |
 * | 6 | Array-of-scalars (elem exact/boxed or numeric↔Number) |
 * | 7 | Array value vs `Array<Any>` / `Any` element, **or** array vs `Cloneable` carrier |
 * | 8 | Collection-like vs collection-like with all-wildcard args |
 * | 9 | Validator `V` is `Any` / `Object` (catch-all) |
 *
 * @author Ghaylan Saada
 */
internal object ValidatorCompatibility {

	/**
	 * How well [validatorValueType] (`V` from `ConstraintValidator<V, C>`) fits [actualValueType].
	 */
	fun scoreFit(actualValueType: KSType, validatorValueType: KSType): Int? =
		scoreFit(TypeView.from(actualValueType), TypeView.from(validatorValueType))

	/**
	 * Pure ranking over [TypeView]s — unit-tested without a KSP round.
	 */
	fun scoreFit(value: TypeView, validator: TypeView): Int? {
		// Arrays store the element in [TypeView.arrayElement], not typeArguments — do not treat
		// two `kotlin.Array` erasures with empty args as an exact match (that would skip ranks 6–7).
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
			// fall through to array / Any tiers below
		} else if (value.sameConcrete(validator) && typeArgsMatch(value.typeArguments, validator.typeArguments)) {
			return 0
		}
		if (!value.isArray && !validator.isArray &&
			KnownTypes.primitiveOrBoxedMatch(value.qualifiedName, validator.qualifiedName) &&
			typeArgsMatch(value.typeArguments, validator.typeArguments)
		) {
			return 1
		}
		if (KnownTypes.isNumeric(value.qualifiedName) && KnownTypes.isNumber(validator.qualifiedName)) return 2
		if (KnownTypes.isNumber(value.qualifiedName) && KnownTypes.isNumeric(validator.qualifiedName)) return 2
		if (KnownTypes.isComparableNumeric(value.qualifiedName) && KnownTypes.isComparable(validator.qualifiedName)) {
			return 3
		}
		if (!value.isArray && !validator.isArray &&
			!KnownTypes.isAny(validator.qualifiedName) &&
			!(KnownTypes.isCloneable(validator.qualifiedName) &&
				KnownTypes.isCharSequenceLike(value.qualifiedName)) &&
			validator.isAssignableFrom(value) &&
			typeArgsMatch(value.typeArguments, validator.typeArguments)
		) {
			return 4
		}
		if (value.isMapLike() && validator.isMapLike() &&
			validator.typeArguments.isNotEmpty() &&
			validator.typeArguments.all { it.isWildcard }
		) {
			return 5
		}
		if (value.isArray && validator.isArray) {
			val ve = value.arrayElement
			val va = validator.arrayElement
			if (ve != null && va != null) {
				if (KnownTypes.primitiveOrBoxedMatch(ve.qualifiedName, va.qualifiedName) ||
					ve.sameConcrete(va)
				) {
					return 6
				}
				if (KnownTypes.isNumeric(ve.qualifiedName) && KnownTypes.isNumber(va.qualifiedName)) return 6
			}
			if (va != null && KnownTypes.isAny(va.qualifiedName)) return 7
			return null
		}
		// Array subjects vs Cloneable — carrier type for ArraySizeValidator (primitive + Array<*>).
		if (value.isArray && KnownTypes.isCloneable(validator.qualifiedName)) return 7
		// Collection<*> carriers soft-fit any collection subject. Empty type-args cover
		// unresolved/raw Collection V (same as ArraySizeValidator’s Cloneable carrier).
		if (value.isCollectionLike() && validator.isCollectionLike() &&
			(isCollectionCarrier(validator.qualifiedName) ||
				validator.typeArguments.isEmpty() ||
				validator.typeArguments.all { it.isWildcard })
		) {
			return 8
		}
		if (KnownTypes.isAny(validator.qualifiedName)) return 9
		return null
	}

	private fun isCollectionCarrier(q: String): Boolean {
		val c = KnownTypes.canonicalize(q)
		return c == TypeNames.COLLECTION_KOTLIN ||
			c == TypeNames.MUTABLE_COLLECTION_KOTLIN ||
			c.startsWith(TypeNames.COLLECTION_JAVA)
	}

	/**
	 * Recursive type-argument matching (port of `typeArgsMatch`).
	 */
	fun typeArgsMatch(actual: List<TypeView>, expected: List<TypeView>): Boolean {
		if (expected.isEmpty()) return true
		if (actual.isEmpty()) return expected.all { it.isWildcard }
		if (actual.size != expected.size) return false
		return actual.zip(expected).all { (act, exp) ->
			exp.isWildcard ||
				(act.sameConcrete(exp) && typeArgsMatch(act.typeArguments, exp.typeArguments))
		}
	}
}
