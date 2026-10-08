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

import io.ghaylan.validata.engine.ValidationCursor
import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Direct unit tests for [ValidationLimitGuards].
 *
 * @author Ghaylan Saada
 */
class ValidationLimitGuardsTest {

	private fun rootAtDepth(depth: Int): ValidationCursor {
		val cursor = ValidationCursor.root(
			oneErrorPerParam = false,
			groups = setOf(OnDefault::class),
		)
		repeat(depth) { i ->
			cursor.pushProperty("p$i", null, null, null)
		}
		return cursor
	}

	@Nested
	@DisplayName("rejectIfTooDeep")
	inner class RejectIfTooDeep {

		@Test
		@DisplayName("depth <= maxDepth returns false and leaves errors empty")
		fun withinCeiling() {
			val limits = ValidationLimits(maxDepth = 3)
			val errors = mutableListOf<ConstraintError<*>>()
			val cursor = rootAtDepth(3)

			assertThat(ValidationLimitGuards.rejectIfTooDeep(limits, cursor, errors)).isFalse()
			assertThat(errors).isEmpty()
		}

		@Test
		@DisplayName("depth > maxDepth adds STRUCTURE_DEPTH_EXCEEDED and returns true")
		fun exceedsCeiling() {
			val limits = ValidationLimits(maxDepth = 2)
			val errors = mutableListOf<ConstraintError<*>>()
			val cursor = rootAtDepth(3)

			assertThat(ValidationLimitGuards.rejectIfTooDeep(limits, cursor, errors)).isTrue()
			assertThat(errors).hasSize(1)
			assertThat(errors.single().code).isEqualTo(ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED)
			assertThat(errors.single().metadata).isNull()
			assertThat(errors.single().message).contains("2")
		}
	}

	@Nested
	@DisplayName("rejectIfTooLarge")
	inner class RejectIfTooLarge {

		@Test
		@DisplayName("size == ceiling returns false")
		fun atCeiling() {
			val limits = ValidationLimits(maxElementsPerContainer = 5)
			val errors = mutableListOf<ConstraintError<*>>()
			val cursor = ValidationCursor.root(
				oneErrorPerParam = false,
				groups = setOf(OnDefault::class),
			)

			assertThat(ValidationLimitGuards.rejectIfTooLarge(limits, 5, cursor, errors)).isFalse()
			assertThat(errors).isEmpty()
		}

		@Test
		@DisplayName("size > ceiling adds COLLECTION_TOO_LARGE")
		fun overCeiling() {
			val limits = ValidationLimits(maxElementsPerContainer = 5)
			val errors = mutableListOf<ConstraintError<*>>()
			val cursor = ValidationCursor.root(
				oneErrorPerParam = false,
				groups = setOf(OnDefault::class),
			)

			assertThat(ValidationLimitGuards.rejectIfTooLarge(limits, 6, cursor, errors)).isTrue()
			assertThat(errors.single().code).isEqualTo(ConstraintErrorCode.COLLECTION_TOO_LARGE)
			assertThat(errors.single().metadata).isNull()
			assertThat(errors.single().message).contains("6")
				.contains("5")
		}
	}

	@Nested
	@DisplayName("shouldAbortWalk")
	inner class ShouldAbortWalk {

		@Test
		@DisplayName("failFast with a non-empty error list aborts")
		fun failFastNonEmpty() {
			val limits = ValidationLimits(maxErrors = 200)
			val errors = mutableListOf<ConstraintError<*>>(
				ConstraintError(code = ConstraintErrorCode.VALUE_MISSING),
			)
			assertThat(ValidationLimitGuards.shouldAbortWalk(limits, errors, failFast = true)).isTrue()
		}

		@Test
		@DisplayName("errors.size >= maxErrors aborts even without failFast")
		fun maxErrorsReached() {
			val limits = ValidationLimits(maxErrors = 2)
			val errors = listOf(
				ConstraintError(code = ConstraintErrorCode.VALUE_MISSING),
				ConstraintError(code = ConstraintErrorCode.VALUE_MISSING),
			)
			assertThat(ValidationLimitGuards.shouldAbortWalk(limits, errors, failFast = false)).isTrue()
		}

		@Test
		@DisplayName("neither failFast nor maxErrors trip returns false")
		fun neither() {
			val limits = ValidationLimits(maxErrors = 10)
			val errors = listOf(ConstraintError(code = ConstraintErrorCode.VALUE_MISSING))
			assertThat(ValidationLimitGuards.shouldAbortWalk(limits, errors, failFast = false)).isFalse()
			assertThat(ValidationLimitGuards.shouldAbortWalk(limits, emptyList(), failFast = true)).isFalse()
		}
	}
}
