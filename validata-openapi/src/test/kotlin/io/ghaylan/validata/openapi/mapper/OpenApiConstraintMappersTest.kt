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
package io.ghaylan.validata.openapi.mapper

import io.ghaylan.validata.openapi.support.OddYearsProbeOpenApiConstraintMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards ServiceLoader discovery of [OpenApiConstraintMapper] providers.
 * 
 * @author Ghaylan Saada
 */
class OpenApiConstraintMappersTest {
	
	@BeforeEach
	fun reset() {
		OpenApiConstraintMappers.resetForTests()
	}
	
	@Test
	@DisplayName("ServiceLoader all() includes test-classpath probe mapper")
	fun allIncludesProbeMapper() {
		assertThat(OpenApiConstraintMappers.all()
			.map { it::class.java }).contains(OddYearsProbeOpenApiConstraintMapper::class.java)
	}
	
	@Test
	@DisplayName("resetForTests reloads after cache")
	fun resetReloads() {
		val first = OpenApiConstraintMappers.all()
		OpenApiConstraintMappers.resetForTests()
		val second = OpenApiConstraintMappers.all()
		assertThat(second.map { it::class.java }).isEqualTo(first.map { it::class.java })
	}
}
