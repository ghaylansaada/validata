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
package io.ghaylan.validata.processor

import com.google.devtools.ksp.processing.KSPLogger
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming
import io.ghaylan.validata.processor.verify.PropertyReferenceVerifier

/**
 * KSP `environment.options` keys understood by Validata processors.
 *
 * Pass via Gradle:
 * ```kotlin
 * ksp {
 *     arg("validata.strictCrossModuleCascade", "true")
 *     arg("validata.maxShapeNestingDepth", "32")
 *     arg("validata.jackson.naming", "SNAKE_CASE")
 * }
 * ```
 *
 * Read by [SchemaProcessor] (via shape / property-ref builders).*
 * 
 * @author Ghaylan Saada
 */
internal object ProcessorOptions {
	
	/**
	 * When `true`, unmarked cascade into a dependency type (no `containingFile`) is a KSP **error**.
	 * Applies to nested property shapes and unmarked cross-module `@RequestBody` types.
	 * Default `false`: warn only (declaring module must run KSP).	 
	 */
	const val STRICT_CROSS_MODULE_CASCADE = "validata.strictCrossModuleCascade"
	
	/**
	 * Maximum iterable/map nesting depth when verifying type-use property references.
	 * Default [PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH].	 
	 */
	const val MAX_SHAPE_NESTING_DEPTH = "validata.maxShapeNestingDepth"

	/**
	 * Jackson-style wire naming when `@JsonProperty` is absent: `IDENTITY` (default) or `SNAKE_CASE`.
	 * Align with the app `ObjectMapper` naming strategy.
	 */
	const val JACKSON_NAMING = "validata.jackson.naming"
	
	/**
	 * Reads [STRICT_CROSS_MODULE_CASCADE] from KSP options.
	 *
	 * @param options `SymbolProcessorEnvironment.options` map (case-insensitive `"true"` enables)
	 * @return `true` when strict cross-module cascade errors are enabled	 
	 */
	fun strictCrossModuleCascade(options: Map<String, String>): Boolean =
		options[STRICT_CROSS_MODULE_CASCADE]?.equals("true", ignoreCase = true) == true
	
	/**
	 * Reads [MAX_SHAPE_NESTING_DEPTH] from KSP options.
	 *
	 * Invalid or negative values fall back to the default and optionally log a warning via [logger].
	 *
	 * @param options `SymbolProcessorEnvironment.options` map
	 * @param logger when non-null, warns on invalid values instead of failing silently
	 * @return parsed non-negative int, or the default when missing / invalid	 
	 */
	fun maxShapeNestingDepth(
		options: Map<String, String>,
		logger: KSPLogger? = null,
	): Int {
		val raw = options[MAX_SHAPE_NESTING_DEPTH]
			?: return PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH
		val parsed = raw.toIntOrNull()
		return if (parsed != null && parsed >= 0) {
			parsed
		}
		else {
			logger?.warn("Invalid KSP option '$MAX_SHAPE_NESTING_DEPTH=$raw' — " +
					"expected a non-negative integer; using default " +
					"${PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH}.")
			PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH
		}
	}

	/**
	 * Reads [JACKSON_NAMING] from KSP options.
	 *
	 * Unknown values fall back to [JacksonPropertyNaming.IDENTITY] and optionally warn.
	 *
	 * @param options `SymbolProcessorEnvironment.options` map
	 * @param logger when non-null, warns on unrecognized values
	 * @return resolved naming strategy
	 */
	fun jacksonNaming(
		options: Map<String, String>,
		logger: KSPLogger? = null,
	): JacksonPropertyNaming {
		val raw = options[JACKSON_NAMING] ?: return JacksonPropertyNaming.IDENTITY
		val parsed = JacksonPropertyNaming.parse(raw)
		if (logger != null &&
			raw.isNotBlank() &&
			JacksonPropertyNaming.entries.none { it.name.equals(raw.trim(), ignoreCase = true) }
		) {
			logger.warn(
				"Invalid KSP option '$JACKSON_NAMING=$raw' — expected IDENTITY or SNAKE_CASE; " +
					"using IDENTITY.",
			)
		}
		return parsed
	}
}
