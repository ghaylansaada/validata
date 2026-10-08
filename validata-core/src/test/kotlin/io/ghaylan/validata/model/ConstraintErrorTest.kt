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
package io.ghaylan.validata.model

import io.ghaylan.validata.constraint.ConstraintValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Error model copy / code defaults used by [ConstraintValidator.runValidation] enrichment.
 *
 * @author Ghaylan Saada
 */
class ConstraintErrorTest {

	@Test
	@DisplayName("copy preserves metadata and replaces path and message")
	fun copyReplacesPathAndMessage() {
		val original = ConstraintError(
			path = null,
			code = ConstraintErrorCode.VALUE_MISSING,
			message = "raw",
			metadata = mapOf("k" to 1),
		)
		val enriched = original.copy(
			path = "user.email",
			message = "please provide",
		)
		assertThat(enriched.path).isEqualTo("user.email")
		assertThat(enriched.message).isEqualTo("please provide")
		assertThat(enriched.code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		assertThat(enriched.metadata).isEqualTo(mapOf("k" to 1))
	}

	@Test
	@DisplayName("each ConstraintErrorCode exposes a non-blank default message and code equals name")
	fun codesHaveDefaultMessages() {
		for (code in ConstraintErrorCode.entries) {
			assertThat(code.message).withFailMessage { "$code has blank default message" }
				.isNotBlank()
			assertThat(code.code).isEqualTo(code.name)
		}
	}
}
