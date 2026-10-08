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
import org.springframework.test.web.servlet.get

class MultiValuePipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("element constraints apply to each element, container constraints to the list")
	fun elementAndContainerConstraints() {
		mockMvc.get("/api/method-level/tags") {
			param("tags", "ab")
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'tags')].code") { value("COLLECTION_TOO_SMALL") }
				jsonPath("$[?(@.path == 'tags[0]')].code") { value("TEXT_TOO_SHORT") }
			}
	}
	
	@Test
	@DisplayName("a list satisfying its own bounds is not judged by the element bounds")
	fun containerConstraintsDoNotLeakToElements() {
		mockMvc.get("/api/method-level/tags") {
			param("tags", "kotlin", "spring", "validation", "mvc-framework-x")
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'tags[3]')].code") { value("TEXT_TOO_LONG") }
				jsonPath("$[?(@.path == 'tags')]") { isEmpty() }
			}
	}
	
	@Test
	@DisplayName("a list valid at both levels reaches the controller")
	fun validMultiValueParam() {
		mockMvc.get("/api/method-level/tags") {
			param("tags", "kotlin", "spring")
		}
			.andExpect {
				status { isOk() }
				content { string("tags 2") }
			}
	}
}
