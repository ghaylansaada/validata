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

class JacksonNamingPipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("@JsonProperty wire name appears in error paths")
	fun jsonPropertyWireNameInPaths() {
		mockMvc.post("/api/naming/profiles") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"G","surname":"Saada"}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'first_name')].code") { value("TEXT_TOO_SHORT") }
				jsonPath("$[?(@.path == 'firstName')]") { isEmpty() }
			}
	}
	
	@Test
	@DisplayName("@JsonProperty wins over the Kotlin property name")
	fun explicitNameWins() {
		mockMvc.post("/api/naming/profiles") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ghaylan"}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'surname')].code") { value("VALUE_MISSING") }
				jsonPath("$[?(@.path == 'last_name')]") { isEmpty() }
			}
	}
	
	@Test
	@DisplayName("a @JsonIgnore property is not validated")
	fun ignoredPropertyIsNotValidated() {
		mockMvc.post("/api/naming/profiles") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ghaylan","surname":"Saada"}"""
		}
			.andExpect {
				status { isOk() }
			}
	}
}
