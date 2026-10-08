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

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * Resolves the **owner type** for sibling `@PropertyRef` paths — the declaring class of the
 * annotated subject (property, getter, or constructor value-parameter), or flat method
 * parameters for handler endpoints.
 *
 * **What.** From a constraint [KtAnnotationEntry], finds the annotated subject member and
 * chooses the correct [PropertyRefSiblingScope]: object-type (DTO / constructor params) vs
 * flat method value-parameters.
 *
 * **Why.** Sibling refs like `@Compare(ref = "maxAge", operation = Compare.Operation.GT)` must resolve against the
 * **same owner** KSP uses — class properties for bean/DTO fields, or peer parameters for
 * `@Validate` handlers — or completions and inspections drift from compile-time checks.
 *
 * **How it fits.** Entry point for SIBLING path resolve / complete. ELEMENT scope is
 * [PropertyRefElementTypeResolver]. Subject type (validator ranking) is
 * `ConstraintSubjectTypeResolver`, which also reuses [findAnnotatedSubject].
 *
 * **Not.** Not ELEMENT-scope. Does not walk dotted paths. Does not classify scalar kinds.
 * Stops at [KtClassOrObject] when walking parents so class-level annotations are not treated
 * as member subjects.
 *
 * **KSP / runtime parity.** Parity target: `docs/PROPERTY_REFERENCES.md` §4 — constructor
 * value-parameters → containing class scope; named function value-parameters → flat params
 * scope; properties → containing class/object.*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefOwnerResolver {
	
	/**
	 * Sibling resolve scope for [annotation]'s use-site.
	 *
	 * @param annotation constraint annotation (typically carrying `@PropertyRef` metadata)
	 * @return [PropertyRefObjectTypeScope] or [PropertyRefFlatParamsScope], or `null` when
	 *   [findAnnotatedSubject] fails or the subject kind / owner is unsupported	 
	 */
	fun resolveSiblingScope(annotation: KtAnnotationEntry): PropertyRefSiblingScope? {
		val subject = findAnnotatedSubject(annotation)
			?: return null
		return when (subject) {
			is KtProperty -> subject.containingClassOrObject?.let { PropertyRefObjectTypeScope(it) }
			is KtParameter -> scopeOfParameter(subject)
			else -> null
		}
	}
	
	/**
	 * Finds the property / parameter that carries [annotation] (modifier list or type-use
	 * annotation on that member’s type).
	 *
	 * Walks PSI parents until a [KtProperty] or [KtParameter] is found. Encountering a
	 * [KtClassOrObject] first means the annotation is class-level / unrelated — returns `null`.
	 *
	 * @param annotation any annotation entry nested under a member (or its type)
	 * @return owning property or parameter, or `null` when no member subject is found before
	 *   a class boundary / root	 
	 */
	fun findAnnotatedSubject(annotation: KtAnnotationEntry): KtModifierListOwner? {
		var current = annotation.parent
		while (current != null) {
			when (current) {
				is KtProperty -> return current
				is KtParameter -> return current
				is KtClassOrObject -> return null
			}
			current = current.parent
		}
		return null
	}
	
	/**
	 * Chooses object vs flat scope for a parameter subject.
	 *
	 * Primary-constructor parameters → containing class. Named-function value-parameters →
	 * flat peer parameters. Other owners fall back to containing class/object when present.
	 *
	 * @param parameter annotated parameter
	 * @return sibling scope, or `null` when no containing class/object exists for non-function
	 *   owners	 
	 */
	private fun scopeOfParameter(parameter: KtParameter): PropertyRefSiblingScope? {
		val ownerFunction = parameter.ownerFunction
			?: parameter.parent?.parent // KtParameterList → function/constructor
		return when (ownerFunction) {
			is KtPrimaryConstructor -> ownerFunction.containingClassOrObject?.let { PropertyRefObjectTypeScope(it) }
			is KtNamedFunction -> PropertyRefFlatParamsScope(ownerFunction.valueParameters)
			else -> parameter.containingClassOrObject?.let { PropertyRefObjectTypeScope(it) }
		}
	}
}
