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
package io.ghaylan.validata.samples.user

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * T-47 / Phase 9 acceptance: body, custom catalog constraint, cross-field refs, and flat params.
 *
 * Runs as a JVM MockMvc test always. The same assertions are the nativeTest contract when
 * GraalVM `native-image` is available (`./gradlew :validata-samples:nativeTest`).*
 * 
 * @author Ghaylan Saada
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationSamplesIT(
	/** MockMvc against the sample Boot application. */
	@Autowired val mockMvc: MockMvc,
) {

	@Test
	@DisplayName("T-47 · invalid payload is rejected with TEXT_TOO_SHORT and NUMBER_TOO_SMALL")
	/** T-47 · invalid payload is rejected with TEXT_TOO_SHORT and NUMBER_TOO_SMALL */
	fun rejectsKnownInvalidPayload() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"G","min_age":1,"max_age":2}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.status") { value(400) }
			jsonPath("$.message") { exists() }
			jsonPath("$.errors") { isArray() }
			jsonPath("$.errors[?(@.path == 'first_name')].code") { value("TEXT_TOO_SHORT") }
			jsonPath("$.errors[?(@.path == 'min_age')].code") { value("NUMBER_TOO_SMALL") }
		}
	}

	@Test
	@DisplayName("T-47 · missing required fields report VALUE_MISSING")
	/** T-47 · missing required fields report VALUE_MISSING */
	fun rejectsMissingRequired() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{}"""
		}.andExpect {
			status { isBadRequest() }
			// `name` is nullable + `@Required` → VALUE_MISSING. Non-null `Int` ages default to 0 under
			// Jackson and therefore do not fail presence.
			jsonPath("$.errors[?(@.path == 'first_name')].code") { value("VALUE_MISSING") }
		}
	}

	@Test
	@DisplayName("Phase 9 · even minAge fails OddYears from the sample ConstraintCatalog")
	/** Phase 9 · even minAge fails OddYears from the sample ConstraintCatalog */
	fun rejectsEvenMinAge() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ada","min_age":4,"max_age":9}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.errors[?(@.path == 'min_age')].code") { value("YEAR_NOT_ODD") }
		}
	}

	@Test
	@DisplayName("Phase 9 · maxAge must be greater than minAge")
	/** Phase 9 · maxAge must be greater than minAge */
	fun rejectsMaxAgeNotGreaterThanMinAge() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ada","min_age":5,"max_age":5}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.errors[?(@.path == 'max_age')].code") { exists() }
		}
	}

	@Test
	@DisplayName("T-47 · inclusive Min / Size boundaries are accepted")
	/** T-47 · inclusive Min / Size boundaries are accepted */
	fun acceptsBoundaryValues() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ada","min_age":3,"max_age":5}"""
		}.andExpect {
			status { isOk() }
			content { string("created Ada") }
		}
	}

	@Test
	@DisplayName("T-47 · valid payload reaches the controller")
	/** T-47 · valid payload reaches the controller */
	fun acceptsValidPayload() {
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"first_name":"Ghaylan","min_age":5,"max_age":30}"""
		}.andExpect {
			status { isOk() }
			content { string("created Ghaylan") }
		}
	}

	@Test
	@DisplayName("Phase 9 · query/header/path violations use path as the only locator")
	/** Phase 9 · query/header/path violations use path as the only locator */
	fun rejectsInvalidLookupParams() {
		mockMvc.get("/api/users/ab") {
			param("q", "x")
			header("X-Tenant", "t")
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.errors[?(@.path == 'userId')].code") { value("TEXT_TOO_SHORT") }
			jsonPath("$.errors[?(@.path == 'q')].code") { value("TEXT_TOO_SHORT") }
			jsonPath("$.errors[?(@.path == 'X-Tenant')].code") { value("TEXT_TOO_SHORT") }
		}
	}

	@Test
	@DisplayName("Phase 9 · valid lookup reaches the controller")
	/** Phase 9 · valid lookup reaches the controller */
	fun acceptsValidLookup() {
		mockMvc.get("/api/users/ada") {
			param("q", "hi")
			header("X-Tenant", "acme")
		}.andExpect {
			status { isOk() }
			content { string("user=ada q=hi tenant=acme") }
		}
	}

	@Test
	@DisplayName("Phase 3 · declared business error maps to envelope with empty errors")
	/** Phase 3 · declared business error maps to envelope with empty errors */
	fun businessErrorReturnsEnvelope() {
		mockMvc.get("/api/users/missing") {
			param("q", "hi")
			header("X-Tenant", "acme")
		}.andExpect {
			status { isNotFound() }
			jsonPath("$.status") { value(404) }
			jsonPath("$.code") { value("USER_NOT_FOUND") }
			jsonPath("$.message") { value("No user exists with the given id") }
			jsonPath("$.errors") { isEmpty() }
		}
	}
}
