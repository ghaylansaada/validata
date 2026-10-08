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

/**
 * Edge cases for [CollectionUtils.normalizeList].
 * 
 * @author Ghaylan Saada
 */
class CollectionUtilsTest {
	
	@Test
	@DisplayName("null and unknown types become emptyList")
	fun nullAndUnknown() {
		assertThat(CollectionUtils.normalizeList(null)).isEmpty()
		assertThat(CollectionUtils.normalizeList("not-a-collection")).isEmpty()
	}
	
	@Test
	@DisplayName("list without nulls reuses the source list identity (no wrapper alloc)")
	fun listWithoutNullsIsView() {
		val source = listOf("a", "b")
		val normalized = CollectionUtils.normalizeList(source)
		assertThat(normalized).containsExactly("a", "b")
		assertThat(normalized).isSameAs(source)
	}
	
	@Test
	@DisplayName("list with nulls is compacted")
	fun listWithNullsCompacted() {
		assertThat(CollectionUtils.normalizeList(listOf("a", null, "b"))).containsExactly("a", "b")
	}
	
	@Test
	@DisplayName("Set is materialised into an index-stable list")
	fun setMaterialised() {
		assertThat(CollectionUtils.normalizeList(linkedSetOf("x", "y"))).containsExactly("x", "y")
	}
	
	@Test
	@DisplayName("primitive arrays wrap without copying elements")
	fun primitiveArrays() {
		assertThat(CollectionUtils.normalizeList(intArrayOf(1, 2, 3))).containsExactly(1, 2, 3)
		assertThat(CollectionUtils.normalizeList(booleanArrayOf(true, false))).containsExactly(true, false)
	}
	
	@Test
	@DisplayName("reference arrays drop nulls")
	fun referenceArrays() {
		assertThat(CollectionUtils.normalizeList(arrayOf("a", null, "b"))).containsExactly("a", "b")
		assertThat(CollectionUtils.normalizeList(arrayOf("a", "b"))).containsExactly("a", "b")
	}
	
	@Test
	@DisplayName("all-null reference array becomes emptyList")
	fun allNullReferenceArray() {
		assertThat(CollectionUtils.normalizeList(arrayOf<String?>(null, null))).isEmpty()
	}
	
	@Test
	@DisplayName("empty list and empty array become emptyList")
	fun emptyContainers() {
		assertThat(CollectionUtils.normalizeList(emptyList<Any>())).isEmpty()
		assertThat(CollectionUtils.normalizeList(emptyArray<Any>())).isEmpty()
	}
	
	@Test
	@DisplayName("CharArray wraps without copying elements")
	fun charArray() {
		assertThat(CollectionUtils.normalizeList(charArrayOf('a', 'b'))).containsExactly('a', 'b')
		assertThat(CollectionUtils.toIndexedElements(charArrayOf('x'))).containsExactly('x')
	}
	
	@Test
	@DisplayName("toIndexedElements preserves nulls and indices")
	fun toIndexedElementsPreservesNulls() {
		assertThat(CollectionUtils.toIndexedElements(listOf("a", null, "b"))).containsExactly("a", null, "b")
		assertThat(CollectionUtils.toIndexedElements(arrayOf<String?>(null, null))).containsExactly(null, null)
		assertThat(CollectionUtils.toIndexedElements(null)).isEmpty()
	}
	
	@Test
	@DisplayName("toArrayContextList shares identity with toIndexedElements")
	fun toArrayContextListSameInstance() {
		val source = listOf("a", null, "b")
		val indexed = CollectionUtils.toIndexedElements(source)
		val arrayCtx = CollectionUtils.toArrayContextList(source)
		assertThat(arrayCtx).isSameAs(indexed)
		assertThat(arrayCtx).containsExactly("a", null, "b")
	}
}
