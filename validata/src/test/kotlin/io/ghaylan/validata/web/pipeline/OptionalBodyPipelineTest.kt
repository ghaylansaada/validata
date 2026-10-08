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

class OptionalBodyPipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("an omitted optional body is not validated as an empty object")
	fun omittedOptionalBody() {
		mockMvc.post("/api/optional-body/notes")
			.andExpect {
				status { isOk() }
				content { string("note null") }
			}
	}
	
	@Test
	@DisplayName("an optional body that is present is still validated")
	fun presentOptionalBody() {
		mockMvc.post("/api/optional-body/notes") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"text":"  "}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'text')].code") { value("TEXT_BLANK") }
			}
	}
}
