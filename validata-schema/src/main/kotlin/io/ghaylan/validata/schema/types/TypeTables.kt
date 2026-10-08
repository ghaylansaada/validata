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
 * Lookup tables backing [KnownTypes] predicates and JVM name mapping.
 *
 * Public so hosts and contract tests can assert catalog contents without re-listing FQCNs.
 *
 * @author Ghaylan Saada
 */
object TypeTables {

	/** Maps common `java.lang.*` FQCNs to Kotlin stdlib equivalents. */
	val JAVA_LANG_TO_KOTLIN: Map<String, String> = mapOf(
		TypeNames.STRING_JAVA to TypeNames.STRING_KOTLIN,
		TypeNames.INTEGER_JAVA to TypeNames.INT_KOTLIN,
		TypeNames.LONG_JAVA to TypeNames.LONG_KOTLIN,
		TypeNames.SHORT_JAVA to TypeNames.SHORT_KOTLIN,
		TypeNames.BYTE_JAVA to TypeNames.BYTE_KOTLIN,
		TypeNames.FLOAT_JAVA to TypeNames.FLOAT_KOTLIN,
		TypeNames.DOUBLE_JAVA to TypeNames.DOUBLE_KOTLIN,
		TypeNames.BOOLEAN_JAVA to TypeNames.BOOLEAN_KOTLIN,
		TypeNames.CHARACTER_JAVA to TypeNames.CHAR_KOTLIN,
		TypeNames.NUMBER_JAVA to TypeNames.NUMBER_KOTLIN,
		TypeNames.OBJECT_JAVA to TypeNames.ANY_KOTLIN,
		TypeNames.CHAR_SEQUENCE_JAVA to TypeNames.CHAR_SEQUENCE_KOTLIN,
		TypeNames.COMPARABLE_JAVA to TypeNames.COMPARABLE_KOTLIN,
	)

	/** Maps Kotlin primitive FQCNs to boxed Java equivalents. */
	val PRIMITIVE_TO_BOXED: Map<String, String> = mapOf(
		TypeNames.INT_KOTLIN to TypeNames.INTEGER_JAVA,
		TypeNames.LONG_KOTLIN to TypeNames.LONG_JAVA,
		TypeNames.SHORT_KOTLIN to TypeNames.SHORT_JAVA,
		TypeNames.BYTE_KOTLIN to TypeNames.BYTE_JAVA,
		TypeNames.FLOAT_KOTLIN to TypeNames.FLOAT_JAVA,
		TypeNames.DOUBLE_KOTLIN to TypeNames.DOUBLE_JAVA,
		TypeNames.BOOLEAN_KOTLIN to TypeNames.BOOLEAN_JAVA,
		TypeNames.CHAR_KOTLIN to TypeNames.CHARACTER_JAVA,
	)

	/** Maps Kotlin primitive FQCNs to JVM primitive spellings (`int`, `long`, …). */
	val KOTLIN_PRIMITIVE_JVM: Map<String, String> = mapOf(
		TypeNames.INT_KOTLIN to "int",
		TypeNames.LONG_KOTLIN to "long",
		TypeNames.SHORT_KOTLIN to "short",
		TypeNames.BYTE_KOTLIN to "byte",
		TypeNames.FLOAT_KOTLIN to "float",
		TypeNames.DOUBLE_KOTLIN to "double",
		TypeNames.BOOLEAN_KOTLIN to "boolean",
		TypeNames.CHAR_KOTLIN to "char",
	)

	/** Maps Kotlin primitive array FQCNs to element scalar FQCNs. */
	val PRIMITIVE_ARRAY_ELEMENT: Map<String, String> = mapOf(
		TypeNames.INT_ARRAY to TypeNames.INT_KOTLIN,
		TypeNames.LONG_ARRAY to TypeNames.LONG_KOTLIN,
		TypeNames.SHORT_ARRAY to TypeNames.SHORT_KOTLIN,
		TypeNames.BYTE_ARRAY to TypeNames.BYTE_KOTLIN,
		TypeNames.FLOAT_ARRAY to TypeNames.FLOAT_KOTLIN,
		TypeNames.DOUBLE_ARRAY to TypeNames.DOUBLE_KOTLIN,
		TypeNames.BOOLEAN_ARRAY to TypeNames.BOOLEAN_KOTLIN,
		TypeNames.CHAR_ARRAY to TypeNames.CHAR_KOTLIN,
	)

	/** Maps Kotlin primitive array FQCNs to JVM array spellings (`int[]`, …). */
	val PRIMITIVE_ARRAY_JVM: Map<String, String> = mapOf(
		TypeNames.INT_ARRAY to "int[]",
		TypeNames.LONG_ARRAY to "long[]",
		TypeNames.SHORT_ARRAY to "short[]",
		TypeNames.BYTE_ARRAY to "byte[]",
		TypeNames.FLOAT_ARRAY to "float[]",
		TypeNames.DOUBLE_ARRAY to "double[]",
		TypeNames.BOOLEAN_ARRAY to "boolean[]",
		TypeNames.CHAR_ARRAY to "char[]",
	)

	/** Maps Kotlin collection/map FQCNs to erased Java interface FQCNs. */
	val COLLECTION_JVM: Map<String, String> = mapOf(
		TypeNames.LIST_KOTLIN to TypeNames.LIST_JAVA,
		TypeNames.MUTABLE_LIST_KOTLIN to TypeNames.LIST_JAVA,
		TypeNames.SET_KOTLIN to TypeNames.SET_JAVA,
		TypeNames.MUTABLE_SET_KOTLIN to TypeNames.SET_JAVA,
		TypeNames.COLLECTION_KOTLIN to TypeNames.COLLECTION_JAVA,
		TypeNames.MUTABLE_COLLECTION_KOTLIN to TypeNames.COLLECTION_JAVA,
		TypeNames.MAP_KOTLIN to TypeNames.MAP_JAVA,
		TypeNames.MUTABLE_MAP_KOTLIN to TypeNames.MAP_JAVA,
	)

	/** Scalar FQCNs treated as numeric for validator ranking / typed literals. */
	val NUMERIC_FQCNS: Set<String> = setOf(
		TypeNames.INT_KOTLIN,
		TypeNames.LONG_KOTLIN,
		TypeNames.SHORT_KOTLIN,
		TypeNames.BYTE_KOTLIN,
		TypeNames.FLOAT_KOTLIN,
		TypeNames.DOUBLE_KOTLIN,
		TypeNames.NUMBER_KOTLIN,
		TypeNames.BIG_DECIMAL,
		TypeNames.BIG_INTEGER,
	)

	/** Kotlin collection interface FQCNs (read-only and mutable). */
	val COLLECTION_FQCNS: Set<String> = setOf(
		TypeNames.LIST_KOTLIN,
		TypeNames.MUTABLE_LIST_KOTLIN,
		TypeNames.SET_KOTLIN,
		TypeNames.MUTABLE_SET_KOTLIN,
		TypeNames.COLLECTION_KOTLIN,
		TypeNames.MUTABLE_COLLECTION_KOTLIN,
	)

	/** Map interface FQCNs (read-only and mutable + Java). */
	val MAP_FQCNS: Set<String> = setOf(
		TypeNames.MAP_KOTLIN,
		TypeNames.MUTABLE_MAP_KOTLIN,
		TypeNames.MAP_JAVA,
	)

	/** Common `java.time` types implementing [TypeNames.TEMPORAL]. */
	val TEMPORAL_IMPLEMENTORS: Set<String> = setOf(
		TypeNames.INSTANT,
		TypeNames.LOCAL_DATE,
		TypeNames.LOCAL_DATE_TIME,
		TypeNames.LOCAL_TIME,
		TypeNames.OFFSET_DATE_TIME,
		TypeNames.OFFSET_TIME,
		TypeNames.ZONED_DATE_TIME,
		TypeNames.YEAR,
		TypeNames.ZONE_OFFSET,
	)

	/**
	 * Temporal types that accept string constraint arguments (ISO / type-specific `parse`).
	 *
	 * Shared by KSP and IntelliJ typed-literal checks.
	 */
	val TEMPORAL_LITERAL_HOSTS: Set<String> = setOf(
		TypeNames.LOCAL_DATE,
		TypeNames.LOCAL_TIME,
		TypeNames.LOCAL_DATE_TIME,
		TypeNames.OFFSET_TIME,
		TypeNames.OFFSET_DATE_TIME,
		TypeNames.ZONED_DATE_TIME,
		TypeNames.INSTANT,
		TypeNames.YEAR,
		TypeNames.MONTH,
		TypeNames.DURATION,
		TypeNames.PERIOD,
		TypeNames.YEAR_MONTH,
		TypeNames.MONTH_DAY,
	)

	/**
	 * Leaf FQCNs that map to a [io.ghaylan.validata.schema.shape.ScalarKind]
	 * (plus all `java.time.*` via prefix).
	 */
	val SCALAR_LEAF_FQCNS: Set<String> = setOf(
		TypeNames.BOOLEAN_KOTLIN,
		TypeNames.CHAR_KOTLIN,
		TypeNames.STRING_KOTLIN,
		TypeNames.INT_KOTLIN,
		TypeNames.LONG_KOTLIN,
		TypeNames.SHORT_KOTLIN,
		TypeNames.BYTE_KOTLIN,
		TypeNames.FLOAT_KOTLIN,
		TypeNames.DOUBLE_KOTLIN,
		TypeNames.BOOLEAN_JAVA,
		TypeNames.CHARACTER_JAVA,
		TypeNames.STRING_JAVA,
		TypeNames.INTEGER_JAVA,
		TypeNames.LONG_JAVA,
		TypeNames.SHORT_JAVA,
		TypeNames.BYTE_JAVA,
		TypeNames.FLOAT_JAVA,
		TypeNames.DOUBLE_JAVA,
		TypeNames.BIG_DECIMAL,
		TypeNames.BIG_INTEGER,
		TypeNames.UUID,
		TypeNames.DATE,
		TypeNames.CALENDAR,
	)

	/**
	 * Package prefixes whose types are always terminal values, never traversable DTOs.
	 *
	 * Used by runtime leaf policy and KSP platform-leaf guards.
	 */
	val LEAF_PACKAGE_PREFIXES: List<String> = listOf(
		TypeNames.JAVA_PACKAGE_PREFIX,
		TypeNames.JAVAX_PACKAGE_PREFIX,
		TypeNames.JAKARTA_PACKAGE_PREFIX,
		TypeNames.KOTLIN_PACKAGE_PREFIX,
		TypeNames.KOTLINX_PACKAGE_PREFIX,
		"sun.",
		"com.sun.",
		"jdk.",
		"org.w3c.",
		"org.xml.",
	)

	/**
	 * Lightweight assignable-supertype matrix for tooling when full hierarchy resolve is unavailable.
	 *
	 * Used by the IntelliJ plugin (and any host that builds type views without KSP/PSI supers)
	 * so `CharSequence` / `Number` / `Comparable` / `Temporal` validators still rank against
	 * concrete builtins. Keys and values are [TypeNames] FQCNs.
	 */
	val BUILTIN_ASSIGNABLE_SUPERTYPES: Map<String, Set<String>> = buildMap {
		val comparable = setOf(TypeNames.COMPARABLE_KOTLIN)
		val numberComparable = setOf(TypeNames.NUMBER_KOTLIN, TypeNames.COMPARABLE_KOTLIN)
		val temporalComparable = setOf(TypeNames.TEMPORAL, TypeNames.COMPARABLE_KOTLIN)
		val collectionIterable = setOf(TypeNames.COLLECTION_KOTLIN, TypeNames.ITERABLE_KOTLIN)

		put(
			TypeNames.STRING_KOTLIN,
			setOf(TypeNames.CHAR_SEQUENCE_KOTLIN, TypeNames.COMPARABLE_KOTLIN),
		)
		for (n in listOf(
			TypeNames.INT_KOTLIN,
			TypeNames.LONG_KOTLIN,
			TypeNames.SHORT_KOTLIN,
			TypeNames.BYTE_KOTLIN,
			TypeNames.FLOAT_KOTLIN,
			TypeNames.DOUBLE_KOTLIN,
		)) {
			put(n, numberComparable)
		}
		put(TypeNames.BIG_DECIMAL, setOf(TypeNames.NUMBER_KOTLIN, TypeNames.NUMBER_JAVA, TypeNames.COMPARABLE_KOTLIN))
		put(TypeNames.BIG_INTEGER, setOf(TypeNames.NUMBER_KOTLIN, TypeNames.NUMBER_JAVA, TypeNames.COMPARABLE_KOTLIN))
		put(TypeNames.BOOLEAN_KOTLIN, comparable)
		put(TypeNames.CHAR_KOTLIN, comparable)
		// Only true Temporal implementors get Temporal as a soft-super. Literal hosts that are
		// not Temporal-assignable (Month, Duration, Period) must not claim Temporal — otherwise
		// IDE ranking would accept Temporal validators for those subjects (production mismatch
		// with KSP / runtime assignability).
		for (t in TEMPORAL_IMPLEMENTORS) {
			put(t, temporalComparable)
		}
		for (t in TEMPORAL_LITERAL_HOSTS - TEMPORAL_IMPLEMENTORS) {
			put(t, comparable)
		}
		put(TypeNames.LIST_KOTLIN, collectionIterable)
		put(TypeNames.MUTABLE_LIST_KOTLIN, collectionIterable)
		put(TypeNames.SET_KOTLIN, collectionIterable)
		put(TypeNames.MUTABLE_SET_KOTLIN, collectionIterable)
		put(TypeNames.MUTABLE_MAP_KOTLIN, setOf(TypeNames.MAP_KOTLIN))
	}
}
