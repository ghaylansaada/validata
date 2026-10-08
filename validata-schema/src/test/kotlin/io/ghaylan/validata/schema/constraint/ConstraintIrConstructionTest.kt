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
package io.ghaylan.validata.schema.constraint

import io.ghaylan.validata.schema.support.metadata.RequiredConfig
import io.ghaylan.validata.schema.support.metadata.RequiredRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Construction contracts for constraint IR payloads.
 *
 * Locks the path-free failure shape the engine depends on. Does not assert stub runner
 * pass/fail semantics — those belong in `validata-core`.
 *
 * @author Ghaylan Saada

 */
class ConstraintIrConstructionTest {

	@Test
	@DisplayName("ConstraintFailure defaults code/message/metadata to null")
	fun failureDefaults() {
		val failure = ConstraintFailure()

		assertThat(failure.code).isNull()
		assertThat(failure.message).isNull()
		assertThat(failure.metadata).isNull()
	}

	@Test
	@DisplayName("ConstraintFailure retains populated code, message, and metadata")
	fun failureRetainsPopulatedFields() {
		val failure = ConstraintFailure(
			code = "VALUE_MISSING",
			message = "required",
			metadata = mapOf("min" to 1),
		)

		assertThat(failure.code).isEqualTo("VALUE_MISSING")
		assertThat(failure.message).isEqualTo("required")
		@Suppress("UNCHECKED_CAST")
		assertThat(failure.metadata as Map<String, Any?>).containsEntry("min", 1)
	}

	@Test
	@DisplayName("ConstraintFailure has no path or location fields — those are engine wire concerns")
	fun failureHasNoPathOrLocationField() {
		// Java reflection only — this module intentionally has no kotlin-reflect dependency.
		val fieldNames = ConstraintFailure::class.java.declaredFields
			.map { it.name }
			.filterNot { it.startsWith("$") || it == "Companion" }

		assertThat(fieldNames).containsExactlyInAnyOrder("code", "message", "metadata")
		assertThat(fieldNames).doesNotContain("path")
		assertThat(fieldNames).doesNotContain("location")
	}

	@Test
	@DisplayName("CompiledConstraint retains metadata, runner, and declaration order")
	fun compiledConstraintRetainsFields() {
		val constraint = CompiledConstraint(
			metadata = RequiredConfig,
			runner = RequiredRunner,
			order = 3,
		)

		assertThat(constraint.metadata).isSameAs(RequiredConfig)
		assertThat(constraint.runner).isSameAs(RequiredRunner)
		assertThat(constraint.order).isEqualTo(3)
	}
}
