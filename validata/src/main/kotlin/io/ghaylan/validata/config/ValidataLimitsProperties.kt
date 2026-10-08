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
package io.ghaylan.validata.config

import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.engine.ValidatorEngine
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Spring Boot property binding for [ValidationLimits].
 *
 * Holds `@ConfigurationProperties` so the plain [ValidationLimits] data class stays Spring-free
 * for [ValidatorEngine] and other core callers. Bind under `validata.limits.*`, then call
 * [toLimits] to obtain the engine-facing value object (which re-validates positivity in its
 * `init` block).
 *
 * Must not be used as the engine constructor argument — always convert via [toLimits].
 *
 * ```yaml
 * validata:
 *   limits:
 *     max-depth: 32
 * 	   max-errors: 200
 *     max-elements-per-container: 10000
 * ```
 *
 * ```kotlin
 * val limits = ValidataLimitsProperties(maxDepth = 16).toLimits()
 * ```
 *
 * @property maxDepth Maximum object-graph depth walked in one run (`validata.limits.max-depth`);
 *   must be positive; default [ValidationLimits.DEFAULT_MAX_DEPTH].
 * @property maxElementsPerContainer Maximum entries traversed in one collection/array/map
 *   (`validata.limits.max-elements-per-container`); must be positive; default
 *   [ValidationLimits.DEFAULT_MAX_ELEMENTS_PER_CONTAINER].
 * @property maxErrors Maximum violations collected before the run stops
 *   (`validata.limits.max-errors`); must be positive; default [ValidationLimits.DEFAULT_MAX_ERRORS].*
 * 
 * @author Ghaylan Saada
 */
@ConfigurationProperties(prefix = "validata.limits")
data class ValidataLimitsProperties(
	val maxDepth: Int = ValidationLimits.DEFAULT_MAX_DEPTH,
	val maxElementsPerContainer: Int = ValidationLimits.DEFAULT_MAX_ELEMENTS_PER_CONTAINER,
	val maxErrors: Int = ValidationLimits.DEFAULT_MAX_ERRORS,
) {
	
	/**
	 * Builds a [ValidationLimits] snapshot from the bound property values.
	 *
	 * @return engine ceilings copied from this properties object
	 * @throws IllegalArgumentException if any value is non-positive	 
	 */
	fun toLimits(): ValidationLimits = ValidationLimits(maxDepth = maxDepth, maxErrors = maxErrors, maxElementsPerContainer = maxElementsPerContainer)
}
