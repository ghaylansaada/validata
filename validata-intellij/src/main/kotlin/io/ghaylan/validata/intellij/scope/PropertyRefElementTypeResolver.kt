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

import io.ghaylan.validata.intellij.scope.PropertyRefElementTypeResolver.COLLECTION_SHORT_NAMES
import io.ghaylan.validata.schema.types.BuiltinTypeShortNames
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Resolves the **element** owner type for `@PropertyRef(Scope.ELEMENT)` paths (e.g. `@Distinct(by=…)`
 * on `List<UserDto>` → `UserDto`).
 *
 * Collection / array short names come from schema [BuiltinTypeShortNames.COLLECTION_OR_ARRAY_SHORT_NAMES].
 *
 * @author Ghaylan Saada
 */
internal object PropertyRefElementTypeResolver {

	/**
	 * Short names treated as collection / array carriers whose first type argument is the
	 * element type.
	 */
	private val COLLECTION_SHORT_NAMES: Set<String> =
		BuiltinTypeShortNames.COLLECTION_OR_ARRAY_SHORT_NAMES

	/**
	 * Element class of the annotated subject’s collection/array type.
	 *
	 * Convenience wrapper over [resolveElementScope].
	 *
	 * @param annotation constraint with ELEMENT-scoped `@PropertyRef` (callers decide scope)
	 * @return element [KtClassOrObject], or `null` when the subject is missing, not a
	 *   collection/array, or the element type does not resolve to a Kotlin class/object
	 */
	fun resolveElementClass(annotation: KtAnnotationEntry): KtClassOrObject? =
		resolveElementScope(annotation)?.klass

	/**
	 * Element-scope resolve root for `@Distinct(by=…)` (property or type-use on `List<T>`).
	 *
	 * Finds the annotated subject via [PropertyRefOwnerResolver.findAnnotatedSubject], reads
	 * its declared type, and wraps [extractElementClass] in a [PropertyRefObjectTypeScope].
	 *
	 * @param annotation constraint use-site
	 * @return object-type scope rooted at the element class, or `null` when subject / type /
	 *   element class cannot be resolved
	 */
	fun resolveElementScope(annotation: KtAnnotationEntry): PropertyRefObjectTypeScope? {
		val subject = PropertyRefOwnerResolver.findAnnotatedSubject(annotation)
			?: return null
		val typeElement = typeElementOf(subject)
			?: return null
		val elementClass = extractElementClass(typeElement)
			?: return null
		return PropertyRefObjectTypeScope(elementClass)
	}
	
	/**
	 * Declared type element of a property or parameter subject.
	 *
	 * @param subject annotated member from [PropertyRefOwnerResolver.findAnnotatedSubject]
	 * @return type element, or `null` for unsupported owner kinds / missing type refs	 
	 */
	private fun typeElementOf(subject: KtModifierListOwner): KtTypeElement? = when (subject) {
		is KtProperty -> subject.typeReference?.typeElement
		is KtParameter -> subject.typeReference?.typeElement
		else -> null
	}
	
	/**
	 * Unwraps nullability and reads the type argument of List/Set/Collection/Array/… .
	 *
	 * Requires `userType`'s short name in [COLLECTION_SHORT_NAMES], takes the **first** type
	 * argument, unwraps nullability on the element, and resolves it to a [KtClassOrObject].
	 *
	 * @param typeElement subject’s declared type (may be nullable, e.g. `List<User>?`)
	 * @return element class, or `null` when not a recognized collection short name, missing
	 *   type argument, element is not a user type, or resolve does not yield a Kotlin
	 *   class/object (e.g. `List<String>` → `String` may still resolve if `String` is a
	 *   class; primitives / unresolved stars return `null`)	 
	 */
	fun extractElementClass(typeElement: KtTypeElement): KtClassOrObject? {
		val userType = unwrapNullable(typeElement) as? KtUserType
			?: return null
		val shortName = userType.referencedName
			?: return null
		if (shortName !in COLLECTION_SHORT_NAMES) return null
		val argElement = userType.typeArguments.firstOrNull()?.typeReference?.typeElement
			?: return null
		val elementUserType = unwrapNullable(argElement) as? KtUserType
			?: return null
		return elementUserType.referenceExpression?.mainReference?.resolve() as? KtClassOrObject
	}
	
	/**
	 * Strips a single layer of Kotlin nullability (`T?` → `T`).
	 *
	 * @param typeElement possibly nullable type element
	 * @return inner type for [KtNullableType], otherwise [typeElement] unchanged	 
	 */
	private fun unwrapNullable(typeElement: KtTypeElement): KtTypeElement = when (typeElement) {
		is KtNullableType -> typeElement.innerType
			?: typeElement
		
		else -> typeElement
	}
}
