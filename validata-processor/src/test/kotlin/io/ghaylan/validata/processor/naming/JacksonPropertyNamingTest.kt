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
package io.ghaylan.validata.processor.naming

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [JacksonPropertyNaming].
 *
 * @author Ghaylan Saada
 */
class JacksonPropertyNamingTest {

	@Test
	@DisplayName("IDENTITY leaves names unchanged")
	fun identity() {
		assertThat(JacksonPropertyNaming.IDENTITY.translate("firstName")).isEqualTo("firstName")
	}

	@Test
	@DisplayName("SNAKE_CASE mirrors Jackson camelCase → snake_case")
	fun snakeCase() {
		assertThat(JacksonPropertyNaming.SNAKE_CASE.translate("firstName")).isEqualTo("first_name")
		assertThat(JacksonPropertyNaming.SNAKE_CASE.translate("minAge")).isEqualTo("min_age")
		// Consecutive capitals stay glued (Jackson SnakeCaseStrategy).
		assertThat(JacksonPropertyNaming.SNAKE_CASE.translate("HTTPServer")).isEqualTo("httpserver")
		assertThat(JacksonPropertyNaming.SNAKE_CASE.translate("already_snake")).isEqualTo("already_snake")
	}

	@Test
	@DisplayName("parse accepts IDENTITY / SNAKE_CASE case-insensitively")
	fun parse() {
		assertThat(JacksonPropertyNaming.parse(null)).isEqualTo(JacksonPropertyNaming.IDENTITY)
		assertThat(JacksonPropertyNaming.parse("")).isEqualTo(JacksonPropertyNaming.IDENTITY)
		assertThat(JacksonPropertyNaming.parse("snake_case")).isEqualTo(JacksonPropertyNaming.SNAKE_CASE)
		assertThat(JacksonPropertyNaming.parse("nope")).isEqualTo(JacksonPropertyNaming.IDENTITY)
	}
}
