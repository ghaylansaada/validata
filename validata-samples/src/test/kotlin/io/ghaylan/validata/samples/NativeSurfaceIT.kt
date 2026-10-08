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
package io.ghaylan.validata.samples

import io.ghaylan.validata.samples.config.SampleAppProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * Native / GraalVM surface contract for the sample app.
 *
 * Always runs on the JVM. Re-run under `./gradlew :validata-samples:nativeTest` when a GraalVM
 * JDK with `native-image` is installed — covers `@Validate` MVC + `@Validate` config properties.*
 * 
 * @author Ghaylan Saada
 */
@SpringBootTest
@AutoConfigureMockMvc
class NativeSurfaceIT(
	/** MockMvc against the sample Boot application.	 */
	@Autowired
	val mockMvc: MockMvc,
	/** Bound `sample.app` properties from `application.properties`.	 */
	@Autowired
	val sampleAppProperties: SampleAppProperties,
) {
	
	@Test
	@DisplayName("valid @Validate @ConfigurationProperties bean is bound at startup")
			/** valid @Validate @ConfigurationProperties bean is bound at startup			 */
	fun configPropertiesBound() {
		assertThat(sampleAppProperties.tenantId).isEqualTo("acme")
	}
	
	@Test
	@DisplayName("@Validate controller still rejects invalid body (request path)")
			/** @Validate controller still rejects invalid body (request path)			 */
	fun validateControllerRejectsInvalidBody() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"G","min_age":1,"max_age":2}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$.errors[?(@.path == 'first_name')].code") { value("TEXT_TOO_SHORT") }
			}
	}
	
	@Test
	@DisplayName("@Validate controller accepts a valid body")
			/** @Validate controller accepts a valid body			 */
	fun validateControllerAcceptsValidBody() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ada","min_age":3,"max_age":5}"""
		}
			.andExpect {
				status { isOk() }
				content { string("created Ada") }
			}
	}
}
