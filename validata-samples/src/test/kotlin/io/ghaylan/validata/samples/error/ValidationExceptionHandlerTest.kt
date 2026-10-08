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
package io.ghaylan.validata.samples.error

import io.ghaylan.validata.exception.ConstraintViolationException
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * Unit tests for [ValidationExceptionHandler]: 400 constraint envelope and business mapping.*
 * 
 * @author Ghaylan Saada
 */
class ValidationExceptionHandlerTest {
	
	/** Handler under test — constructed directly, no Spring context.	 */
	private val handler = ValidationExceptionHandler()
	
	@Nested
	@DisplayName("ConstraintViolationException")
	inner class ConstraintViolation {
		
		@Test
		@DisplayName("HTTP 400 copies exception message and errors")
				/** HTTP 400 copies exception message and errors				 */
		fun copiesMessageAndErrors() {
			val errors = listOf(
				ConstraintError(code = ConstraintErrorCode.TEXT_TOO_SHORT, path = "first_name"),
			)
			val response = handler.onConstraintViolation(
				ConstraintViolationException(errors = errors),
			)
			assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
			assertThat(response.body).isNotNull
			assertThat(response.body!!.status).isEqualTo(400)
			assertThat(response.body!!.code).isNull()
			assertThat(response.body!!.message).isEqualTo("Validation failed due to invalid input.")
			assertThat(response.body!!.errors).isEqualTo(errors)
		}
	}
	
	@Nested
	@DisplayName("SampleBusinessException")
	inner class Business {
		
		@Test
		@DisplayName("maps catalog status, code, message, and empty errors")
				/** maps catalog status, code, message, and empty errors				 */
		fun mapsCatalogEnvelope() {
			val declared = SampleApiErrors.USER_NOT_FOUND
			val response = handler.onBusinessFailure(
				SampleBusinessException(declared.httpStatus, declared.code, declared.message),
			)
			assertThat(response.statusCode.value()).isEqualTo(404)
			assertThat(response.body!!.code).isEqualTo("USER_NOT_FOUND")
			assertThat(response.body!!.message).isEqualTo(declared.message)
			assertThat(response.body!!.errors).isEmpty()
		}
	}
}
