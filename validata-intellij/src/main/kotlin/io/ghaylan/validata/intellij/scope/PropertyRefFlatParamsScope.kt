/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.scope

import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Flat transport parameters on a `@Validate` handler (and similar).
 *
 * **What.** [PropertyRefSiblingScope] whose members are the peer [KtParameter]s of a named
 * function (query / header / path / body params at the same level).
 *
 * **Why.** Endpoint handlers often annotate one parameter while referencing another by name
 * (`@RequiredWhen(ref = "country")` on `city`). There is no enclosing DTO type — the
 * scope **is** the parameter list.
 *
 * **How it fits.** Created by [PropertyRefOwnerResolver] when the annotated subject is a
 * [org.jetbrains.kotlin.psi.KtNamedFunction] value-parameter. Completions list parameter names;
 * [findMember] matches by [KtParameter.name].
 *
 * **Not.** Not an object type. Nested dotted paths are invalid here (parity with KSP endpoint
 * verifier) — callers must reject multi-segment paths when this scope is in use. Does not
 * include receiver / extension / local variables.
 *
 * **KSP / runtime parity.** Matches processor flat endpoint argument sibling checks: single
 * segment names only among value-parameters.
 *
 * @property parameters owning function’s value-parameters (including the annotated one)*
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyRefFlatParamsScope(
	val parameters: List<KtParameter>,
): PropertyRefSiblingScope {
	
	/**
	 * @param name exact parameter name
	 * @return matching parameter, or `null` when no parameter has that name	 
	 */
	override fun findMember(name: String): KtNamedDeclaration? = parameters.firstOrNull { it.name == name }
	
	/**
	 * @return parameters that have a non-null name; empty when [parameters] is empty or all
	 *   unnamed	 
	 */
	override fun listMembers(): List<KtNamedDeclaration> = parameters.filter { it.name != null }
	
	/**
	 * Fixed label distinguishing this scope from object-type class names in UI / diagnostics.
	 *
	 * @return always `"parameters"`	 
	 */
	override fun typeText(): String = "parameters"
}
