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

/**
 * Ceilings the engine applies to any single validation run.
 *
 * Request bodies are attacker-controlled and schemas expand lazily, so cost is bounded by the data,
 * not the model. Exceeding [maxDepth] or [maxElementsPerContainer] is a validation failure (reject),
 * not silent truncation — stopping early would fail open. [maxErrors] may stop collection early once
 * the request already has errors (safe by construction).
 *
 * Spring-free by design: [ValidatorEngine] takes this as a constructor default. Host modules own
 * property binding. Defaults sit above plausible legitimate payloads; raise only the limit you need.
 * A limit overrides a more permissive `@Size(max = …)` — the smaller of the two wins.
 *
 * @property maxDepth How far the engine descends into a nested graph before rejecting it. Guards the
 *    stack against cyclic and deeply nested payloads.
 * @property maxElementsPerContainer How many entries of a single collection, array, or map are
 *    traversed before the container is rejected. Guards CPU against very wide payloads.
 * @property maxErrors How many violations are collected before the run stops early. Guards heap and
 *    response size against payloads engineered to fail on every element.
 * @throws IllegalArgumentException When any property is not positive at construction.
 * 
 * @author Ghaylan Saada
 */
data class ValidationLimits(
	val maxDepth: Int = DEFAULT_MAX_DEPTH,
	val maxElementsPerContainer: Int = DEFAULT_MAX_ELEMENTS_PER_CONTAINER,
	val maxErrors: Int = DEFAULT_MAX_ERRORS
) {
	
	init {
		require(maxDepth > 0) { "validata.limits.max-depth must be positive, was $maxDepth" }
		require(maxElementsPerContainer > 0) { "validata.limits.max-elements-per-container must be positive, was $maxElementsPerContainer" }
		require(maxErrors > 0) { "validata.limits.max-errors must be positive, was $maxErrors" }
	}
	
	companion object {
		
		/**
		 * Default [maxDepth]: deep enough for hand-written models; beyond this is recursive data or
		 * an attack. Also keeps the worst case far from a stack overflow.
		 */
		const val DEFAULT_MAX_DEPTH = 32
		
		/**
		 * Default [maxElementsPerContainer]: large enough that ordinary lists never hit it; small
		 * enough that a hostile container is stopped early.
		 */
		const val DEFAULT_MAX_ELEMENTS_PER_CONTAINER = 10_000
		
		/**
		 * Default [maxErrors]: past a couple of hundred violations, an error response stops being
		 * useful to its reader.
		 */
		const val DEFAULT_MAX_ERRORS = 200
	}
}
