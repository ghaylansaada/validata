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

import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Lists Kotlin properties on a class and navigates to nested object types for dotted paths.
 *
 * **What.** Shared member enumeration for [PropertyRefObjectTypeScope]: body properties plus
 * primary-constructor `val`/`var` parameters, and resolve of a member’s type to the next
 * [KtClassOrObject] for multi-segment `@PropertyRef` paths (`"address.city"`).
 *
 * **Why.** One listing policy keeps completions, path resolve, and type stepping consistent
 * across SIBLING and ELEMENT object scopes.
 *
 * **How it fits.** Called by [PropertyRefObjectTypeScope]; path walkers use [resolveTypeClass]
 * then wrap the result in a new [PropertyRefObjectTypeScope].
 *
 * **Not.** Not JavaBean `getX` / `isX` synthesis. Does not list local properties, nested
 * classes, or functions. Does not unwrap nullability / generics for stepping — only the
 * outermost [KtUserType] is resolved (nullable `User?` may fail the `as? KtUserType` cast
 * unless callers unwrap first; current code expects a plain user type on the member).
 *
 * **KSP / runtime parity.** Kotlin property names only — matching processor object-schema
 * property discovery. [resolveTypeClass] returning `null` for non-object leaves matches KSP’s
 * “cannot step into non-object” spirit.*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefClassMembers {
	
	/**
	 * Declared properties plus primary-constructor parameters that introduce properties
	 * (`val` / `var`). Kotlin property names only — not Java-style `getX` accessors.
	 *
	 * Constructor parameters are inserted first; body properties with the same name overwrite
	 * (body wins). Unnamed parameters / properties are skipped.
	 *
	 * @param ownerClass class or object to inspect
	 * @return distinct members in constructor-then-body encounter order (with body overriding);
	 *   empty when [ownerClass] has neither	 
	 */
	fun listMembers(ownerClass: KtClassOrObject): List<KtNamedDeclaration> {
		val fromBody = ownerClass.declarations.filterIsInstance<KtProperty>()
			.filter { !it.isLocal }
		val fromCtor = ownerClass.primaryConstructor?.valueParameters?.filter { it.hasValOrVar() }
			.orEmpty()
		val byName = LinkedHashMap<String, KtNamedDeclaration>()
		for (p in fromCtor) {
			val n = p.name
				?: continue
			byName[n] = p
		}
		for (p in fromBody) {
			val n = p.name
				?: continue
			byName[n] = p
		}
		return byName.values.toList()
	}
	
	/**
	 * Finds a member by declared Kotlin name on [ownerClass].
	 *
	 * @param ownerClass class or object to search
	 * @param name exact property name
	 * @return matching declaration from [listMembers], or `null` when absent	 
	 */
	fun findMember(
		ownerClass: KtClassOrObject,
		name: String
	): KtNamedDeclaration? = listMembers(ownerClass).find { it.name == name }
	
	/**
	 * Resolves the class/object type of [member] for stepping into the next path segment.
	 *
	 * Returns `null` when the type is missing, unresolved, or not a class/object (e.g. `String`)
	 * — matching KSP’s “cannot step into non-object” spirit.
	 *
	 * @param member property or constructor parameter from [listMembers]
	 * @return nested [KtClassOrObject], or `null` when [member] is not a property/parameter,
	 *   has no type reference, the type element is not a [KtUserType], or resolve does not
	 *   yield a Kotlin class/object	 
	 */
	fun resolveTypeClass(member: KtNamedDeclaration): KtClassOrObject? {
		val typeElement: KtTypeElement? = when (member) {
			is KtProperty -> member.typeReference?.typeElement
			is KtParameter -> member.typeReference?.typeElement
			else -> null
		}
		val userType = typeElement as? KtUserType
			?: return null
		return userType.referenceExpression?.mainReference?.resolve() as? KtClassOrObject
	}
}
