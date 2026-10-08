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
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.constraint.ConstraintConfig
import io.ghaylan.validata.schema.constraint.ConstraintFailure
import io.ghaylan.validata.schema.constraint.ConstraintRunner
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.TinyBody
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Lookup and runner-contract edge cases for [ValidatorEngine].
 * 
 * @author Ghaylan Saada
 */
class ValidatorEngineRequestLookupTest {
	
	private val engine = EngineTestSupport.engine()
	
	@Test
	@DisplayName("validateRequest(id) looks up the schema from the registry")
	fun validateById() {
		val registry = EngineTestSupport.unfrozenRegistry()
		val engine = ValidatorEngine(registry)
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			id = "lookup-by-id",
		)
		registry.registerStaticSchemas(mapOf(schema.id to schema))
		registry.freeze()
		val errors = engine.validateRequest(
			id = "lookup-by-id",
			body = TinyBody(name = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactly("name")
	}
	
	@Test
	@DisplayName("validateRequest(id) fails loudly when the endpoint is unknown")
	fun missingEndpointId() {
		assertThatThrownBy {
			engine.validateRequest(
				id = "missing-endpoint",
				body = null,
				params = null,
				headers = null,
				pathVariables = null,
			)
		}.isInstanceOf(IllegalStateException::class.java)
			.hasMessageContaining("missing-endpoint")
	}
	
	@Test
	@DisplayName("a non-ContextAwareConstraintRunner fails as ClassCastException on the hot path")
	fun wrongRunnerType() {
		val config = object: ConstraintConfig {
			override val message: String = "x"
			override val groups: Set<KClass<*>> = emptySet()
		}
		val runner = ConstraintRunner { _, _ -> ConstraintFailure(code = "X") }
		val bodySchema = ObjectSchema(
			type = TinyBody::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "name",
					externalName = "name",
					shape = ScalarShape(ScalarKind.STRING),
					read = { (it as TinyBody).name },
					constraints = listOf(CompiledConstraint(config, runner, 0)),
				),
			),
		)
		val schema = EngineTestSupport.bodyRequestSchema(body = bodySchema, id = "wrong-runner")
		assertThatThrownBy {
			engine.validateRequest(
				schema = schema,
				body = TinyBody(name = "anything"),
				params = null,
				headers = null,
				pathVariables = null,
			)
		}.isInstanceOf(ClassCastException::class.java)
	}
}
