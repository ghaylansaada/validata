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
package io.ghaylan.validata.exception

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [ConstraintViolationException] carries [ConstraintError]s without a stack walk.
 * 
 * @author Ghaylan Saada
 */
class ConstraintViolationExceptionTest {
	
	@Test
	@DisplayName("exception exposes errors and a fixed message without a stack")
	fun exposesPayload() {
		val errors = listOf(
			ConstraintError(
				path = "email",
				code = ConstraintErrorCode.VALUE_MISSING,
				message = "required",
			),
		)
		val ex = ConstraintViolationException(errors = errors)
		assertThat(ex.errors).isEqualTo(errors)
		assertThat(ex.message).isEqualTo("Validation failed due to invalid input.")
		assertThat(ex.stackTrace).isEmpty()
	}
}
