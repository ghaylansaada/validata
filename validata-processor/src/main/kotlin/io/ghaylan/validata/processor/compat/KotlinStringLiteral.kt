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
package io.ghaylan.validata.processor.compat

/**
 * Escapes a string so it is safe inside a generated Kotlin `"…"` literal.
 *
 * Covers `\`, `"`, `$` (string templates), and common control characters. All writers and
 * analyze-time literal renderers must use this — do not invent per-file escape helpers.*
 * 
 * @author Ghaylan Saada
 */
internal object KotlinStringLiteral {
	
	/**
	 * Escapes [value] for embedding between double quotes in generated source.
	 *
	 * Side effects: none.
	 *
	 * @param value Raw text to embed.
	 * @return Escaped content without surrounding quotes.	 
	 */
	fun escape(value: String): String {
		val out = StringBuilder(value.length + 8)
		for (ch in value) {
			when (ch) {
				'\\' -> out.append("\\\\")
				'"' -> out.append("\\\"")
				'$' -> out.append("\\$")
				'\n' -> out.append("\\n")
				'\r' -> out.append("\\r")
				'\t' -> out.append("\\t")
				else -> out.append(ch)
			}
		}
		return out.toString()
	}
}
