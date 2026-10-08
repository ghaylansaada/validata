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
package io.ghaylan.validata.aot

import io.ghaylan.validata.constraint.spi.ConstraintCatalog
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.request.spi.RequestSchemaModule
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.schema.spi.ObjectSchemaModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.getTypeHint
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates

/**
 * Checks that [ValidationRuntimeHints] registers the reflection types and ServiceLoader
 * resources the native-image path needs.
 * 
 * @author Ghaylan Saada
 */
class ValidationRuntimeHintsTest {
	
	@Test
	@DisplayName("registers error JSON + SPI entry types for reflection")
	fun registerHintsPopulatesReflection() {
		val hints = RuntimeHints()
		ValidationRuntimeHints().registerHints(hints, classLoader = null)
		val reflection = hints.reflection()
		assertThat(reflection.getTypeHint<ConstraintError<*>>()).isNotNull
		assertThat(reflection.getTypeHint<ConstraintErrorCode>()).isNotNull
		assertThat(reflection.getTypeHint<ObjectSchemaModule>()).isNotNull
		assertThat(reflection.getTypeHint<GeneratedSchemas>()).isNotNull
		assertThat(reflection.getTypeHint<ConstraintCatalog>()).isNotNull
		assertThat(reflection.getTypeHint<ObjectSchema>()).isNotNull
		assertThat(reflection.getTypeHint<RequestSchemaModule>()).isNotNull
	}
	
	@Test
	@DisplayName("registers META-INF/services patterns for ObjectSchemaModule, ConstraintCatalog, RequestSchemaModule")
	fun registersServiceLoaderPatterns() {
		val hints = RuntimeHints()
		ValidationRuntimeHints().registerHints(hints, javaClass.classLoader)
		
		assertThat(
			RuntimeHintsPredicates.resource()
				.forResource("META-INF/services/" + ObjectSchemaModule::class.java.name)
				.test(hints),
		).isTrue()
		
		assertThat(
			RuntimeHintsPredicates.resource()
				.forResource("META-INF/services/" + ConstraintCatalog::class.java.name)
				.test(hints),
		).isTrue()
		
		assertThat(
			RuntimeHintsPredicates.resource()
				.forResource("META-INF/services/" + RequestSchemaModule::class.java.name)
				.test(hints),
		).isTrue()
	}
}
