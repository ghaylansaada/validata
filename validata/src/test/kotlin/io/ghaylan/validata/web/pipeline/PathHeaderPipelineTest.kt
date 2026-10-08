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

class PathHeaderPipelineTest: PipelineIntegrationTest() {
	
	@Test
	@DisplayName("path variable violations are reported under the variable name")
	fun invalidPathVariable() {
		mockMvc.get("/api/method-level/orders/abc") {
			header("X-Tenant", "acme")
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'orderId')].code") { value("TEXT_TOO_SHORT") }
			}
	}
	
	@Test
	@DisplayName("header violations are reported under the header name")
	fun invalidHeader() {
		mockMvc.get("/api/method-level/orders/ORD-1234") {
			header("X-Tenant", "a")
		}
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'X-Tenant')].code") { value("TEXT_TOO_SHORT") }
			}
	}
	
	@Test
	@DisplayName("a fully valid path and header request reaches the controller")
	fun validPathAndHeader() {
		mockMvc.get("/api/method-level/orders/ORD-1234") {
			header("X-Tenant", "acme")
		}
			.andExpect {
				status { isOk() }
				content { string("order ORD-1234") }
			}
	}
	
	@Test
	@DisplayName("a missing required header becomes VALUE_MISSING via the exception translator")
	fun missingRequiredHeader() {
		mockMvc.get("/api/method-level/orders/ORD-1234")
			.andExpect {
				status { isBadRequest() }
				jsonPath("$[?(@.path == 'X-Tenant')].code") { value("VALUE_MISSING") }
				jsonPath("$[?(@.path == 'X-Tenant')].location") { value("HEADER") }
			}
	}
}
