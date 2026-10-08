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
package io.ghaylan.validata.processor.naming

/**
 * Compile-time Jackson-style property naming strategies for wire / error-path names.
 *
 * Selected via KSP option `validata.jackson.naming`. Applied only when `@JsonProperty` is absent —
 * an explicit Jackson name always wins.
 *
 * [SNAKE_CASE] mirrors Jackson `PropertyNamingStrategies.SnakeCaseStrategy` (insert `_` before
 * uppercase runs, then lower-case).
 *
 * @author Ghaylan Saada
 */
enum class JacksonPropertyNaming {

	/** Leave the Kotlin property name unchanged (default). */
	IDENTITY,

	/** `firstName` → `first_name`, `HTTPServer` → `http_server`. */
	SNAKE_CASE,
	;

	/**
	 * Transforms a declared Kotlin property name into the wire / error-path spelling.
	 *
	 * Side effects: none.
	 *
	 * @param declaredName Kotlin property simple name
	 * @return wire name under this strategy
	 */
	fun translate(declaredName: String): String = when (this) {
		IDENTITY -> declaredName
		SNAKE_CASE -> toSnakeCase(declaredName)
	}

	companion object {

		/**
		 * Parses a KSP option value into a strategy.
		 *
		 * Accepts `IDENTITY` / `SNAKE_CASE` (case-insensitive). Unknown / blank → [IDENTITY].
		 *
		 * @param raw option value; may be null
		 * @return resolved strategy
		 */
		fun parse(raw: String?): JacksonPropertyNaming {
			if (raw.isNullOrBlank()) return IDENTITY
			return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
				?: IDENTITY
		}

		/**
		 * Jackson-compatible camelCase → snake_case.
		 *
		 * Side effects: none.
		 */
		internal fun toSnakeCase(input: String): String {
			if (input.isEmpty()) return input
			val result = StringBuilder(input.length * 2)
			var resultLength = 0
			var wasPrevTranslated = false
			for (i in input.indices) {
				var c = input[i]
				if (i > 0 || c != '_') {
					if (c.isUpperCase()) {
						if (!wasPrevTranslated && resultLength > 0 && result[resultLength - 1] != '_') {
							result.append('_')
							resultLength++
						}
						c = c.lowercaseChar()
						wasPrevTranslated = true
					} else {
						wasPrevTranslated = false
					}
					result.append(c)
					resultLength++
				}
			}
			return if (resultLength > 0) result.toString() else input
		}
	}
}
