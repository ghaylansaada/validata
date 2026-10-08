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
package io.ghaylan.validata.openapi.support

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.schema.shape.TypeShape
import io.swagger.v3.oas.models.media.Schema

/**
 * Test-only mapper proving ServiceLoader extensibility (mirrors how apps map custom constraints).
 *
 * Does not overwrite [Schema.description] — Validata leaves that for `@Schema`.
 * Writes a **test-local** vendor key, not a framework public extension constant.
 * 
 * @author Ghaylan Saada
 */
class OddYearsProbeOpenApiConstraintMapper: OpenApiConstraintMapper {
	
	override fun apply(
		metadata: ConstraintMetadata,
		schema: Schema<*>,
		shape: TypeShape?,
	): Boolean {
		if (metadata !is OddYearsProbeConstraint) return false
		schema.addExtension(PROBE_EXTENSION, PROBE_VALUE)
		return true
	}
	
	companion object {
		
		/**
		 * Test-only OpenAPI extension key proving a custom mapper ran.
		 */
		const val PROBE_EXTENSION: String = "x-odd-years-probe"
		
		/**
		 * Marker value written under [PROBE_EXTENSION].
		 */
		const val PROBE_VALUE: String = "OddYearsProbe"
	}
}
