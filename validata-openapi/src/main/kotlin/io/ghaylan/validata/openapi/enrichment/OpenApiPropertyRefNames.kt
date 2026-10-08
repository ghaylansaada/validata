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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertyPath
import io.ghaylan.validata.schema.PropertySpec

/**
 * Rewrites `@PropertyRef` path segments to wire [PropertySpec.externalName]
 * for OpenAPI docs (`@JsonProperty` / naming strategy).*
 * 
 * @author Ghaylan Saada
 */
internal object OpenApiPropertyRefNames {
	
	/**
	 * Maps a single-segment property reference to its wire name on [schema].
	 *
	 * Accepts either [PropertySpec.declaredName] or [PropertySpec.externalName] as input (same as runtime).
	 *
	 * @param schema owner schema for resolution; when `null`, [name] is returned unchanged
	 * @param name referenced property spelling from the constraint annotation
	 * @return [PropertySpec.externalName] when found; otherwise [name]	 
	 */
	fun toWire(
		schema: ObjectSchema?,
		name: String
	): String {
		if (schema == null || name.isBlank()) return name
		return PropertyPath.findProperty(
			schema = schema,
			name = name
		)?.externalName ?: name
	}
}
