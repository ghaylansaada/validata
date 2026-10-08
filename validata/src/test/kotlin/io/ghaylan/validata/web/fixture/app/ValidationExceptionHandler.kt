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
package io.ghaylan.validata.web.fixture.app

import io.ghaylan.validata.exception.ConstraintViolationException
import io.ghaylan.validata.model.ConstraintError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Renders [ConstraintViolationException] as HTTP 400 with a Jackson-safe error list.
 *
 * Strips [ConstraintError.metadata] — generated `*Constraint` instances are not wire-serializable.
 * 
 * @author Ghaylan Saada
 */
@RestControllerAdvice
class ValidationExceptionHandler {
	
	@ExceptionHandler(ConstraintViolationException::class)
	fun onRequestValidationFailure(ex: ConstraintViolationException): ResponseEntity<List<ConstraintError<*>>> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ex.errors.map { it.copy(metadata = null) })
}
