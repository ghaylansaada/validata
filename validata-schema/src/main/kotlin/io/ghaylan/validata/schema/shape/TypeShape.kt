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
package io.ghaylan.validata.schema.shape

import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Structural shape of a property type for engine walk dispatch (scalar / object / iterable / map / dynamic).
 *
 * Property-level annotations sit on the owning property; type-use annotations sit on the matching
 * [TypeShape] node. Every variant exposes [constraints].
 * 
 * @author Ghaylan Saada
 */
sealed interface TypeShape {
	
	/**
	 * Constraints declared on this type-use node; empty when the node carries none.
	 */
	val constraints: List<CompiledConstraint>
}
