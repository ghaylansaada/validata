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

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * Declaring class of annotated members — used for object / element scopes.
 *
 * **What.** [PropertyRefSiblingScope] backed by a [KtClassOrObject]: members are the class’s
 * Kotlin properties (body + `val`/`var` constructor parameters) via [PropertyRefClassMembers].
 *
 * **Why.** DTO / bean sibling refs and ELEMENT-scoped Distinct paths both need “properties of
 * this type” as the resolve root.
 *
 * **How it fits.** Built by [PropertyRefOwnerResolver] (containing class) and
 * [PropertyRefElementTypeResolver] (collection element class). Nested path segments create
 * new scopes from [PropertyRefClassMembers.resolveTypeClass].
 *
 * **Not.** Not flat handler parameters — see [PropertyRefFlatParamsScope]. Does not include
 * local properties, extensions, or Java-only fields.
 *
 * **KSP / runtime parity.** Member listing rules match [PropertyRefClassMembers] /
 * processor object-schema property discovery (Kotlin names only).
 *
 * @property klass Kotlin class or object whose properties form this scope*
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyRefObjectTypeScope(
	val klass: KtClassOrObject,
): PropertyRefSiblingScope {
	
	/**
	 * @param name exact Kotlin property name
	 * @return member declaration, or `null` when unknown on [klass]	 
	 */
	override fun findMember(name: String): KtNamedDeclaration? = PropertyRefClassMembers.findMember(klass, name)
	
	/**
	 * @return all listable properties on [klass]; empty when the class declares none
	 */
	override fun listMembers(): List<KtNamedDeclaration> = PropertyRefClassMembers.listMembers(klass)
	
	/**
	 * @return [klass]'s simple [KtClassOrObject.name], or `null` when anonymous / unnamed
	 */
	override fun typeText(): String? = klass.name
}
