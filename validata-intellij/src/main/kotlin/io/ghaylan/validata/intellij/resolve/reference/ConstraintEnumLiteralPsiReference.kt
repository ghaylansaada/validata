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

package io.ghaylan.validata.intellij.resolve.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import io.ghaylan.validata.intellij.analysis.literal.ConstraintEnumLiteralSupport
import io.ghaylan.validata.intellij.analysis.literal.ConstraintLiteralFormats
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * PSI reference from an enum-name string literal to a Kotlin / Java enum constant
 * (e.g. `"PHONE"` → `ContactType.PHONE`, including `java.time.Month` names).
 *
 * ## What
 * One reference spans the full value text of a [KtStringTemplateExpression] whose constraint
 * argument is marked `@ConstraintArg(TYPED_LITERAL)` and whose annotated subject type is an
 * enum. Resolve maps the trimmed string to the matching constant on [enumClass]; variants list
 * every constant for completion.
 *
 * ## Why
 * Authors write enum constants as **strings** in Validata constraints (`@In(values =
 * ["ADMIN"])`, `@Compare(ref = "PHONE", operation = Compare.Operation.EQ)`). Without a PSI reference, Go to Declaration and rename
 * against the real enum entry do not work, and completion cannot offer constant names.
 *
 * ## Wiring (not a plugin.xml EP itself)
 * Created only by `ConstraintEnumLiteralReferenceProvider`,
 * registered via `psi.referenceContributor` → `PropertyRefReferenceContributor`.
 *
 * ## Soft vs hard
 * Soft empty `""` hosts completion while typing. Non-empty unresolved names use a hard
 * reference so Go to Declaration is unavailable and annotators can treat the name as broken.
 *
 * ## What it is NOT
 * - Not a typed-literal **format** checker (ISO dates, numbers) — that lives in
 *   `ConstraintLiteralAnnotator` / [ConstraintEnumLiteralSupport] / [ConstraintLiteralFormats].
 * - Not a `@PropertyRef` path reference — see [PropertyRefPsiReference].
 * - Not responsible for deciding which annotation arguments are typed-literal hosts.
 *
 * @param element host string template (constraint argument value)
 * @param rangeInElement range inside [element] covering the literal text (defaults to value range)
 * @param soft when `true` (typically empty `""`), unresolved is soft for the platform
 * @property enumClass Kotlin [KtClass] or Java [PsiClass] for the subject enum type*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintEnumLiteralPsiReference(
	element: KtStringTemplateExpression,
	private val enumClass: PsiElement,
	rangeInElement: TextRange = ElementManipulators.getValueTextRange(element),
	soft: Boolean = false,
): PsiReferenceBase<KtStringTemplateExpression>(element, rangeInElement, soft), PsiPolyVariantReference {
	
	
	/**
	 * Single-result resolve for Go to Declaration.
	 *
	 * Validata: first (and only) [multiResolve] hit — the enum constant PSI whose name equals
	 * this literal — or `null` when blank / unknown.
	 *
	 * @return enum constant element, or `null` when unresolved	 
	 */
	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element
	
	/**
	 * Poly-variant resolve of the enum constant name.
	 *
	 * Validata: delegates to [ConstraintEnumLiteralSupport.findConstant] on [enumClass]. At most
	 * one constant matches a given name.
	 *
	 * @param incompleteCode ignored — names are matched exactly after trim
	 * @return zero or one [PsiElementResolveResult]	 
	 */
	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
		val name = value.trim()
		if (name.isEmpty()) return ResolveResult.EMPTY_ARRAY
		val target = ConstraintEnumLiteralSupport.findConstant(enumClass, name)
			?: return ResolveResult.EMPTY_ARRAY
		return arrayOf(PsiElementResolveResult(target))
	}
	
	/**
	 * Completion candidates: every constant of [enumClass].
	 *
	 * Validata: lookup type text is the enum’s simple name (`KtClass.name` / `PsiClass.name`).
	 *
	 * @return lookup elements for all named constants	 
	 */
	override fun getVariants(): Array<Any> {
		val typeLabel = when (enumClass) {
			is KtClass -> enumClass.name
			is PsiClass -> enumClass.name
			else -> null
		}
		return ConstraintEnumLiteralSupport.listConstants(enumClass)
			.mapNotNull { constant ->
				val name = constant.name
					?: return@mapNotNull null
				LookupElementBuilder.create(constant, name)
					.withIcon(constant.getIcon(0))
					.withTypeText(typeLabel, true)
			}
			.toTypedArray()
	}
	
	/**
	 * Renames the string literal to match a renamed enum constant.
	 *
	 * Validata: writes [newElementName] into [rangeInElement] only (the constant spelling).
	 *
	 * @param newElementName new enum constant name
	 * @return the mutated host [PsiElement]	 
	 */
	override fun handleElementRename(newElementName: String): PsiElement {
		return ElementManipulators.handleContentChange(element, rangeInElement, newElementName)
	}
}
