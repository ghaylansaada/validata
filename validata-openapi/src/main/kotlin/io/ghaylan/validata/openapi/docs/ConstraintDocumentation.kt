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

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * ServiceLoader SPI: maps a [ConstraintMetadata] occurrence to [ConstraintDocHints].
 *
 * Prefer this SPI for native JSON Schema facets. Use `io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper`
 * when the swagger
 * `io.swagger.v3.oas.models.media.Schema` itself must be mutated (vendor extensions, arbitrary surgery).
 *
 * Register implementations via ServiceLoader (`META-INF/services/` + this interface's binary name).
 * Built-in documenters ship from this module.
 *
 * ```kotlin
 * class SizeDocs : ConstraintDocumentation {
 *     override fun supports(metadata: ConstraintMetadata): Boolean =
 *         metadata is SizeConstraint
 *
 *     override fun hints(metadata: ConstraintMetadata, shape: TypeShape?): ConstraintDocHints =
 *         ConstraintDocHints(facets = setOf(JsonSchemaFacet.MinLength(1)))
 * }
 * ```*
 * 
 * @author Ghaylan Saada
 */
interface ConstraintDocumentation {
	
	/**
	 * Whether this documenter owns [metadata].
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @return `true` when this documenter should supply hints	 
	 */
	fun supports(metadata: ConstraintMetadata): Boolean
	
	/**
	 * Builds documentation hints for [metadata].
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @param shape structural hint for shape-sensitive rules (e.g. [SizeConstraint] on string vs list); may be `null`
	 * @return JSON Schema facets for enrichment, or empty when there is no native equivalent	 
	 */
	fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints
}
