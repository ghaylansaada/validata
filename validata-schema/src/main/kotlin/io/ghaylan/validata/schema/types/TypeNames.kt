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
 * Fully qualified type names shared by KSP, the IntelliJ plugin, and the runtime.
 *
 * This is the single source of truth for leaf / collection / temporal spelling.
 * Hosts extract an FQCN from `KSType`, PSI, or `Class`, then call [KnownTypes] / [ScalarKinds].
 *
 * @author Ghaylan Saada
 */
object TypeNames {

	const val JAVA_PACKAGE_PREFIX: String = "java."
	const val JAVAX_PACKAGE_PREFIX: String = "javax."
	const val JAKARTA_PACKAGE_PREFIX: String = "jakarta."
	const val KOTLIN_PACKAGE_PREFIX: String = "kotlin."
	const val KOTLIN_REFLECT_PACKAGE_PREFIX: String = "kotlin.reflect."
	const val KOTLINX_PACKAGE_PREFIX: String = "kotlinx."
	const val JAVA_LANG_PACKAGE_PREFIX: String = "java.lang."
	const val JAVA_LANG_ANNOTATION_PACKAGE_PREFIX: String = "java.lang.annotation."
	const val KOTLIN_ANNOTATION_PACKAGE_PREFIX: String = "kotlin.annotation."
	const val JAVA_TIME_PACKAGE_PREFIX: String = "java.time."

	const val STRING_KOTLIN: String = "kotlin.String"
	const val STRING_JAVA: String = "java.lang.String"
	const val ARRAY_KOTLIN: String = "kotlin.Array"
	const val SET_KOTLIN: String = "kotlin.collections.Set"
	const val MUTABLE_SET_KOTLIN: String = "kotlin.collections.MutableSet"
	const val SET_JAVA: String = "java.util.Set"
	const val KCLASS: String = "kotlin.reflect.KClass"
	const val ANY_KOTLIN: String = "kotlin.Any"
	const val OBJECT_JAVA: String = "java.lang.Object"
	const val UNIT_KOTLIN: String = "kotlin.Unit"
	const val NUMBER_KOTLIN: String = "kotlin.Number"
	const val NUMBER_JAVA: String = "java.lang.Number"
	const val COMPARABLE_KOTLIN: String = "kotlin.Comparable"
	const val COMPARABLE_JAVA: String = "java.lang.Comparable"
	const val CHAR_SEQUENCE_KOTLIN: String = "kotlin.CharSequence"
	const val CHAR_SEQUENCE_JAVA: String = "java.lang.CharSequence"
	const val CONTINUATION_KOTLIN: String = "kotlin.coroutines.Continuation"
	const val BIG_DECIMAL: String = "java.math.BigDecimal"
	const val BIG_INTEGER: String = "java.math.BigInteger"
	const val UUID: String = "java.util.UUID"
	const val DATE: String = "java.util.Date"
	const val CALENDAR: String = "java.util.Calendar"
	const val URI: String = "java.net.URI"
	const val CLONEABLE_KOTLIN: String = "kotlin.Cloneable"
	const val CLONEABLE_JAVA: String = "java.lang.Cloneable"
	const val TEMPORAL: String = "java.time.temporal.Temporal"

	const val BOOLEAN_KOTLIN: String = "kotlin.Boolean"
	const val CHAR_KOTLIN: String = "kotlin.Char"
	const val BYTE_KOTLIN: String = "kotlin.Byte"
	const val SHORT_KOTLIN: String = "kotlin.Short"
	const val INT_KOTLIN: String = "kotlin.Int"
	const val LONG_KOTLIN: String = "kotlin.Long"
	const val FLOAT_KOTLIN: String = "kotlin.Float"
	const val DOUBLE_KOTLIN: String = "kotlin.Double"

	const val BOOLEAN_JAVA: String = "java.lang.Boolean"
	const val CHARACTER_JAVA: String = "java.lang.Character"
	const val BYTE_JAVA: String = "java.lang.Byte"
	const val SHORT_JAVA: String = "java.lang.Short"
	const val INTEGER_JAVA: String = "java.lang.Integer"
	const val LONG_JAVA: String = "java.lang.Long"
	const val FLOAT_JAVA: String = "java.lang.Float"
	const val DOUBLE_JAVA: String = "java.lang.Double"

	const val LIST_KOTLIN: String = "kotlin.collections.List"
	const val MUTABLE_LIST_KOTLIN: String = "kotlin.collections.MutableList"
	const val COLLECTION_KOTLIN: String = "kotlin.collections.Collection"
	const val MUTABLE_COLLECTION_KOTLIN: String = "kotlin.collections.MutableCollection"
	const val ITERABLE_KOTLIN: String = "kotlin.collections.Iterable"
	const val MUTABLE_ITERABLE_KOTLIN: String = "kotlin.collections.MutableIterable"
	const val MAP_KOTLIN: String = "kotlin.collections.Map"
	const val MUTABLE_MAP_KOTLIN: String = "kotlin.collections.MutableMap"
	const val LIST_JAVA: String = "java.util.List"
	const val COLLECTION_JAVA: String = "java.util.Collection"
	const val MAP_JAVA: String = "java.util.Map"
	const val SERIALIZABLE_JAVA: String = "java.io.Serializable"

	const val INT_ARRAY: String = "kotlin.IntArray"
	const val LONG_ARRAY: String = "kotlin.LongArray"
	const val SHORT_ARRAY: String = "kotlin.ShortArray"
	const val BYTE_ARRAY: String = "kotlin.ByteArray"
	const val FLOAT_ARRAY: String = "kotlin.FloatArray"
	const val DOUBLE_ARRAY: String = "kotlin.DoubleArray"
	const val BOOLEAN_ARRAY: String = "kotlin.BooleanArray"
	const val CHAR_ARRAY: String = "kotlin.CharArray"

	const val LOCAL_DATE: String = "java.time.LocalDate"
	const val LOCAL_TIME: String = "java.time.LocalTime"
	const val LOCAL_DATE_TIME: String = "java.time.LocalDateTime"
	const val OFFSET_TIME: String = "java.time.OffsetTime"
	const val OFFSET_DATE_TIME: String = "java.time.OffsetDateTime"
	const val ZONED_DATE_TIME: String = "java.time.ZonedDateTime"
	const val INSTANT: String = "java.time.Instant"
	const val YEAR: String = "java.time.Year"
	const val MONTH: String = "java.time.Month"
	const val DAY_OF_WEEK: String = "java.time.DayOfWeek"
	const val DURATION: String = "java.time.Duration"
	const val PERIOD: String = "java.time.Period"
	const val YEAR_MONTH: String = "java.time.YearMonth"
	const val MONTH_DAY: String = "java.time.MonthDay"
	const val ZONE_OFFSET: String = "java.time.ZoneOffset"
}
