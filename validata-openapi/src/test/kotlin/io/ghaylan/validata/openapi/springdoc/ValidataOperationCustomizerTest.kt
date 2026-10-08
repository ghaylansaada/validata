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
package io.ghaylan.validata.openapi.springdoc

import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.openapi.enrichment.ConstraintExtensionKeys
import io.ghaylan.validata.openapi.enrichment.OpenApiEnrichmentCache
import io.ghaylan.validata.openapi.presentation.ErrorDocPublishContext
import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod

/**
 * Guards publisher invocation, cache skip on a second customize, and Size facets on query params.
 * 
 * @author Ghaylan Saada
 */
class ValidataOperationCustomizerTest {
	
	@BeforeEach
	fun reset() {
		OpenApiEnrichmentCache.resetForTests()
	}
	
	@Test
	@DisplayName("applies Size facets to matching query params and invokes publishers once")
	fun enrichesQueryParamAndPublishesOnce() {
		val target = ProbeController()
		val method = ProbeController::class.java.getMethod("lookup", String::class.java)
		val handler = HandlerMethod(target, method)
		val endpointId = method.getUniqueIdentifier()
		val size = SizeConstraint(2, 40, "", setOf(OnDefault::class))
		val registry = ValidationRegistry()
		registry.registerStaticSchemas(
			mapOf(
				endpointId to EndpointSchema(
					id = endpointId,
					queryParams = ObjectSchema(
						type = Any::class.java,
						properties = listOf(
							PropertySpec(
								declaredName = "name",
								externalName = "name",
								shape = ScalarShape(ScalarKind.STRING),
								read = ValueReader { null },
								constraints = listOf(
									CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 0),
								),
							),
						),
					),
					groups = setOf(OnDefault::class),
				),
			),
		)
		val operation = Operation().apply {
			parameters = mutableListOf(
				Parameter().name("name")
					.`in`("query")
					.schema(Schema<Any>()),
			)
		}
		val publisher = CountingPublisher()
		val customizer = ValidataOperationCustomizer(
			registry = registry,
			mappers = emptyList(),
			errorDocPublishers = listOf(publisher),
		)
		
		customizer.customize(operation, handler)
		assertThat(operation.parameters[0].schema.minLength).isEqualTo(2)
		assertThat(operation.parameters[0].schema.maxLength).isEqualTo(40)
		assertThat(publisher.calls).isEqualTo(1)
		
		customizer.customize(operation, handler)
		assertThat(publisher.calls).isEqualTo(1)
	}
	
	@Test
	@DisplayName("skips Size facets when constraint groups are inactive for the endpoint")
	fun skipsInactiveGroupFacets() {
		val target = ProbeController()
		val method = ProbeController::class.java.getMethod("lookup", String::class.java)
		val handler = HandlerMethod(target, method)
		val endpointId = method.getUniqueIdentifier()
		val size = SizeConstraint(2, 40, "", setOf(OnCreate::class))
		val registry = ValidationRegistry()
		registry.registerStaticSchemas(
			mapOf(
				endpointId to EndpointSchema(
					id = endpointId,
					queryParams = ObjectSchema(
						type = Any::class.java,
						properties = listOf(
							PropertySpec(
								declaredName = "name",
								externalName = "name",
								shape = ScalarShape(ScalarKind.STRING),
								read = ValueReader { null },
								constraints = listOf(
									CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 0),
								),
							),
						),
					),
					groups = setOf(OnDefault::class),
				),
			),
		)
		val operation = Operation().apply {
			parameters = mutableListOf(
				Parameter().name("name")
					.`in`("query")
					.schema(Schema<Any>()),
			)
		}
		val customizer = ValidataOperationCustomizer(
			registry = registry,
			mappers = emptyList(),
		)
		customizer.customize(operation, handler)
		val schema = operation.parameters[0].schema
		assertThat(schema).extracting("minLength", "maxLength")
			.containsExactly(null, null)
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.CONSTRAINTS)).isNull()
	}
	
	private class ProbeController {
		
		@Suppress("unused")
		fun lookup(name: String) = Unit
	}
	
	private class CountingPublisher: ErrorDocPublisher {
		
		var calls: Int = 0
		override fun publish(context: ErrorDocPublishContext) {
			calls += 1
		}
	}
}
