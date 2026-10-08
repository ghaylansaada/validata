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

/**
 * Minimal name → shape resolution used by PropertyReferenceVerifier.
 *
 * Both object schemas and flat endpoint parameter sets expose the same one-function surface so there
 * is exactly one cross-reference algorithm — not a body verifier and a second endpoint verifier.
 * */
internal fun interface NamedShapeLookup {
	
	/**
	 * Resolves a sibling / flat-parameter name to its shape.
	 *
	 * Implementations typically accept both the declared Kotlin name and the wire / Spring name.
	 *
	 * Side effects: none.
	 *
	 * @param name Property or parameter name as written in a constraint argument.
	 * @return Shape for [name], or `null` when the name is unknown on this owner.	 
	 */
	fun resolve(name: String): ShapeModel?
}
