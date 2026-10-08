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
package io.ghaylan.validata.openapi.aot

import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.presentation.ApiError
import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates

class ValidataOpenApiRuntimeHintsTest {
	
	@Test
	@DisplayName("registers ServiceLoader resources for docs SPI and OpenAPI mapper SPI")
	fun registersServiceLoaderPatterns() {
		val hints = RuntimeHints()
		ValidataOpenApiRuntimeHints().registerHints(hints, javaClass.classLoader)
		assertThat(
			RuntimeHintsPredicates.resource()
				.forResource(
					"META-INF/services/" + ConstraintDocumentation::class.java.name,
				)
				.test(hints),
		).isTrue()
		assertThat(
			RuntimeHintsPredicates.resource()
				.forResource(
					"META-INF/services/" + OpenApiConstraintMapper::class.java.name,
				)
				.test(hints),
		).isTrue()
	}
	
	@Test
	@DisplayName("registers reflection for presentation annotations and ErrorDocPublisher")
	fun registersPresentationAnnotationHints() {
		val hints = RuntimeHints()
		ValidataOpenApiRuntimeHints().registerHints(hints, javaClass.classLoader)
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(ApiError::class.java)
			.test(hints)).isTrue()
		assertThat(
			RuntimeHintsPredicates.reflection()
				.onType(ErrorDocPublisher::class.java)
				.test(hints),
		).isTrue()
	}
	
	@Test
	@DisplayName("registers reflection for built-in constraint metadata used by x-validata-constraints")
	fun registersConstraintMetadataHints() {
		val hints = RuntimeHints()
		ValidataOpenApiRuntimeHints().registerHints(hints, javaClass.classLoader)
		assertThat(
			RuntimeHintsPredicates.reflection()
				.onType(io.ghaylan.validata.constraint.annotation.SizeConstraint::class.java)
				.test(hints),
		).isTrue()
		assertThat(
			RuntimeHintsPredicates.reflection()
				.onType(io.ghaylan.validata.constraint.ConstraintMetadata::class.java)
				.test(hints),
		).isTrue()
	}
}
