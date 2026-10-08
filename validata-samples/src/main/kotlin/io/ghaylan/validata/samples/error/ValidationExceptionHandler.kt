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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import tools.jackson.module.kotlin.KotlinInvalidNullException

/**
 * App-owned mapping: Validata throws [ConstraintViolationException]; the host chooses the JSON shape.
 * 
 * @author Ghaylan Saada
 */
@RestControllerAdvice
class ValidationExceptionHandler {
	
	/**
	 * Maps a request-constraint failure to HTTP 400 with [SampleErrorBody.errors] populated.
	 *
	 * Apps own serialization: strip [ConstraintError.metadata] (live `ConstraintMetadata` is not
	 * Jackson-safe) or project public args to a Map before returning JSON.
	 *
	 * @param ex failure thrown by the Validata MVC host after argument binding
	 * @return 400 response whose [SampleErrorBody.message] is [ConstraintViolationException.message]
	 */
	@ExceptionHandler(ConstraintViolationException::class)
	fun onConstraintViolation(ex: ConstraintViolationException): ResponseEntity<SampleErrorBody> {
		val body = SampleErrorBody(
			status = HttpStatus.BAD_REQUEST.value(),
			code = null,
			message = ex.message ?: "Request failed Validata constraints.",
			// Host-owned wire shape: omit metadata so Jackson does not walk ConstraintMetadata / KClass.
			errors = ex.errors.map { it.copy(metadata = null) })
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(body)
	}
	
	@ExceptionHandler(KotlinInvalidNullException::class)
	fun onInvalidNullException(ex: KotlinInvalidNullException): ResponseEntity<SampleErrorBody> {
		
		val error = ConstraintError(
			path = ex.propertyName.fullName.simpleName,
			code = ConstraintErrorCode.VALUE_MISSING,
			message = ConstraintErrorCode.VALUE_MISSING.message)
		
		val body = SampleErrorBody(
			status = HttpStatus.BAD_REQUEST.value(),
			code = null,
			message = ex.message ?: "Request failed Validata constraints.",
			errors = listOf(error))
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(body)
	}
	
	/**
	 * Maps a sample business failure to the catalog HTTP status with an empty `errors` array.
	 *
	 * @param ex application-thrown failure (not a Validata constraint violation)
	 * @return response whose status, code, and message come from [SampleBusinessException]
	 */
	@ExceptionHandler(SampleBusinessException::class)
	fun onBusinessFailure(ex: SampleBusinessException): ResponseEntity<SampleErrorBody> {
		val body = SampleErrorBody(
			status = ex.httpStatus,
			code = ex.code,
			message = ex.message ?: ex.code,
			errors = emptyList())
		
		return ResponseEntity.status(ex.httpStatus)
			.body(body)
	}
}
