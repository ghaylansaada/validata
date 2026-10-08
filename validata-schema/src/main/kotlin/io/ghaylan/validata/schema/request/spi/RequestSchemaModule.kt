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
package io.ghaylan.validata.schema.request.spi

import io.ghaylan.validata.schema.request.EndpointSchema
import java.util.*

/**
 * One compilation unit's contribution of pre-built [EndpointSchema] graphs for `@Validate` endpoints.
 *
 * Discovered via [ServiceLoader]; [GeneratedRequestSchemas] merges modules and rejects duplicate
 * endpoint identifiers. Register implementations at `META-INF/services/` under this interface's binary name.
 * */
fun interface RequestSchemaModule {
	
	/**
	 * Endpoint schemas contributed by this module.
	 *
	 * No side effects required; typically returns a pre-built map.
	 *
	 * @return Map keyed by endpoint id (`pkg.Class#method(pkg.Type1,pkg.Type2)`), matching
	 *   `Method.getUniqueIdentifier()`	 
	 */
	fun schemas(): Map<String, EndpointSchema>
}
