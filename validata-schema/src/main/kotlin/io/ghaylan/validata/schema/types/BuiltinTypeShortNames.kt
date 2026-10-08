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
 * Short-name → FQCN map when PSI / KSP resolve is unavailable (light fixtures, unresolved refs).
 *
 * Derived from [TypeNames] so short-name fallbacks cannot drift from the FQCN catalog.
 *
 * @author Ghaylan Saada
 */
object BuiltinTypeShortNames {

	/** Full map for validator ranking and general subject typing. */
	val ALL: Map<String, String> = mapOf(
		"Boolean" to TypeNames.BOOLEAN_KOTLIN,
		"Char" to TypeNames.CHAR_KOTLIN,
		"String" to TypeNames.STRING_KOTLIN,
		"Int" to TypeNames.INT_KOTLIN,
		"Long" to TypeNames.LONG_KOTLIN,
		"Short" to TypeNames.SHORT_KOTLIN,
		"Byte" to TypeNames.BYTE_KOTLIN,
		"Float" to TypeNames.FLOAT_KOTLIN,
		"Double" to TypeNames.DOUBLE_KOTLIN,
		"Number" to TypeNames.NUMBER_KOTLIN,
		"Any" to TypeNames.ANY_KOTLIN,
		"Object" to TypeNames.ANY_KOTLIN,
		"Integer" to TypeNames.INT_KOTLIN,
		"CharSequence" to TypeNames.CHAR_SEQUENCE_KOTLIN,
		"Comparable" to TypeNames.COMPARABLE_KOTLIN,
		"Cloneable" to TypeNames.CLONEABLE_JAVA,
		"BigDecimal" to TypeNames.BIG_DECIMAL,
		"BigInteger" to TypeNames.BIG_INTEGER,
		"List" to TypeNames.LIST_KOTLIN,
		"MutableList" to TypeNames.MUTABLE_LIST_KOTLIN,
		"Set" to TypeNames.SET_KOTLIN,
		"MutableSet" to TypeNames.MUTABLE_SET_KOTLIN,
		"Collection" to TypeNames.COLLECTION_KOTLIN,
		"java.lang.Collection" to TypeNames.COLLECTION_JAVA,
		"Map" to TypeNames.MAP_KOTLIN,
		"MutableMap" to TypeNames.MUTABLE_MAP_KOTLIN,
		"Array" to TypeNames.ARRAY_KOTLIN,
		"Iterable" to TypeNames.ITERABLE_KOTLIN,
		"MutableIterable" to TypeNames.MUTABLE_ITERABLE_KOTLIN,
		"UUID" to TypeNames.UUID,
		"URI" to TypeNames.URI,
		"LocalDate" to TypeNames.LOCAL_DATE,
		"LocalDateTime" to TypeNames.LOCAL_DATE_TIME,
		"LocalTime" to TypeNames.LOCAL_TIME,
		"Instant" to TypeNames.INSTANT,
		"OffsetDateTime" to TypeNames.OFFSET_DATE_TIME,
		"OffsetTime" to TypeNames.OFFSET_TIME,
		"ZonedDateTime" to TypeNames.ZONED_DATE_TIME,
		"Year" to TypeNames.YEAR,
		"YearMonth" to TypeNames.YEAR_MONTH,
		"MonthDay" to TypeNames.MONTH_DAY,
		"Month" to TypeNames.MONTH,
		"DayOfWeek" to TypeNames.DAY_OF_WEEK,
		"Duration" to TypeNames.DURATION,
		"Period" to TypeNames.PERIOD,
		"Date" to TypeNames.DATE,
		"Calendar" to TypeNames.CALENDAR,
		"Temporal" to TypeNames.TEMPORAL,
	)

	/** Leaf scalars / temporals for property-ref kind classification (subset of [ALL]). */
	val SCALAR_LEAVES: Map<String, String> = mapOf(
		"Boolean" to TypeNames.BOOLEAN_KOTLIN,
		"Char" to TypeNames.CHAR_KOTLIN,
		"String" to TypeNames.STRING_KOTLIN,
		"Int" to TypeNames.INT_KOTLIN,
		"Long" to TypeNames.LONG_KOTLIN,
		"Short" to TypeNames.SHORT_KOTLIN,
		"Byte" to TypeNames.BYTE_KOTLIN,
		"Float" to TypeNames.FLOAT_KOTLIN,
		"Double" to TypeNames.DOUBLE_KOTLIN,
		"BigDecimal" to TypeNames.BIG_DECIMAL,
		"BigInteger" to TypeNames.BIG_INTEGER,
		"UUID" to TypeNames.UUID,
		"LocalDate" to TypeNames.LOCAL_DATE,
		"LocalDateTime" to TypeNames.LOCAL_DATE_TIME,
		"LocalTime" to TypeNames.LOCAL_TIME,
		"Instant" to TypeNames.INSTANT,
		"OffsetDateTime" to TypeNames.OFFSET_DATE_TIME,
		"ZonedDateTime" to TypeNames.ZONED_DATE_TIME,
		"Date" to TypeNames.DATE,
		"Calendar" to TypeNames.CALENDAR,
	)

	/**
	 * Short names treated as collection / array carriers whose first type argument is the element
	 * (ELEMENT-scoped `@PropertyRef` / `@Distinct`). Includes common concrete JDK aliases.
	 */
	val COLLECTION_OR_ARRAY_SHORT_NAMES: Set<String> = setOf(
		"List",
		"MutableList",
		"Set",
		"MutableSet",
		"Collection",
		"MutableCollection",
		"Iterable",
		"MutableIterable",
		"ArrayList",
		"HashSet",
		"LinkedHashSet",
		"Array",
	)
}
