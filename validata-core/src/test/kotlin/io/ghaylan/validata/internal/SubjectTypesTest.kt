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
package io.ghaylan.validata.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.util.UUID

/**
 * Runtime subject-type predicates used by constraint validators / error-code docs.
 *
 * @author Ghaylan Saada
 */
class SubjectTypesTest {

	@Test
	@DisplayName("box() maps every JVM primitive from the schema primitive table")
	fun boxAllPrimitives() {
		assertThat(SubjectTypes.box(Integer.TYPE)).isEqualTo(Integer::class.java)
		assertThat(SubjectTypes.box(java.lang.Long.TYPE)).isEqualTo(java.lang.Long::class.java)
		assertThat(SubjectTypes.box(java.lang.Boolean.TYPE)).isEqualTo(java.lang.Boolean::class.java)
		assertThat(SubjectTypes.box(Character.TYPE)).isEqualTo(Character::class.java)
		assertThat(SubjectTypes.box(java.lang.Short.TYPE)).isEqualTo(java.lang.Short::class.java)
		assertThat(SubjectTypes.box(java.lang.Byte.TYPE)).isEqualTo(java.lang.Byte::class.java)
		assertThat(SubjectTypes.box(java.lang.Float.TYPE)).isEqualTo(java.lang.Float::class.java)
		assertThat(SubjectTypes.box(java.lang.Double.TYPE)).isEqualTo(java.lang.Double::class.java)
		assertThat(SubjectTypes.box(String::class.java)).isEqualTo(String::class.java)
	}

	@Test
	@DisplayName("family predicates: number / temporal / charSequence / collection / map / unknown")
	fun familyPredicates() {
		assertThat(SubjectTypes.isNumber(Int::class.javaPrimitiveType!!)).isTrue()
		assertThat(SubjectTypes.isNumber(BigDecimal::class.java)).isTrue()
		assertThat(SubjectTypes.isNumber(String::class.java)).isFalse()
		assertThat(SubjectTypes.isStrictIntegralNumber(Int::class.java)).isTrue()
		assertThat(SubjectTypes.isStrictIntegralNumber(BigInteger::class.java)).isTrue()
		assertThat(SubjectTypes.isStrictIntegralNumber(BigDecimal::class.java)).isFalse()
		assertThat(SubjectTypes.isTemporal(LocalDate::class.java)).isTrue()
		assertThat(SubjectTypes.isTemporal(String::class.java)).isFalse()
		assertThat(SubjectTypes.isCharSequence(String::class.java)).isTrue()
		assertThat(SubjectTypes.isCollectionLike(List::class.java)).isTrue()
		assertThat(SubjectTypes.isCollectionLike(IntArray::class.java)).isTrue()
		assertThat(SubjectTypes.isMap(Map::class.java)).isTrue()
		assertThat(SubjectTypes.isUnknown(Any::class.java)).isTrue()
		assertThat(SubjectTypes.isUnknown(String::class.java)).isFalse()
	}

	@Test
	@DisplayName("day/month support excludes time-only / duration / period leaves")
	fun temporalFieldSupport() {
		assertThat(SubjectTypes.supportsDayOfWeek(LocalDate::class.java)).isTrue()
		assertThat(SubjectTypes.supportsDayOfWeek(LocalTime::class.java)).isFalse()
		assertThat(SubjectTypes.supportsDayOfWeek(Instant::class.java)).isFalse()
		assertThat(SubjectTypes.supportsDayOfMonth(LocalDate::class.java)).isTrue()
		assertThat(SubjectTypes.supportsMonthOfYear(LocalDate::class.java)).isTrue()
		assertThat(SubjectTypes.isDurationCompatible(Duration::class.java)).isTrue()
		assertThat(SubjectTypes.isPeriodCompatible(java.time.Period::class.java)).isTrue()
		assertThat(SubjectTypes.mayBeDeepEmptyStructured(UUID::class.java)).isFalse()
		assertThat(SubjectTypes.mayBeDeepEmptyStructured(String::class.java)).isFalse()
		assertThat(SubjectTypes.mayBeDeepEmptyStructured(Map::class.java)).isTrue()
		// Month is an enum, not Temporal — must not be treated as a date-bearing temporal.
		assertThat(SubjectTypes.isTemporal(Month::class.java)).isFalse()
	}
}
