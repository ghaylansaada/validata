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
import com.google.devtools.ksp.symbol.Variance
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames
import io.ghaylan.validata.schema.types.TypeTables

/**
 * Normalized type view for ranking — FQCN + args; no KSP dependency at test time.
 *
 * Companion to [ValidatorCompatibility]. Tests build values directly; production uses [from].
 *
 * Type-family membership for FQCNs comes from schema [KnownTypes] / [TypeNames]. Hierarchy
 * bits that need [assignableSupertypes] (map/collection soft-fit) live here.
 *
 * @property qualifiedName Erased type FQCN (`kotlin.String`, `kotlin.collections.List`, …).
 * @property typeArguments Resolved type arguments; wildcards use [WILDCARD].
 * @property isArray `true` for `Array` / primitive arrays.
 * @property arrayElement Component type when [isArray]; otherwise null.
 * @property isWildcard Star / `*` / unresolved catch-all arg.
 * @property assignableSupertypes Supertype FQCNs for rank-4 checks (excludes self); KSP-filled or test-set.
 *
 * @author Ghaylan Saada
 */
internal data class TypeView(
	val qualifiedName: String,
	val typeArguments: List<TypeView> = emptyList(),
	val isArray: Boolean = false,
	val arrayElement: TypeView? = null,
	val isWildcard: Boolean = false,
	val assignableSupertypes: Set<String> = emptySet(),
) {
	/**
	 * Exact concrete match ignoring wildcards: same canonical FQCN after [KnownTypes.canonicalize].
	 */
	fun sameConcrete(other: TypeView): Boolean =
		!isWildcard && !other.isWildcard &&
			KnownTypes.canonicalize(qualifiedName) == KnownTypes.canonicalize(other.qualifiedName)

	/**
	 * Whether this type is map-shaped for validator rank tier 5 (uses assignable supers from KSP).
	 */
	fun isMapLike(): Boolean {
		val c = KnownTypes.canonicalize(qualifiedName)
		return c == TypeNames.MAP_KOTLIN ||
			c == TypeNames.MUTABLE_MAP_KOTLIN ||
			qualifiedName.startsWith(TypeNames.MAP_JAVA) ||
			TypeNames.MAP_KOTLIN in assignableSupertypes ||
			TypeNames.MAP_JAVA in assignableSupertypes
	}

	/**
	 * Whether this type is collection-shaped (not array) for validator rank tier 8.
	 */
	fun isCollectionLike(): Boolean {
		if (isArray) return false
		val c = KnownTypes.canonicalize(qualifiedName)
		return c in TypeTables.COLLECTION_FQCNS ||
			qualifiedName.startsWith(TypeNames.LIST_JAVA) ||
			qualifiedName.startsWith(TypeNames.SET_JAVA) ||
			qualifiedName.startsWith(TypeNames.COLLECTION_JAVA) ||
			TypeNames.COLLECTION_KOTLIN in assignableSupertypes ||
			TypeNames.COLLECTION_JAVA in assignableSupertypes
	}

	/**
	 * Whether a value of [other] is assignable where this type is expected (KSP `isAssignableFrom` sense).
	 *
	 * Uses [assignableSupertypes] plus platform hierarchies (CharSequence, Number, Comparable, Temporal)
	 * so unit tests work without a full hierarchy walk.
	 */
	fun isAssignableFrom(other: TypeView): Boolean {
		if (sameConcrete(other)) return true
		val need = KnownTypes.canonicalize(qualifiedName)
		if (other.assignableSupertypes.any { KnownTypes.canonicalize(it) == need }) return true
		if (KnownTypes.isCharSequence(need) && KnownTypes.isCharSequenceLike(other.qualifiedName)) return true
		if (KnownTypes.isNumber(need) && KnownTypes.isNumeric(other.qualifiedName)) return true
		if (KnownTypes.isComparable(need) &&
			(KnownTypes.isComparableNumeric(other.qualifiedName) || KnownTypes.isTemporal(other.qualifiedName))
		) {
			return true
		}
		if (KnownTypes.isTemporal(need) && KnownTypes.isTemporal(other.qualifiedName)) return true
		return false
	}

	companion object {
		/** Wildcard / star-projection placeholder. */
		val WILDCARD: TypeView = TypeView(qualifiedName = "*", isWildcard = true)

		/**
		 * Builds a [TypeView] from [type], stripping nullability and recording supertype FQCNs.
		 */
		fun from(type: KSType): TypeView {
			val raw = type.makeNotNullable()
			val q = raw.declaration.qualifiedName?.asString() ?: return WILDCARD
			if (q == TypeNames.ANY_KOTLIN || q == TypeNames.OBJECT_JAVA) {
				return TypeView(q, assignableSupertypes = emptySet())
			}
			val supers = TypeClassification.assignableSupertypes(raw)

			if (TypeClassification.isArray(raw)) {
				val elem = arrayElementOf(raw)
				return TypeView(
					qualifiedName = q,
					isArray = true,
					arrayElement = elem,
					assignableSupertypes = supers)
			}

			val args = raw.arguments.map { arg ->
				val typeRef = arg.type
				when {
					arg.variance == Variance.STAR || typeRef == null -> WILDCARD
					else -> from(typeRef.resolve())
				}
			}
			return TypeView(
				qualifiedName = q,
				typeArguments = args,
				assignableSupertypes = supers,
			)
		}

		private fun arrayElementOf(arrayType: KSType): TypeView? {
			val q = arrayType.declaration.qualifiedName?.asString() ?: return null
			if (q == TypeNames.ARRAY_KOTLIN) {
				return arrayType.arguments.firstOrNull()?.type?.resolve()?.let { from(it) }
			}
			KnownTypes.primitiveArrayElementFqcn(q)?.let { elemFqcn ->
				return TypeView(elemFqcn)
			}
			return arrayType.arguments.firstOrNull()?.type?.resolve()?.let { from(it) }
		}
	}
}
