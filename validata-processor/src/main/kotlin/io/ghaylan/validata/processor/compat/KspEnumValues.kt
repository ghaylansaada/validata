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

import com.google.devtools.ksp.symbol.KSType

/**
 * Maps KSP annotation enum arguments onto classpath enum types by [Enum.name].
 *
 * KSP may surface an enum constant as a [KSType], an FQCN string, or an opaque value whose
 * `toString` ends with `.ENTRY`. Call sites must not hard-code entry-name tables.*
 * 
 * @author Ghaylan Saada
 */
internal object KspEnumValues {
	
	/**
	 * Extracts the enum entry simple name from a KSP argument value.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value from KSP; may be null.
	 * @return Entry name (e.g. `ELEMENT`), or `null` when absent or blank.	 
	 */
	fun entryName(raw: Any?): String? =
		when (raw) {
			null -> null
			is KSType -> raw.declaration.simpleName.asString()
			is String -> raw.substringAfterLast('.').takeIf { it.isNotBlank() }
			is Enum<*> -> raw.name
			else -> raw.toString().substringAfterLast('.').takeIf { it.isNotBlank() }
		}
	
	/**
	 * Resolves [raw] to an [E] constant whose [Enum.name] matches [entryName].
	 *
	 * Side effects: none.
	 *
	 * @param E Target enum on the processor classpath (typically validata-schema).
	 * @param raw KSP argument value; may be null.
	 * @return Matching constant, or `null` when the name is absent or unknown.	 
	 */
	inline fun <reified E: Enum<E>> parse(raw: Any?): E? {
		val name = entryName(raw)
			?: return null
		return enumValues<E>().firstOrNull { it.name == name }
	}
}
