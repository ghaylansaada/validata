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
import io.ghaylan.validata.schema.TwoRequiredFieldsDto
import io.ghaylan.validata.schema.runtime.SchemaNotFoundException
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.TwoFields
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

/**
 * Standalone [ValidatorEngine.validate] requires a generated schema for the root type.
 * 
 * @author Ghaylan Saada
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidatorEngineValidateTest {
	
	@BeforeAll
	fun resetSchemas() {
		GeneratedSchemas.resetForTests()
	}
	
	@AfterAll
	fun clearSchemas() {
		GeneratedSchemas.resetForTests()
	}
	
	@Test
	@DisplayName("validate() walks a generated ObjectSchema for the root type")
	fun validateStandalone() {
		val engine = EngineTestSupport.engine()
		val errors = engine.validate(
			params = RegistryFixtureDto(value = null),
			options = ValidationOptions(oneErrorPerParam = false),
		)
		assertThat(errors.map { it.path }).contains("value")
	}
	
	@Test
	@DisplayName("validate() with failFast=true stops after the first of two field violations")
	fun validateStandaloneFailFast() {
		val engine = EngineTestSupport.engine()
		val errors = engine.validate(
			params = TwoRequiredFieldsDto(left = null, right = null),
			options = ValidationOptions(failFast = true, oneErrorPerParam = false),
		)
		assertThat(errors).hasSize(1)
	}
	
	@Test
	@DisplayName("validate() with failFast=false collects both Required field violations")
	fun validateStandaloneCollectsAll() {
		val engine = EngineTestSupport.engine()
		val errors = engine.validate(
			params = TwoRequiredFieldsDto(left = null, right = null),
			options = ValidationOptions(failFast = false, oneErrorPerParam = false),
		)
		assertThat(errors.map { it.path }).containsExactlyInAnyOrder("left", "right")
	}
	
	@Test
	@DisplayName("validate() fails when no generated schema exists")
	fun validateMissingSchema() {
		val engine = EngineTestSupport.engine()
		assertThatThrownBy {
			engine.validate(TwoFields(left = "a", right = "b"))
		}.isInstanceOf(SchemaNotFoundException::class.java)
	}
}
