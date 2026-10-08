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

import io.ghaylan.validata.schema.RegistryFixtureDto
import io.ghaylan.validata.schema.shape.DynamicShape
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.DynamicHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * [DynamicShape] cascade via [ValidatorEngine.validateRequest].
 * 
 * @author Ghaylan Saada
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidatorEngineDynamicShapeTest {
	
	private val engine = EngineTestSupport.engine()
	
	@BeforeAll
	fun resetSchemas() {
		GeneratedSchemas.resetForTests()
	}
	
	@AfterAll
	fun clearSchemas() {
		GeneratedSchemas.resetForTests()
	}
	
	@Test
	@DisplayName("DynamicShape resolves a generated nested schema and reports nested field paths")
	fun resolvesGeneratedNestedSchema() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.dynamicHolderSchema(),
			id = "dynamic-resolve",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = DynamicHolder(payload = RegistryFixtureDto(value = null)),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).contains("payload.value")
	}
	
	@Test
	@DisplayName("DynamicShape with unknown runtime class does not throw and adds no nested errors")
	fun unknownRuntimeClassIsOpaque() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.dynamicHolderSchema(),
			id = "dynamic-miss",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = DynamicHolder(payload = NeverSeenPayload("x")),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).isEmpty()
	}
	
	@Test
	@DisplayName("null DynamicShape value does not cascade")
	fun nullPayloadSkipsCascade() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.dynamicHolderSchema(),
			id = "dynamic-null",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = DynamicHolder(payload = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).isEmpty()
	}
	
	private class NeverSeenPayload(val label: String)
}
