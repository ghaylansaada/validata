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

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Polymorphic body dispatch: runtime class selects the subtype schema during validateRequest.
 * 
 * @author Ghaylan Saada
 */
class ValidatorEnginePolymorphismTest {
	
	open class Parent(val label: String? = null)
	
	class Child(
		label: String? = null,
		val childOnly: String? = null
	): Parent(label)
	
	private fun childSchema(): ObjectSchema {
		val required = EngineTestSupport.requiredCompiled()
		return ObjectSchema(
			type = Child::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "label",
					externalName = "label",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as Child).label },
				),
				PropertySpec(
					declaredName = "childOnly",
					externalName = "childOnly",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as Child).childOnly },
					constraints = listOf(required),
				),
			),
		)
	}
	
	private fun parentSchema(child: ObjectSchema = childSchema()): ObjectSchema = ObjectSchema(
		type = Parent::class.java,
		properties = listOf(
			PropertySpec(
				declaredName = "label",
				externalName = "label",
				shape = ScalarShape(ScalarKind.STRING),
				read = ValueReader { (it as Parent).label },
			),
		),
		subtypes = mapOf(Child::class.java to child),
	)
	
	@Test
	@DisplayName("Child instance missing childOnly reports that child field path")
	fun childMissingRequiredField() {
		val engine = EngineTestSupport.engine()
		val schema = EngineTestSupport.bodyRequestSchema(parentSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Child(label = "ok", childOnly = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).contains("childOnly")
	}
	
	@Test
	@DisplayName("Parent instance walks parent properties only — no childOnly path")
	fun parentInstanceIgnoresChildFields() {
		val engine = EngineTestSupport.engine()
		val schema = EngineTestSupport.bodyRequestSchema(parentSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Parent(label = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path.orEmpty() }).noneMatch { it == "childOnly" }
		assertThat(errors).isEmpty()
	}
}
