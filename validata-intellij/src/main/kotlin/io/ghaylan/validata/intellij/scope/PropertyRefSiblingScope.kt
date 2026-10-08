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

/**
 * Where sibling / element property-ref segments resolve and complete.
 *
 * **What.** Sealed resolve root for the first (and nested object) segments of a `@PropertyRef`
 * path: look up a member by name, list completion candidates, and expose a short type label.
 *
 * **Why.** Object DTO fields and flat handler parameters share the same path language but
 * different member sources. One interface keeps inspections / completions agnostic.
 *
 * **How it fits.** Produced by [PropertyRefOwnerResolver.resolveSiblingScope] (SIBLING) or
 * [PropertyRefElementTypeResolver.resolveElementScope] (ELEMENT). Nested dotted segments step
 * through [PropertyRefClassMembers.resolveTypeClass] into further [PropertyRefObjectTypeScope]s.
 *
 * **Not.** Not a full Kotlin scope (no extensions, no Java getters-as-properties beyond what
 * [PropertyRefClassMembers] lists). Does not parse path strings — callers split segments.
 *
 * **KSP / runtime parity.** Mirrors processor sibling vs flat-parameter ownership described in
 * `docs/PROPERTY_REFERENCES.md` §4.
 *
 * Variants:
 * - [PropertyRefObjectTypeScope] — properties of a Kotlin class (DTO body fields, Distinct element)
 * - [PropertyRefFlatParamsScope] — value-parameters of a handler method (query / header / path)*
 * 
 * @author Ghaylan Saada
 */
internal sealed interface PropertyRefSiblingScope {
	
	/**
	 * Finds a member by Kotlin name, or `null` if unknown.
	 *
	 * @param name exact property / parameter name (case-sensitive)
	 * @return matching declaration, or `null` when no member has that name	 
	 */
	fun findMember(name: String): KtNamedDeclaration?
	
	/**
	 * Completion candidates in this scope.
	 *
	 * @return all named members suitable for a property-ref segment; empty when the owner has
	 *   no listable properties / parameters	 
	 */
	fun listMembers(): List<KtNamedDeclaration>
	
	/**
	 * Short label for lookup type text (e.g. class simple name, or `"parameters"`).
	 *
	 * @return display label, or `null` when the object scope’s class has no name	 
	 */
	fun typeText(): String?
}
