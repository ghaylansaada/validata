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
package io.ghaylan.validata.openapi.docs.builtin

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Maps cross-field constraint metadata; emits no native OpenAPI facets.
 *
 * Args are published under `x-validata-constraints` by
 * `io.ghaylan.validata.openapi.enrichment.PropertyOpenApiExtensionsWriter`
 * (every constraint gets an array entry).*
 * 
 * @author Ghaylan Saada
 */
class CrossFieldConstraintDocumentation: ConstraintDocumentation {
	
	/**
	 * Reports whether [metadata] is handled by this documenter.
	 *
	 * @param metadata constraint metadata under consideration
	 * @return `true` for Compare or RequiredWhen metadata	 
	 */
	override fun supports(metadata: ConstraintMetadata): Boolean {
		return metadata is CompareConstraint || metadata is RequiredWhenConstraint
	}
	
	/**
	 * Builds OpenAPI facets and extension hints for [metadata].
	 *
	 * Pure mapping; does not mutate [metadata] or [shape].
	 * Cross-field args are not native JSON Schema facets.
	 *
	 * @param metadata constraint metadata to document
	 * @param shape property or type-use shape, or null when unavailable
	 * @return always [ConstraintDocHints.EMPTY]	 
	 */
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints = ConstraintDocHints.EMPTY
}
