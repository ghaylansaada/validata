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
package io.ghaylan.validata.web.pipeline

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class BodyPipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("a valid body reaches the controller")
	fun validBody() {
		mockMvc.post("/api/method-level/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"Ghaylan","age":30}"""
		}
			.andExpect {
				status { isOk() }
				content { string("created Ghaylan") }
			}
	}
	
	@Test
	@DisplayName("body violations are reported with the property path")
	fun invalidBody() {
		mockMvc.post("/api/method-level/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"G","age":12}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'name')].code") { value("TEXT_TOO_SHORT") }
				jsonPath("$[?(@.path == 'age')].code") { value("NUMBER_TOO_SMALL") }
			}
	}
	
	@Test
	@DisplayName("a missing required body property is reported as missing")
	fun missingBodyProperty() {
		mockMvc.post("/api/method-level/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"age":30}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'name')].code") { value("VALUE_MISSING") }
			}
	}
}
