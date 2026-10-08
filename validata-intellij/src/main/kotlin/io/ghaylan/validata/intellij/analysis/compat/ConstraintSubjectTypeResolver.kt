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

package io.ghaylan.validata.intellij.analysis.compat

import io.ghaylan.validata.intellij.scope.PropertyRefOwnerResolver
import io.ghaylan.validata.intellij.typing.ValidatorTypeView
import io.ghaylan.validata.schema.shape.ScalarKind
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Resolves the **direct annotated subject type** for constraint tooling.
 *
 * **What.** Given a constraint [KtAnnotationEntry], finds the type it actually constrains —
 * either the [KtTypeReference] that hosts it as a type-use annotation, or the enclosing
 * property / parameter’s declared type — and exposes that as [ValidatorTypeView], FQCN, or
 * scalar kind.
 *
 * **Why.** Type-use vs declaration-site placement changes the subject: `List<@Email String>`
 * constrains `String`, while `@Email val x: List<String>` constrains `List<String>`. Getting
 * this wrong makes validator ranking and `TYPED_LITERAL` checks lie.
 *
 * **How it fits.** Upstream of [SubjectTypeViews] / [PropertyRefScalarKinds] for inspections,
 * completions, and literal verifiers. Owner discovery for “which member?” is
 * [PropertyRefOwnerResolver.findAnnotatedSubject]; this object answers “which **type**?”.
 *
 * **Not.** Not a path / sibling resolver. Does not decide PropertyRef scope (SIBLING vs
 * ELEMENT). Does not parse annotation arguments.
 *
 * **KSP / runtime parity.** Matches KSP: type-use annotations constrain the type they sit on,
 * not the outer property.
 *
 * | Site | Subject |
 * |---|---|
 * | `List<@Email String>` | `String` |
 * | `@Email val x: List<String>` / `@field:Email` | `List<String>` |
 * | `val x: @Email List<String>` | `List<String>` |*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintSubjectTypeResolver {
	
	/**
	 * [ValidatorTypeView] for [annotation]'s immediate subject, or `null` when unresolved.
	 *
	 * Prefers [typeReferenceHosting] when the annotation is a type-use entry on a
	 * [KtTypeReference]; otherwise falls back to the enclosing property / parameter via
	 * [PropertyRefOwnerResolver.findAnnotatedSubject] + [SubjectTypeViews.ofDeclaration].
	 *
	 * @param annotation constraint annotation use-site
	 * @return subject type view, or `null` when neither a hosting type reference nor a named
	 *   declaration subject can be classified	 
	 */
	fun typeView(annotation: KtAnnotationEntry): ValidatorTypeView? {
		typeReferenceHosting(annotation)?.typeElement?.let { return SubjectTypeViews.ofTypeElement(it) }
		val subject = PropertyRefOwnerResolver.findAnnotatedSubject(annotation) as? KtNamedDeclaration
			?: return null
		return SubjectTypeViews.ofDeclaration(subject)
	}
	
	/**
	 * FQCN of the direct annotated subject (for `TYPED_LITERAL` / enum membership).
	 *
	 * @param annotation constraint annotation use-site
	 * @return qualified name from [typeView], or `null` when the view is missing or is the
	 *   wildcard (`"*"`) placeholder	 
	 */
	fun fqName(annotation: KtAnnotationEntry): String? = typeView(annotation)?.qualifiedName?.takeUnless { it == "*" }
	
	/**
	 * Scalar kind of the direct annotated subject, when classifiable.
	 *
	 * @param annotation constraint annotation use-site
	 * @return [ScalarKind] from [PropertyRefScalarKinds.scalarKind], or `null` when [fqName]
	 *   cannot be resolved (missing subject / wildcard)	 
	 */
	fun scalarKind(annotation: KtAnnotationEntry): ScalarKind? {
		val fq = fqName(annotation)
			?: return null
		return PropertyRefScalarKinds.scalarKind(fq)
	}
	
	/**
	 * The [KtTypeReference] that lists [annotation] among its type-use annotations, if any.
	 *
	 * Walks to the nearest [KtTypeReference] ancestor and confirms [annotation] is in that
	 * reference’s [KtTypeReference.annotationEntries] (avoids picking up an unrelated parent
	 * type ref).
	 *
	 * @param annotation constraint annotation use-site
	 * @return hosting type reference, or `null` when the annotation is declaration-site only
	 *   (modifier-list / use-site target) or not listed on the parent type reference	 
	 */
	fun typeReferenceHosting(annotation: KtAnnotationEntry): KtTypeReference? {
		val typeRef = annotation.getStrictParentOfType<KtTypeReference>()
			?: return null
		return typeRef.takeIf { annotation in it.annotationEntries }
	}
}
