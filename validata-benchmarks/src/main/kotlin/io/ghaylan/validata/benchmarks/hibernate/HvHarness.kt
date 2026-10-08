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
package io.ghaylan.validata.benchmarks.hibernate

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

/**
 * Warmed Hibernate Validator factory for JMH and fixture parity tests.
 *
 * Builds the [ValidatorFactory] **once** at construction time. Rebuilding the factory inside a
 * benchmark loop would measure metadata bootstrap, not steady-state validation.
 *
 * ## Thread safety
 *
 * A Jakarta [Validator] is thread-safe; one harness may be shared across JMH worker threads.
 *
 * ```kotlin
 * HvHarness().use { harness ->
 *     val violations = harness.validate(SizedPayloads.hvSmallValid())
 *     check(violations.isEmpty())
 * }
 * ```*
 * 
 * @author Ghaylan Saada
 */
class HvHarness: AutoCloseable {
	
	private val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
	
	/**
	 * Cached validator used for every measured call.	 
	 */
	val validator: Validator = factory.validator
	
	/**
	 * Validates [target] with Hibernate Validator defaults (all constraints, all groups).
	 *
	 * @param target annotated bean graph
	 * @return violation set; empty when valid	 
	 */
	fun validate(target: Any): Set<ConstraintViolation<Any>> = validator.validate(target)
	
	override fun close() {
		factory.close()
	}
}
