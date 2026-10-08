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
package io.ghaylan.validata.schema.types

/**
 * Pure FQCN classification and normalization shared by tooling and the runtime.
 *
 * Hosts that already have a fully qualified name (from KSP, PSI, or `Class.getName`) call these
 * helpers — they never walk type hierarchies themselves. Hierarchy walks stay in each host.
 *
 * @author Ghaylan Saada
 */
object KnownTypes {

	/**
	 * Maps a possibly-Java FQCN to its Kotlin canonical form when known.
	 *
	 * @param q Raw fully qualified type name.
	 * @return Canonical Kotlin FQCN, or [q] when no mapping exists.
	 */
	fun canonicalize(q: String): String =
		TypeTables.JAVA_LANG_TO_KOTLIN[q] ?: q

	/**
	 * Whether [a] and [b] denote the same scalar after primitive/boxed normalization.
	 */
	fun primitiveOrBoxedMatch(a: String, b: String): Boolean {
		val ca = canonicalize(a)
		val cb = canonicalize(b)
		if (ca == cb) return true
		val boxed = TypeTables.PRIMITIVE_TO_BOXED
		return boxed[ca] == b ||
			boxed[cb] == a ||
			canonicalize(boxed[ca] ?: "") == cb ||
			canonicalize(boxed[cb] ?: "") == ca
	}

	fun isNumeric(q: String): Boolean =
		canonicalize(q) in TypeTables.NUMERIC_FQCNS

	fun isNumber(q: String): Boolean =
		canonicalize(q) == TypeNames.NUMBER_KOTLIN

	fun isComparable(q: String): Boolean =
		canonicalize(q) == TypeNames.COMPARABLE_KOTLIN

	fun isComparableNumeric(q: String): Boolean =
		isNumeric(q) && canonicalize(q) != TypeNames.NUMBER_KOTLIN

	fun isAny(q: String): Boolean =
		canonicalize(q) == TypeNames.ANY_KOTLIN

	fun isCloneable(q: String): Boolean =
		q == TypeNames.CLONEABLE_KOTLIN || q == TypeNames.CLONEABLE_JAVA

	fun isCharSequence(q: String): Boolean =
		canonicalize(q) == TypeNames.CHAR_SEQUENCE_KOTLIN

	fun isString(q: String): Boolean =
		canonicalize(q) == TypeNames.STRING_KOTLIN

	fun isCharSequenceLike(q: String): Boolean {
		val c = canonicalize(q)
		return c == TypeNames.STRING_KOTLIN || c == TypeNames.CHAR_SEQUENCE_KOTLIN
	}

	/**
	 * Whether [q] is a `java.time.temporal.Temporal` implementor used for validator ranking.
	 *
	 * Narrower than [ScalarKinds] temporal leaves: [TypeNames.DURATION], [TypeNames.MONTH],
	 * [TypeNames.PERIOD], etc. are literal hosts / scalar leaves but not Temporal-assignable
	 * peers for ranking (see [TypeTables.TEMPORAL_IMPLEMENTORS]).
	 */
	fun isTemporal(q: String): Boolean {
		val c = canonicalize(q)
		return c == TypeNames.TEMPORAL || c in TypeTables.TEMPORAL_IMPLEMENTORS
	}

	fun isTemporalLiteralHost(q: String): Boolean =
		q in TypeTables.TEMPORAL_LITERAL_HOSTS

	fun isMapFqcn(q: String): Boolean =
		q in TypeTables.MAP_FQCNS || q.startsWith(TypeNames.MAP_JAVA)

	fun isCollectionFqcn(q: String): Boolean =
		canonicalize(q) in TypeTables.COLLECTION_FQCNS ||
			q.startsWith(TypeNames.LIST_JAVA) ||
			q.startsWith(TypeNames.SET_JAVA) ||
			q.startsWith(TypeNames.COLLECTION_JAVA)

	fun isArrayFqcn(q: String?): Boolean {
		if (q == null) return false
		if (q == TypeNames.ARRAY_KOTLIN) return true
		return primitiveArrayElementFqcn(q) != null
	}

	fun primitiveArrayElementFqcn(arrayFqcn: String): String? =
		TypeTables.PRIMITIVE_ARRAY_ELEMENT[arrayFqcn]

	/**
	 * Builtin assignable supers for [q] when hosts cannot walk a real hierarchy
	 * (light fixtures / unresolved PSI). Empty for unknown types.
	 */
	fun builtinSupertypes(q: String): Set<String> =
		TypeTables.BUILTIN_ASSIGNABLE_SUPERTYPES[canonicalize(q)].orEmpty()

	/**
	 * JVM erased name for [kotlinFqcn] as [Class.getTypeName] would report it.
	 *
	 * @param kotlinFqcn Kotlin declaration FQCN.
	 * @param notNull Whether the use site is non-null (primitive vs boxed for scalars).
	 * @return JVM erased name, or `null` when [kotlinFqcn] has no known mapping.
	 */
	fun jvmErasedName(kotlinFqcn: String, notNull: Boolean): String? {
		TypeTables.PRIMITIVE_ARRAY_JVM[kotlinFqcn]?.let { return it }
		TypeTables.COLLECTION_JVM[kotlinFqcn]?.let { return it }
		if (kotlinFqcn == TypeNames.STRING_KOTLIN) return TypeNames.STRING_JAVA
		if (kotlinFqcn == TypeNames.UNIT_KOTLIN) return TypeNames.UNIT_KOTLIN
		val boxed = TypeTables.PRIMITIVE_TO_BOXED[kotlinFqcn] ?: return null
		val primitive = TypeTables.KOTLIN_PRIMITIVE_JVM[kotlinFqcn] ?: return boxed
		return if (notNull) primitive else boxed
	}

	/**
	 * Whether [packageName] is under a platform leaf package (never a traversable DTO).
	 */
	fun isLeafPackage(packageName: String): Boolean =
		TypeTables.LEAF_PACKAGE_PREFIXES.any {
			packageName == it.dropLast(1) || packageName.startsWith(it)
		}

	/** Sorted distinct JVM-loadable FQCNs referenced by contract tests. */
	val ALL_JVM_LOADABLE: List<String> = buildList {
		addAll(TypeTables.JAVA_LANG_TO_KOTLIN.keys)
		addAll(TypeTables.PRIMITIVE_TO_BOXED.values)
		addAll(TypeTables.NUMERIC_FQCNS.filter { it.startsWith(TypeNames.JAVA_PACKAGE_PREFIX) })
		add(TypeNames.CLONEABLE_JAVA)
		add(TypeNames.TEMPORAL)
		addAll(TypeTables.TEMPORAL_IMPLEMENTORS)
		addAll(TypeTables.TEMPORAL_LITERAL_HOSTS)
		add(TypeNames.MAP_JAVA)
		add(TypeNames.LIST_JAVA)
		add(TypeNames.SET_JAVA)
		add(TypeNames.COLLECTION_JAVA)
		add(TypeNames.UUID)
		add(TypeNames.DATE)
		add(TypeNames.CALENDAR)
		add(TypeNames.URI)
		add(TypeNames.DAY_OF_WEEK)
		add(TypeNames.SERIALIZABLE_JAVA)
	}.distinct().sorted()

	/** Sorted distinct Kotlin FQCNs referenced by contract tests. */
	val ALL_KOTLIN_FQCN: List<String> = buildList {
		addAll(TypeTables.JAVA_LANG_TO_KOTLIN.values)
		addAll(TypeTables.PRIMITIVE_TO_BOXED.keys)
		addAll(TypeTables.NUMERIC_FQCNS.filter { it.startsWith(TypeNames.KOTLIN_PACKAGE_PREFIX) })
		addAll(TypeTables.COLLECTION_FQCNS)
		add(TypeNames.MAP_KOTLIN)
		add(TypeNames.MUTABLE_MAP_KOTLIN)
		add(TypeNames.ITERABLE_KOTLIN)
		add(TypeNames.MUTABLE_ITERABLE_KOTLIN)
		add(TypeNames.CLONEABLE_KOTLIN)
		add(TypeNames.ARRAY_KOTLIN)
		add(TypeNames.KCLASS)
		add(TypeNames.UNIT_KOTLIN)
		add(TypeNames.CONTINUATION_KOTLIN)
		addAll(TypeTables.PRIMITIVE_ARRAY_ELEMENT.keys)
	}.distinct().sorted()
}
