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

class QueryPipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("query violations are reported under the parameter name")
	fun invalidQueryParam() {
		mockMvc.get("/api/method-level/search") {
			param("q", "ab")
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'q')].code") { value("TEXT_TOO_SHORT") }
			}
	}
	
	@Test
	@DisplayName("an omitted optional query parameter is not reported")
	fun omittedOptionalQueryParam() {
		mockMvc.get("/api/method-level/search") {
			param("q", "kotlin")
		}
			.andExpect {
				status { isOk() }
				content { string("searched kotlin") }
			}
	}
	
	@Test
	@DisplayName("an omitted required query parameter becomes VALUE_MISSING via the exception translator")
	fun omittedRequiredQueryParam() {
		mockMvc.get("/api/method-level/search")
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'q')].code") { value("VALUE_MISSING") }
				jsonPath("$[?(@.path == 'q')].location") { value("QUERY") }
			}
	}
	
	@Test
	@DisplayName("a query type mismatch on a @Required param becomes VALUE_TYPE_MISMATCH")
	fun queryTypeMismatch() {
		mockMvc.get("/api/method-level/limit") {
			param("limit", "not-an-int")
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'limit')].code") { value("VALUE_TYPE_MISMATCH") }
				jsonPath("$[?(@.path == 'limit')].location") { value("QUERY") }
			}
	}
}
