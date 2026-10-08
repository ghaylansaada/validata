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
package io.ghaylan.validata.constraint.validator.distinct

import io.ghaylan.validata.constraint.annotation.DistinctConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.runtime.PathSegment
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.shape.*
import io.ghaylan.validata.support.TestValidationContext
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DistinctValidator] (element type-use placement only).
 *
 * @author Ghaylan Saada
 */
class DistinctValidatorTest {
	
	data class KeyedItem(
		val code: String?,
		val label: String?
	)
	
	private fun constraint(
		by: Set<String> = emptySet(),
	): DistinctConstraint = DistinctConstraint(
		by = by,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
	
	private fun keyedItemSchema(): ObjectSchema = ObjectSchema(
		type = KeyedItem::class.java,
		properties = listOf(
			PropertySpec(
				"code",
				"code",
				ScalarShape(ScalarKind.STRING),
				ValueReader { (it as KeyedItem).code },
			),
			PropertySpec(
				"label",
				"label",
				ScalarShape(ScalarKind.STRING),
				ValueReader { (it as KeyedItem).label },
			),
		),
	)
	
	private fun elementContext(
		siblings: List<Any>,
		elementIndex: Int,
		elementShape: TypeShape = ScalarShape(ScalarKind.STRING),
	): TestValidationContext {
		val listPath = PathSegment.Name(PathSegment.Root, "items")
		val indexedPath = PathSegment.Index(listPath, elementIndex)
		return TestValidationContext(
			path = indexedPath,
			fieldName = "items",
			shape = elementShape,
			elementIndex = elementIndex,
			array = ValidationContextValue(
				value = siblings,
				shape = IterableShape(element = elementShape),
			),
			groups = ValidatorTestSupport.defaultGroups,
		)
	}
	
	@Test
	@DisplayName("requiresArrayContext flag is true")
	fun requiresArrayContextFlagIsTrue() {
		assertThat(DistinctValidator.requiresArrayContext).isTrue()
	}
	
	@Nested
	@DisplayName("Container shapes are no-ops")
	inner class ContainerNoOps {
		
		@Test
		@DisplayName("IterableShape at current node is a no-op")
		fun iterableShapeIsNoOp() {
			val ctx = TestValidationContext(
				shape = IterableShape(element = ScalarShape(ScalarKind.STRING)),
				groups = ValidatorTestSupport.defaultGroups,
			)
			assertValid(DistinctValidator, listOf("a", "a"), constraint(), ctx)
		}
		
		@Test
		@DisplayName("MapShape at current node is a no-op")
		fun mapShapeIsNoOp() {
			val ctx = TestValidationContext(
				shape = MapShape(
					key = ScalarShape(ScalarKind.STRING),
					value = ScalarShape(ScalarKind.STRING),
				),
				groups = ValidatorTestSupport.defaultGroups,
			)
			assertValid(DistinctValidator, mapOf("a" to "1", "b" to "1"), constraint(), ctx)
		}
	}
	
	@Nested
	@DisplayName("Element context — scalar siblings")
	inner class ElementContextScalar {
		
		@Test
		@DisplayName("duplicate element fails with COLLECTION_DUPLICATE")
		fun duplicateElementFails() {
			val siblings = listOf("a", "a", "b")
			val ctx = elementContext(siblings, elementIndex = 1)
			val constraint = constraint()
			val error = DistinctValidator.runValidation("a", constraint, ctx)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.COLLECTION_DUPLICATE)
			assertThat(error.metadata).isEqualTo(constraint)
			assertThat(error.message).isEqualTo("Item is duplicated in the collection.")
		}
		
		@Test
		@DisplayName("unique element passes")
		fun uniqueElementPasses() {
			val siblings = listOf("a", "b", "a")
			val ctx = elementContext(siblings, elementIndex = 1)
			assertValid(DistinctValidator, "b", constraint(), ctx)
		}
		
		@Test
		@DisplayName("missing array context skips")
		fun missingArrayContextSkips() {
			val ctx = TestValidationContext(
				shape = ScalarShape(ScalarKind.STRING),
				groups = ValidatorTestSupport.defaultGroups,
			)
			assertValid(DistinctValidator, "a", constraint(), ctx)
		}
	}
	
	@Nested
	@DisplayName("Element context — keyed object siblings")
	inner class ElementContextKeyed {
		
		@Test
		@DisplayName("duplicate by field fails with keyed message")
		fun keyedDuplicateFails() {
			val schema = keyedItemSchema()
			val elementShape = ObjectRefShape(lazy { schema })
			val siblings = listOf(KeyedItem("A", "one"), KeyedItem("A", "two"))
			val ctx = elementContext(siblings, elementIndex = 1, elementShape = elementShape)
			val constraint = constraint(by = setOf("code"))
			val error = DistinctValidator.runValidation(siblings[1], constraint, ctx)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.COLLECTION_DUPLICATE)
			assertThat(error.message).isEqualTo("Item is duplicated in the collection by fields: code.")
		}
		
		@Test
		@DisplayName("unique by field passes")
		fun keyedUniquePasses() {
			val schema = keyedItemSchema()
			val elementShape = ObjectRefShape(lazy { schema })
			val siblings = listOf(KeyedItem("A", "one"), KeyedItem("B", "two"))
			val ctx = elementContext(siblings, elementIndex = 1, elementShape = elementShape)
			assertValid(
				DistinctValidator,
				siblings[1],
				constraint(by = setOf("code")),
				ctx,
			)
		}
		
		@Test
		@DisplayName("empty by on objects uses whole-element equality")
		fun emptyByWholeObject() {
			val schema = keyedItemSchema()
			val elementShape = ObjectRefShape(lazy { schema })
			val a = KeyedItem("A", "one")
			val siblings = listOf(a, KeyedItem("A", "one"))
			val ctx = elementContext(siblings, elementIndex = 1, elementShape = elementShape)
			val error = DistinctValidator.runValidation(siblings[1], constraint(), ctx)
			assertThat(error).isNotNull
			assertThat(error!!.message).isEqualTo("Item is duplicated in the collection.")
		}
	}
}
