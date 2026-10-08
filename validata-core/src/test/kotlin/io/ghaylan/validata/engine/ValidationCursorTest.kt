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
package io.ghaylan.validata.engine

import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Push/pop protocol and root binding for [ValidationCursor].
 * 
 * @author Ghaylan Saada
 */
class ValidationCursorTest {
	
	@Test
	@DisplayName("pushProperty then pop restores the previous frame")
	fun pushPopProperty() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = true,
			groups = setOf(OnDefault::class),
		)
		assertThat(cursor.depth).isEqualTo(0)
		
		cursor.pushProperty(
			name = "email",
			propertyShape = ScalarShape(ScalarKind.STRING),
			container = null,
			arrayContext = null,
		)
		assertThat(cursor.depth).isEqualTo(1)
		assertThat(cursor.fieldName).isEqualTo("email")
		assertThat(cursor.fieldPath).isEqualTo("email")
		
		cursor.pop()
		assertThat(cursor.depth).isEqualTo(0)
		assertThat(cursor.fieldName).isEmpty()
	}
	
	@Test
	@DisplayName("pushIndex builds bracket paths and restores on pop")
	fun pushPopIndex() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		cursor.pushProperty("items", ScalarShape(ScalarKind.STRING), null, null)
		cursor.pushIndex(2, ScalarShape(ScalarKind.STRING), null)
		assertThat(cursor.fieldPath).isEqualTo("items[2]")
		assertThat(cursor.elementIndex).isEqualTo(2)
		assertThat(cursor.depth).isEqualTo(2)
		
		cursor.pop()
		assertThat(cursor.fieldPath).isEqualTo("items")
		assertThat(cursor.elementIndex).isEqualTo(ValidationContext.NO_ELEMENT_INDEX)
	}
	
	@Test
	@DisplayName("nested map key/value frames restore correctly")
	fun pushPopMapFrames() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		cursor.pushProperty("entries", null, null, null)
		cursor.pushMapKeys("k1", ScalarShape(ScalarKind.STRING))
		assertThat(cursor.fieldPath).contains("keys")
		cursor.pop()
		cursor.pushMapValue("k1", ScalarShape(ScalarKind.STRING))
		assertThat(cursor.fieldPath).isEqualTo("entries[k1]")
		cursor.pop()
		assertThat(cursor.fieldPath).isEqualTo("entries")
	}
	
	@Test
	@DisplayName("markPathFailed makes hasFailedPath true for the current path")
	fun failedPathSet() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = true,
			groups = setOf(OnDefault::class),
		)
		cursor.pushProperty(
			name = "email",
			propertyShape = ScalarShape(ScalarKind.STRING),
			container = null,
			arrayContext = null,
		)
		assertThat(cursor.hasFailedPath()).isFalse()
		cursor.markPathFailed()
		assertThat(cursor.hasFailedPath()).isTrue()
		
		cursor.pop()
		cursor.pushProperty(
			name = "name",
			propertyShape = ScalarShape(ScalarKind.STRING),
			container = null,
			arrayContext = null,
		)
		assertThat(cursor.hasFailedPath()).isFalse()
	}
	
	@Test
	@DisplayName("bindRoot sets shape without changing depth")
	fun bindRootKeepsDepthZero() {
		val shape = ScalarShape(ScalarKind.STRING)
		val cursor = ValidationCursor.root(
			oneErrorPerParam = true,
			groups = emptySet(),
			shape = null,
		)
		cursor.bindRoot(shape = shape, container = null)
		assertThat(cursor.shape).isSameAs(shape)
		assertThat(cursor.depth).isEqualTo(0)
		cursor.bindRoot(shape = null, container = null)
		assertThat(cursor.shape).isNull()
	}
	
	@Test
	@DisplayName("pop with an empty stack fails loudly")
	fun popEmptyStack() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = true,
			groups = setOf(OnDefault::class),
		)
		assertThatThrownBy { cursor.pop() }.isInstanceOf(IllegalStateException::class.java)
			.hasMessageContaining("empty stack")
	}
	
	@Test
	@DisplayName("pushSectionContainer replaces containerObject and restores on pop")
	fun pushPopSectionContainer() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		val root = ValidationContextValue(value = "root" as Any, objectSchema = null, shape = null)
		val section = ValidationContextValue(value = "section" as Any, objectSchema = null, shape = null)
		cursor.bindRoot(null, root)
		assertThat(cursor.containerObject).isSameAs(root)
		
		cursor.pushSectionContainer(section)
		assertThat(cursor.containerObject).isSameAs(section)
		assertThat(cursor.depth).isEqualTo(0)
		
		cursor.pop()
		assertThat(cursor.containerObject).isSameAs(root)
	}
	
	@Test
	@DisplayName("deep push/pop reuses frame slots without leaking depth")
	fun deepPushPop() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		repeat(20) { i ->
			cursor.pushProperty("p$i", null, null, null)
		}
		assertThat(cursor.depth).isEqualTo(20)
		repeat(20) { cursor.pop() }
		assertThat(cursor.depth).isEqualTo(0)
	}
	
	@Test
	@DisplayName("sibling indexes share beforeLastIndex identity for Distinct caching")
	fun siblingIndexesShareParentPathIdentity() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		cursor.pushProperty("items", ScalarShape(ScalarKind.STRING), null, null)
		cursor.pushIndex(0, ScalarShape(ScalarKind.STRING), null)
		val parent0 = cursor.path.beforeLastIndex()
		cursor.pop()
		cursor.pushIndex(1, ScalarShape(ScalarKind.STRING), null)
		val parent1 = cursor.path.beforeLastIndex()
		assertThat(parent1).isSameAs(parent0)
		assertThat(cursor.fieldPath).isEqualTo("items[1]")
	}
	
	@Test
	@DisplayName("empty property name push leaves fieldPath unchanged from parent")
	fun emptyPropertyNameLeavesFieldPathUnchanged() {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		cursor.pushProperty("email", ScalarShape(ScalarKind.STRING), null, null)
		assertThat(cursor.fieldPath).isEqualTo("email")
		
		cursor.pushProperty("", ScalarShape(ScalarKind.STRING), null, null)
		assertThat(cursor.fieldPath).isEqualTo("email")
		assertThat(cursor.depth).isEqualTo(2)
		
		cursor.pop()
		assertThat(cursor.fieldPath).isEqualTo("email")
	}
}
