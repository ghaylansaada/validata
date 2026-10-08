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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Pins every documented `scoreFit` tier against the pure [TypeView] API so selection ranks cannot
 * silently drift.
 * 
 * @author Ghaylan Saada
 */
class ValidatorCompatibilityTest {
	
	@Test
	@DisplayName("rank 0 — exact concrete match")
	fun exactMatch() {
		assertThat(rank(string(), string())).isEqualTo(0)
	}
	
	@Test
	@DisplayName("rank 1 / 0 — boxed Int and kotlin.Int share a canonical form")
	fun boxedPrimitive() {
		val kotlinInt = TypeView(TypeNames.INT_KOTLIN)
		val javaInt = TypeView(TypeNames.INTEGER_JAVA)
		assertThat(rank(kotlinInt, javaInt)).isEqualTo(0)
	}
	
	@Test
	@DisplayName("rank 2 — numeric widens to Number")
	fun numericToNumber() {
		assertThat(rank(TypeView(TypeNames.INT_KOTLIN), TypeView(TypeNames.NUMBER_KOTLIN))).isEqualTo(2)
		assertThat(rank(TypeView(TypeNames.NUMBER_KOTLIN), TypeView(TypeNames.INT_KOTLIN))).isEqualTo(2)
	}
	
	@Test
	@DisplayName("rank 3 — comparable-numeric to Comparable")
	fun comparableNumeric() {
		assertThat(rank(TypeView(TypeNames.INT_KOTLIN), TypeView(TypeNames.COMPARABLE_KOTLIN))).isEqualTo(3)
	}
	
	@Test
	@DisplayName("rank 4 — Temporal fits LocalDate; Duration and Month do not")
	fun temporalNotDurationOrMonth() {
		val localDate = TypeView(
			qualifiedName = TypeNames.LOCAL_DATE,
			assignableSupertypes = setOf(TypeNames.TEMPORAL, "java.time.chrono.ChronoLocalDate"),
		)
		assertThat(rank(localDate, TypeView(TypeNames.TEMPORAL))).isEqualTo(4)
		assertThat(rank(localDate, TypeView(TypeNames.DURATION))).isNull()
		assertThat(rank(localDate, TypeView(TypeNames.MONTH))).isNull()
	}
	
	@Test
	@DisplayName("rank 5 — map-like types that are not assignable (Map value vs HashMap<*> validator)")
	fun mapWildcard() {
		val value = TypeView(
			TypeNames.MAP_KOTLIN,
			typeArguments = listOf(string(), string()),
		)
		val validator = TypeView(
			"java.util.HashMap",
			typeArguments = listOf(TypeView.WILDCARD, TypeView.WILDCARD),
			assignableSupertypes = setOf(TypeNames.MAP_KOTLIN, TypeNames.MAP_JAVA),
		)
		assertThat(rank(value, validator)).isEqualTo(5)
	}
	
	@Test
	@DisplayName("rank 6 — array of scalars (elem boxed / numeric)")
	fun arrayOfScalars() {
		val value = TypeView(
			qualifiedName = TypeNames.INT_ARRAY,
			isArray = true,
			arrayElement = TypeView(TypeNames.INT_KOTLIN),
		)
		val validator = TypeView(
			qualifiedName = TypeNames.ARRAY_KOTLIN,
			isArray = true,
			arrayElement = TypeView(TypeNames.NUMBER_KOTLIN),
		)
		assertThat(rank(value, validator)).isEqualTo(6)
	}
	
	@Test
	@DisplayName("rank 7 — array vs Cloneable carrier (ArraySizeValidator)")
	fun arrayVsCloneable() {
		val value = TypeView(
			qualifiedName = TypeNames.INT_ARRAY,
			isArray = true,
			arrayElement = TypeView(TypeNames.INT_KOTLIN),
		)
		val validator = TypeView(qualifiedName = TypeNames.CLONEABLE_KOTLIN)
		assertThat(rank(value, validator)).isEqualTo(7)
	}
	
	@Test
	@DisplayName("rank 7 — array vs Array of Any")
	fun arrayOfAny() {
		val value = TypeView(
			qualifiedName = TypeNames.ARRAY_KOTLIN,
			isArray = true,
			arrayElement = string(),
		)
		val validator = TypeView(
			qualifiedName = TypeNames.ARRAY_KOTLIN,
			isArray = true,
			arrayElement = TypeView(TypeNames.ANY_KOTLIN),
		)
		assertThat(rank(value, validator)).isEqualTo(7)
	}
	
	@Test
	@DisplayName("rank 8 — collection-like types that are not assignable (Collection vs List<*>)")
	fun collectionWildcard() {
		val value = TypeView(
			TypeNames.COLLECTION_KOTLIN,
			typeArguments = listOf(string()),
		)
		val validator = TypeView(
			TypeNames.LIST_KOTLIN,
			typeArguments = listOf(TypeView.WILDCARD),
			assignableSupertypes = setOf(TypeNames.COLLECTION_KOTLIN),
		)
		assertThat(rank(value, validator)).isEqualTo(8)
	}
	
	@Test
	@DisplayName("rank 9 — Any catch-all")
	fun anyFallback() {
		assertThat(rank(TypeView("java.time.DayOfWeek"), TypeView(TypeNames.ANY_KOTLIN))).isEqualTo(9)
	}
	
	@Test
	@DisplayName("incompatible types yield null")
	fun incompatible() {
		assertThat(rank(string(), TypeView(TypeNames.NUMBER_KOTLIN))).isNull()
	}
	
	@Test
	@DisplayName("tie on rank is broken by FQCN outside this function — ranks alone stay equal")
	fun genuineTieRanksEqual() {
		val value = TypeView(
			TypeNames.STRING_KOTLIN,
			assignableSupertypes = setOf(TypeNames.CHAR_SEQUENCE_KOTLIN),
		)
		val a = TypeView(TypeNames.CHAR_SEQUENCE_KOTLIN)
		val b = TypeView(TypeNames.CHAR_SEQUENCE_JAVA)
		assertThat(rank(value, a)).isEqualTo(rank(value, b))
	}
	
	@Test
	@DisplayName("typeArgsMatch accepts empty expected and wildcards")
	fun typeArgsMatchRules() {
		assertThat(ValidatorCompatibility.typeArgsMatch(listOf(string()), emptyList())).isTrue()
		assertThat(
			ValidatorCompatibility.typeArgsMatch(
				emptyList(),
				listOf(TypeView.WILDCARD),
			),
		).isTrue()
		assertThat(
			ValidatorCompatibility.typeArgsMatch(
				listOf(string()),
				listOf(TypeView(TypeNames.INT_KOTLIN)),
			),
		).isFalse()
	}
	
	private fun rank(
		value: TypeView,
		validator: TypeView
	): Int? = ValidatorCompatibility.scoreFit(value, validator)
	
	private fun string(): TypeView = TypeView(TypeNames.STRING_KOTLIN)
	
	@Test
	@DisplayName("TypeView overload is the ranking surface (KSType overload wraps both sides)")
	fun typeViewOverloadIsPrimary() {
		val a = string()
		val b = TypeView(TypeNames.ANY_KOTLIN)
		assertThat(ValidatorCompatibility.scoreFit(a, b)).isEqualTo(9)
	}
}
