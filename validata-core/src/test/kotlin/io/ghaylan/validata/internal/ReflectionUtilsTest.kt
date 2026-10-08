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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Public [ReflectionUtils] APIs not already covered by [TypeStructureTest].
 * 
 * @author Ghaylan Saada
 */
class ReflectionUtilsTest {
	
	data class SampleDto(val name: String?)
	
	enum class Color { RED }
	
	interface Marker
	
	@Nested
	@DisplayName("map / collection shape")
	inner class MapAndCollectionShape {
		
		@Test
		@DisplayName("isMapLike is true for Map and its implementations")
		fun isMapLike() {
			assertThat(ReflectionUtils.isMapLike(Map::class.java)).isTrue()
			assertThat(ReflectionUtils.isMapLike(HashMap::class.java)).isTrue()
			assertThat(ReflectionUtils.isMapLike(List::class.java)).isFalse()
			assertThat(ReflectionUtils.isMapLike(String::class.java)).isFalse()
		}
		
		@Test
		@DisplayName("isCollectionLike covers arrays and Collection subtypes")
		fun isCollectionLike() {
			assertThat(ReflectionUtils.isCollectionLike(List::class.java)).isTrue()
			assertThat(ReflectionUtils.isCollectionLike(Set::class.java)).isTrue()
			assertThat(ReflectionUtils.isCollectionLike(Array<String>::class.java)).isTrue()
			assertThat(ReflectionUtils.isCollectionLike(IntArray::class.java)).isTrue()
			assertThat(ReflectionUtils.isCollectionLike(Map::class.java)).isFalse()
			assertThat(ReflectionUtils.isCollectionLike(String::class.java)).isFalse()
		}
		
		@Test
		@DisplayName("isCollection classifies runtime values by their class")
		fun isCollection() {
			assertThat(ReflectionUtils.isCollection(listOf(1))).isTrue()
			assertThat(ReflectionUtils.isCollection(intArrayOf(1, 2))).isTrue()
			assertThat(ReflectionUtils.isCollection("text")).isFalse()
			assertThat(ReflectionUtils.isCollection(mapOf("a" to 1))).isFalse()
		}
	}
	
	@Nested
	@DisplayName("isObjectLike")
	inner class ObjectLike {
		
		@Test
		@DisplayName("true for data classes; false for String, List, interface, enum")
		fun dataClassVsNonObjects() {
			assertThat(ReflectionUtils.isObjectLike(SampleDto::class.java)).isTrue()
			assertThat(ReflectionUtils.isObjectLike(String::class.java)).isFalse()
			assertThat(ReflectionUtils.isObjectLike(List::class.java)).isFalse()
			assertThat(ReflectionUtils.isObjectLike(Marker::class.java)).isFalse()
			assertThat(ReflectionUtils.isObjectLike(Color::class.java)).isFalse()
		}
	}
	
	@Nested
	@DisplayName("isScalar")
	inner class Scalar {
		
		@Test
		@DisplayName("true for string and number; false for list and data class")
		fun stringNumberVsListDto() {
			assertThat(ReflectionUtils.isScalar("hello")).isTrue()
			assertThat(ReflectionUtils.isScalar(42)).isTrue()
			assertThat(ReflectionUtils.isScalar(listOf(1))).isFalse()
			assertThat(ReflectionUtils.isScalar(SampleDto("x"))).isFalse()
		}
	}
	
	@Nested
	@DisplayName("numeric helpers")
	inner class Numeric {
		
		@Test
		@DisplayName("isNumericType accepts boxed Number subtypes")
		fun isNumericType() {
			assertThat(ReflectionUtils.isNumericType(Integer::class)).isTrue()
			assertThat(ReflectionUtils.isNumericType(BigDecimal::class)).isTrue()
			assertThat(ReflectionUtils.isNumericType(String::class)).isFalse()
			assertThat(ReflectionUtils.isNumericType(SampleDto::class)).isFalse()
		}
		
		@Test
		@DisplayName("isComparableNumeric requires Number and Comparable")
		fun isComparableNumeric() {
			assertThat(ReflectionUtils.isComparableNumeric(Integer::class)).isTrue()
			assertThat(ReflectionUtils.isComparableNumeric(BigDecimal::class)).isTrue()
			assertThat(ReflectionUtils.isComparableNumeric(String::class)).isFalse()
		}
		
		@Test
		@DisplayName("primitiveOrBoxedMatch pairs Int with Integer")
		fun primitiveOrBoxedMatch() {
			assertThat(ReflectionUtils.primitiveOrBoxedMatch(Int::class, Integer::class)).isTrue()
			assertThat(ReflectionUtils.primitiveOrBoxedMatch(Integer::class, Int::class)).isTrue()
			assertThat(ReflectionUtils.primitiveOrBoxedMatch(Int::class, Long::class)).isFalse()
			assertThat(ReflectionUtils.primitiveOrBoxedMatch(String::class, String::class)).isTrue()
		}
	}
	
	@Nested
	@DisplayName("TypeInfo helpers")
	inner class TypeInfoHelpers {
		
		@Test
		@DisplayName("isTypeInfoCollectionLike / isTypeInfoMapLike / isWildcard via infoFromType")
		fun collectionMapAndWildcard() {
			val listInfo = ReflectionUtils.infoFromType(
				object: Any() {
					@Suppress("unused")
					val field: List<String>? = null
				}.javaClass.getDeclaredField("field").genericType,
			)
			assertThat(ReflectionUtils.isTypeInfoCollectionLike(listInfo)).isTrue()
			assertThat(ReflectionUtils.isTypeInfoMapLike(listInfo)).isFalse()
			val mapInfo = ReflectionUtils.infoFromType(
				object: Any() {
					@Suppress("unused")
					val field: Map<String, Int>? = null
				}.javaClass.getDeclaredField("field").genericType,
			)
			assertThat(ReflectionUtils.isTypeInfoMapLike(mapInfo)).isTrue()
			assertThat(ReflectionUtils.isTypeInfoCollectionLike(mapInfo)).isFalse()
			val starInfo = ReflectionUtils.infoFromType(
				object: Any() {
					@Suppress("unused")
					val field: List<*>? = null
				}.javaClass.getDeclaredField("field").genericType,
			)
			val element = starInfo.typeArguments.single()
			assertThat(ReflectionUtils.isWildcard(element)).isTrue()
			assertThat(ReflectionUtils.isWildcard(ReflectionUtils.infoFromClass(Any::class.java))).isTrue()
		}
		
		@Test
		@DisplayName("infoFromClass for IntArray has ARRAY structure")
		fun intArrayStructure() {
			val info = ReflectionUtils.infoFromClass(IntArray::class.java)
			assertThat(info.structure).isEqualTo(TypeStructure.ARRAY)
		}
	}
	
	@Nested
	@DisplayName("leaf types")
	inner class LeafTypes {
		
		@Test
		@DisplayName("isLeafType is true for java.time.LocalDate (package leaf)")
		fun localDateIsPackageLeaf() {
			assertThat(ReflectionUtils.isLeafType(LocalDate::class.java)).isTrue()
		}
	}
}
