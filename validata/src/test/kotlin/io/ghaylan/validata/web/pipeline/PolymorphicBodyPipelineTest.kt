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

class PolymorphicBodyPipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("a sealed-interface @RequestBody is validated via the runtime subtype")
	fun polymorphicBodyValidated() {
		mockMvc.post("/api/uploads") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"context":"PROFILE_AVATAR","content_type":null,"display_name":"avatar.png"}"""
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'content_type')].code") { value("VALUE_MISSING") }
			}
	}
	
	@Test
	@DisplayName("a valid polymorphic body reaches the controller")
	fun polymorphicBodyValid() {
		mockMvc.post("/api/uploads") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"context":"PROFILE_AVATAR","content_type":"image/png","display_name":"avatar.png"}"""
		}
			.andExpect {
				status { isOk() }
				content { string("uploaded avatar.png") }
			}
	}
}
