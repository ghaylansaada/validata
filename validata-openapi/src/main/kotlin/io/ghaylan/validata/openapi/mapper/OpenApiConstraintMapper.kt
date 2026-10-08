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
package io.ghaylan.validata.openapi.mapper

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.schema.shape.TypeShape
import io.swagger.v3.oas.models.media.Schema

/**
 * Advanced override for one [ConstraintMetadata] type that manipulates the swagger [Schema] directly.
 *
 * Prefer [ConstraintDocumentation] for facet docs. Use this SPI
 * when facets are insufficient (arbitrary schema surgery, vendor extensions, etc.).
 *
 * **Do not overwrite [Schema.description]** — that belongs to `@Schema` / authors.
 * Put Validata-specific prose and full IR under `x-validata-constraints` array entries instead.
 *
 * Register implementations via ServiceLoader (`META-INF/services/` + this interface's binary name).
 *
 * ```kotlin
 * class OddYearsMapper : OpenApiConstraintMapper {
 *     override fun apply(
 *         metadata: ConstraintMetadata,
 *         schema: Schema<*>,
 *         shape: TypeShape?,
 *     ): Boolean {
 *         if (metadata !is OddYearsConstraint) return false
 *         schema.addExtension("x-odd-years", true)
 *         return true
 *     }
 * }
 * ```
 * */
fun interface OpenApiConstraintMapper {
	
	/**
	 * Attempts to document [metadata] onto [schema].
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @param schema OpenAPI schema being enriched
	 * @param shape structural hint for shape-sensitive rules; may be `null` for flat parameters
	 * @return `true` if this mapper handled [metadata] (skip documenters for this constraint)	 
	 */
	fun apply(
		metadata: ConstraintMetadata,
		schema: Schema<*>,
		shape: TypeShape?,
	): Boolean
}
