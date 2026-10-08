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
package io.ghaylan.validata.engine

import io.ghaylan.validata.engine.support.ConstraintErrorBuilder
import io.ghaylan.validata.engine.support.RequestErrorCollector
import io.ghaylan.validata.exception.ConstraintViolationException
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ErrorLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Business-error fluent path: [ConstraintErrorBuilder], [RequestErrorCollector], [ConstraintViolationException].
 *
 * @author Ghaylan Saada
 */
class RequestErrorCollectorTest {

	@Test
	@DisplayName("collector builds enriched constraint errors")
	fun collectEnrichedErrors() {
		val collector = RequestErrorCollector()
		collector.error("email", ConstraintErrorCode.VALUE_MISSING)
			.message("required")
			.metadata("hint", "missing")
		val errors = collector.collect()
		assertThat(errors).hasSize(1)
		assertThat(errors.single().path).isEqualTo("email")
		assertThat(errors.single().message).isEqualTo("required")
		assertThat(errors.single().metadata).isEqualTo(mapOf("hint" to "missing"))
		assertThat(collector.isEmpty()).isFalse
	}

	@Test
	@DisplayName("throwIfNotEmpty raises ConstraintViolationException with collected errors")
	fun throwWhenNotEmpty() {
		val collector = RequestErrorCollector()
		collector.error("x", ConstraintErrorCode.VALUE_MISSING)
		val ire = assertThrows<ConstraintViolationException> {
			collector.throwIfNotEmpty()
		}
		assertThat(ire.errors).hasSize(1)
	}

	@Test
	@DisplayName("throwIfNotEmpty is a no-op when empty")
	fun throwWhenEmptyIsNoOp() {
		RequestErrorCollector().throwIfNotEmpty()
	}

	@Test
	@DisplayName("constraint(Map) and constraint(lambda) merge; opaque constraint(Any) wins")
	fun constraintOverloads() {
		val mapBuilt = ConstraintErrorBuilder("a", ConstraintErrorCode.VALUE_MISSING).metadata(mapOf("k1" to 1))
			.metadata { mapOf("k2" to 2) }
			.build()
		assertThat(mapBuilt.metadata).isEqualTo(mapOf("k1" to 1, "k2" to 2))
		val opaque = ConstraintErrorBuilder("b", ConstraintErrorCode.VALUE_MISSING).metadata("ignored", "x")
			.metadata(listOf(1, 2, 3))
			.build()
		assertThat(opaque.metadata).isEqualTo(listOf(1, 2, 3))
	}

	@Test
	@DisplayName("location is preserved on build")
	fun locationOnBuild() {
		val error = ConstraintErrorBuilder("email", ConstraintErrorCode.VALUE_MISSING).location(ErrorLocation.QUERY)
			.build()
		assertThat(error.location).isEqualTo(ErrorLocation.QUERY)
	}

	@Test
	@DisplayName("collect on an empty collector returns an empty list")
	fun collectEmpty() {
		assertThat(RequestErrorCollector().collect()).isEmpty()
		assertThat(RequestErrorCollector().isEmpty()).isTrue
	}
}
