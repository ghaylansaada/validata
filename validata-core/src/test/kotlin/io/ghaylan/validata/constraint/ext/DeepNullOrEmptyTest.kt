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
package io.ghaylan.validata.constraint.ext

import io.ghaylan.validata.ext.isDeepNullOrEmpty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Edge cases for [isDeepNullOrEmpty].
 * 
 * @author Ghaylan Saada
 */
class DeepNullOrEmptyTest {
	
	@Nested
	@DisplayName("scalar empties")
	inner class ScalarEmpties {
		
		@Test
		@DisplayName("null, Unit, blank, and empty containers are empty")
		fun basicEmpties() {
			assertThat(Unit.isDeepNullOrEmpty()).isTrue()
			assertThat("   ".isDeepNullOrEmpty()).isTrue()
			assertThat(emptyList<String>().isDeepNullOrEmpty()).isTrue()
			assertThat(emptyMap<String, String>().isDeepNullOrEmpty()).isTrue()
			assertThat(Optional.empty<String>()
				.isDeepNullOrEmpty()).isTrue()
		}
		
		@Test
		@DisplayName("present scalars and enums are not empty")
		fun presentLeaves() {
			assertThat("x".isDeepNullOrEmpty()).isFalse()
			assertThat(0.isDeepNullOrEmpty()).isFalse()
			assertThat(DayOfWeekLike.MON.isDeepNullOrEmpty()).isFalse()
		}
		
		@Test
		@DisplayName("chars are always present; empty char arrays are empty")
		fun charAndCharArray() {
			assertThat(' '.isDeepNullOrEmpty()).isFalse()
			assertThat('\u0000'.isDeepNullOrEmpty()).isFalse()
			assertThat('x'.isDeepNullOrEmpty()).isFalse()
			assertThat(charArrayOf().isDeepNullOrEmpty()).isTrue()
			assertThat(charArrayOf('x').isDeepNullOrEmpty()).isFalse()
		}
		
		@Test
		@DisplayName("present Optional and Sequence are not empty")
		fun optionalAndSequence() {
			assertThat(Optional.of("x")
				.isDeepNullOrEmpty()).isFalse()
			assertThat(emptySequence<String>().isDeepNullOrEmpty()).isFalse()
		}
	}
	
	@Nested
	@DisplayName("nested structures")
	inner class NestedStructures {
		
		@Test
		@DisplayName("nested all-empty collections are empty; mixed are not")
		fun nested() {
			assertThat(listOf(null, "", emptyList<Any>()).isDeepNullOrEmpty()).isTrue()
			assertThat(listOf("", "x").isDeepNullOrEmpty()).isFalse()
			assertThat((null to "").isDeepNullOrEmpty()).isTrue()
			assertThat(("a" to "").isDeepNullOrEmpty()).isFalse()
		}
		
		@Test
		@DisplayName("triples require all three components to be empty")
		fun triple() {
			assertThat(Triple(null, "", emptyList<Any>()).isDeepNullOrEmpty()).isTrue()
			assertThat(Triple("a", "", null).isDeepNullOrEmpty()).isFalse()
		}
		
		@Test
		@DisplayName("maps are empty when every value is deeply empty")
		fun mapValues() {
			assertThat(mapOf("a" to null, "b" to "").isDeepNullOrEmpty()).isTrue()
			assertThat(mapOf("a" to "x").isDeepNullOrEmpty()).isFalse()
		}
		
		@Test
		@DisplayName("reference arrays recurse into elements")
		fun referenceArrays() {
			assertThat(arrayOf<Any?>(null, "").isDeepNullOrEmpty()).isTrue()
			assertThat(arrayOf<Any?>("x").isDeepNullOrEmpty()).isFalse()
		}
	}
	
	@Nested
	@DisplayName("primitive arrays")
	inner class PrimitiveArrays {
		
		@Test
		@DisplayName("primitive arrays are empty only when length is zero")
		fun primitiveArrays() {
			assertThat(intArrayOf().isDeepNullOrEmpty()).isTrue()
			assertThat(intArrayOf(0).isDeepNullOrEmpty()).isFalse()
			assertThat(booleanArrayOf().isDeepNullOrEmpty()).isTrue()
		}
	}
	
	@Nested
	@DisplayName("cycle safety")
	inner class CycleSafety {
		
		@Test
		@DisplayName("self-referential structures do not recurse forever")
		fun cycles() {
			val list = mutableListOf<Any?>()
			list.add(list)
			assertThat(list.isDeepNullOrEmpty()).isFalse()
		}
	}
	
	private enum class DayOfWeekLike { MON }
}
