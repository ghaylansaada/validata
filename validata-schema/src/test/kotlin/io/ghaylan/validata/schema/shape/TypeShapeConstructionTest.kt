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
package io.ghaylan.validata.schema.shape

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.support.metadata.RequiredConfig
import io.ghaylan.validata.schema.support.metadata.RequiredRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Construction contracts for the sealed [TypeShape] tree beyond scalar / object-ref smoke tests.
 *
 * ## Why this suite exists
 *
 * Documents how producers should nest [IterableShape] / [MapShape] / [DynamicShape] and where
 * type-use constraints hang. Property-level constraints stay on
 * [PropertySpec] — this suite asserts that split stays visible in IR.
 * 
 * @author Ghaylan Saada
 */
class TypeShapeConstructionTest {
	
	@Test
	@DisplayName("IterableShape carries an element shape and optional type-use constraints")
	fun iterableShape() {
		val elementConstraint = CompiledConstraint(RequiredConfig, RequiredRunner, order = 0)
		val shape = IterableShape(
			element = ScalarShape(ScalarKind.STRING, constraints = listOf(elementConstraint)),
		)
		
		assertThat(shape.element).isInstanceOf(ScalarShape::class.java)
		assertThat((shape.element as ScalarShape).kind).isEqualTo(ScalarKind.STRING)
		assertThat(shape.element.constraints).containsExactly(elementConstraint)
		assertThat(shape.constraints).isEmpty()
	}
	
	@Test
	@DisplayName("MapShape keeps independent key and value shapes")
	fun mapShape() {
		val shape = MapShape(
			key = ScalarShape(ScalarKind.STRING),
			value = ObjectRefShape(lazyOf(ObjectSchema(type = Any::class.java, properties = emptyList()))),
		)
		
		assertThat(shape.key).isInstanceOf(ScalarShape::class.java)
		assertThat(shape.value).isInstanceOf(ObjectRefShape::class.java)
		assertThat((shape.value as ObjectRefShape).ref.value.type).isEqualTo(Any::class.java)
	}
	
	@Test
	@DisplayName("DynamicShape is a constraint-bearing opaque node")
	fun dynamicShape() {
		val constraint = CompiledConstraint(RequiredConfig, RequiredRunner, order = 1)
		val shape = DynamicShape(constraints = listOf(constraint))
		
		assertThat(shape.constraints).containsExactly(constraint)
	}
	
	@Test
	@DisplayName("MapShape can carry type-use constraints on key and value nodes")
	fun mapShapeWithKeyValueConstraints() {
		val keyConstraint = CompiledConstraint(RequiredConfig, RequiredRunner, order = 0)
		val valueConstraint = CompiledConstraint(RequiredConfig, RequiredRunner, order = 1)
		val shape = MapShape(
			key = ScalarShape(ScalarKind.STRING, constraints = listOf(keyConstraint)),
			value = ScalarShape(ScalarKind.INTEGRAL, constraints = listOf(valueConstraint)),
		)
		
		assertThat(shape.key.constraints).containsExactly(keyConstraint)
		assertThat(shape.value.constraints).containsExactly(valueConstraint)
		assertThat(shape.constraints).isEmpty()
	}
	
	@Test
	@DisplayName("IterableShape may nest another IterableShape for multi-dimensional collections")
	fun nestedIterableShape() {
		val shape = IterableShape(
			element = IterableShape(element = ScalarShape(ScalarKind.STRING)),
		)
		
		assertThat(shape.element).isInstanceOf(IterableShape::class.java)
		assertThat((shape.element as IterableShape).element).isInstanceOf(ScalarShape::class.java)
	}
	
	@Test
	@DisplayName("property-level constraints sit on PropertySpec; type-use constraints sit on the shape")
	fun propertyLevelVsTypeUseSplit() {
		val propertyConstraint = CompiledConstraint(RequiredConfig, RequiredRunner, order = 0)
		val typeUseConstraint = CompiledConstraint(RequiredConfig, RequiredRunner, order = 1)
		val property = PropertySpec(
			declaredName = "emails",
			externalName = "emails",
			shape = IterableShape(
				element = ScalarShape(ScalarKind.STRING, constraints = listOf(typeUseConstraint)),
			),
			read = { null },
			constraints = listOf(propertyConstraint),
		)
		
		assertThat(property.constraints).containsExactly(propertyConstraint)
		assertThat(property.shape.constraints).isEmpty()
		assertThat((property.shape as IterableShape).element.constraints).containsExactly(typeUseConstraint)
	}
}
