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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class ClassLevelValidatePipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("validates the body instead of failing with a 500")
	fun classLevelBody() {
		mockMvc.post("/api/class-level/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"G","age":12}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'name')].code") { value("TEXT_TOO_SHORT") }
			}
	}
	
	@Test
	@DisplayName("validates query parameters too")
	fun classLevelQuery() {
		mockMvc.get("/api/class-level/search")
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'q')].code") { value("VALUE_MISSING") }
			}
	}
	
	@Test
	@DisplayName("a valid request reaches the controller")
	fun classLevelValid() {
		mockMvc.post("/api/class-level/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"Ghaylan","age":30}"""
		}
			.andExpect {
				status { isOk() }
			}
	}
	
	@Test
	@DisplayName("a method-level declaration overrides the class-level fail-fast policy")
	fun methodOverridesClass() {
		mockMvc.post("/api/class-level/bulk-check") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":null,"age":null}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$.length()") { value(1) }
				jsonPath("$[0].path") { value("name") }
			}
	}
	
	@Test
	@DisplayName("class-level @Validate still translates Jackson body type mismatches")
	fun classLevelJacksonTypeMismatch() {
		mockMvc.post("/api/class-level/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"Ghaylan","age":"not-a-number"}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'age')].code") { value("VALUE_TYPE_MISMATCH") }
				jsonPath("$[?(@.path == 'age')].location") { value("BODY") }
			}
	}
}
