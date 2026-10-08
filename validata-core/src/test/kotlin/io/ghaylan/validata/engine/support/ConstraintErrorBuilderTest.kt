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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ErrorLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [ConstraintErrorBuilder].
 *
 * @author Ghaylan Saada
 */
class ConstraintErrorBuilderTest {

	@Nested
	@DisplayName("path and message")
	inner class PathAndMessage {

		@Test
		@DisplayName("field sets path; blank message leaves message null for callers to fill")
		fun fieldAndDefaultMessage() {
			val error = ConstraintErrorBuilder("user.email", ConstraintErrorCode.VALUE_MISSING).build()
			assertThat(error.path).isEqualTo("user.email")
			assertThat(error.code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
			assertThat(error.message).isNull()
			assertThat(error.location).isNull()
		}

		@Test
		@DisplayName("message override is preserved on build")
		fun customMessage() {
			val error = ConstraintErrorBuilder("x", ConstraintErrorCode.VALUE_EMPTY).message("custom")
				.build()
			assertThat(error.message).isEqualTo("custom")
		}

		@Test
		@DisplayName("location is preserved on build")
		fun customLocation() {
			val error = ConstraintErrorBuilder("x", ConstraintErrorCode.VALUE_EMPTY).location(ErrorLocation.HEADER)
				.build()
			assertThat(error.location).isEqualTo(ErrorLocation.HEADER)
		}
	}

	@Nested
	@DisplayName("constraint")
	inner class ConstraintPayload {

		@Test
		@DisplayName("map-style constraint entries are retained")
		fun mapConstraint() {
			val error = ConstraintErrorBuilder("n", ConstraintErrorCode.NUMBER_TOO_SMALL).metadata("min", 1)
				.metadata("max", 10)
				.build()

			@Suppress("UNCHECKED_CAST")
			val meta = error.metadata as Map<String, Any?>
			assertThat(meta).containsEntry("min", 1)
				.containsEntry("max", 10)
		}

		@Test
		@DisplayName("opaque constraint() wins over map entries")
		fun opaqueConstraintWins() {
			val error = ConstraintErrorBuilder("n", ConstraintErrorCode.VALUE_NOT_ALLOWED).metadata("k", "v")
				.metadata("opaque-payload")
				.build()
			assertThat(error.metadata).isEqualTo("opaque-payload")
		}

		@Test
		@DisplayName("supplier constraint() merges into the constraint map")
		fun supplierConstraint() {
			val error = ConstraintErrorBuilder("t", ConstraintErrorCode.TEXT_TOO_SHORT).metadata { mapOf("min" to 3) }
				.build()

			@Suppress("UNCHECKED_CAST")
			val meta = error.metadata as Map<String, Any?>
			assertThat(meta).containsEntry("min", 3)
		}
	}
}
