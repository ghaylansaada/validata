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
package io.ghaylan.validata.openapi.docs

import java.math.BigDecimal

/**
 * Parses constraint bound / factor strings for native OpenAPI number facets.
 *
 * Used by Min / Max / MultipleOf documenters when mapping numeric bounds to native OpenAPI facets.*
 * 
 * @author Ghaylan Saada
 */
internal object NumericBound {
	
	/**
	 * Parses [raw] as a decimal after trim.
	 *
	 * @param raw constraint bound or factor string
	 * @return parsed value, or `null` when blank or not a number	 
	 */
	fun parse(raw: String): BigDecimal? {
		val trimmed = raw.trim()
		if (trimmed.isEmpty()) return null
		return try {
			BigDecimal(trimmed)
		} catch (_: NumberFormatException) {
			null
		}
	}
}
