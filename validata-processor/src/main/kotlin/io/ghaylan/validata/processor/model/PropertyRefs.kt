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
package io.ghaylan.validata.processor.model

import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind

/**
 * Cross-field / element property names extracted from a single constraint annotation usage.
 *
 * Temporary carrier between PropertyRefHostDiscovery and [ConstraintModel] construction; also the
 * input shape for PropertyReferenceVerifier.
 *
 * @property siblings Paths from metadata fields marked `@PropertyRef` with sibling scope
 * @property elements Paths from metadata fields marked `@PropertyRef(scope = ELEMENT)`
 * @property compatibilityKind Scalar rule from sibling `@PropertyRef.compatibility`
 *   (defaults [PropertyRefCompatibilityKind.NONE])
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyRefs(
	val siblings: List<String> = emptyList(),
	val elements: List<String> = emptyList(),
	val compatibilityKind: PropertyRefCompatibilityKind = PropertyRefCompatibilityKind.NONE
)