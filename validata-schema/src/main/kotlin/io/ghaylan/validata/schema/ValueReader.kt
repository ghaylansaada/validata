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
package io.ghaylan.validata.schema

/**
 * Reads one property value from a non-null container instance.
 *
 * Producers supply bytecode (typically a cast + field/property access); the engine only calls [read].
 *
 * @author Ghaylan Saada
 */
fun interface ValueReader {
	
	/**
	 * Extracts the property from [container].
	 *
	 * Does not mutate [container].
	 *
	 * @param container Object or map that owns the property; never null at the call site
	 * @return Property value, or `null` when absent / explicitly null	 
	 */
	fun read(container: Any): Any?
}
