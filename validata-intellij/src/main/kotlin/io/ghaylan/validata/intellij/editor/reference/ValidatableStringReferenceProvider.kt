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

package io.ghaylan.validata.intellij.editor.reference

import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import io.ghaylan.validata.intellij.analysis.validatable.ValidatableAnnotationAnalysis
import io.ghaylan.validata.intellij.analysis.validatable.ValidatableDiscriminatorSupport
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.scope.PropertyRefObjectTypeScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * PSI references for `@Validatable` string hosts:
 * - `discriminator = "prop"` → property on the annotated class (completion / Ctrl+Click)
 * - `Subtype(name = "CONST")` → enum constant when the discriminator property is an enum
 *
 * ## When it fires
 * Every Kotlin string template via [PropertyRefReferenceContributor]. Early-outs unless the
 * string sits on a `@Validatable` (or nested `Subtype`) argument named `discriminator` / `name`.
 *
 * ## What it is NOT
 * - Not a format validator for numeric/temporal subtype names — [ValidatableAnnotator] does that
 *   (and recolors valid names like constraint typed literals).
 * - Not a constraint `@PropertyRef` / `@ConstraintArg` provider.*
 * 
 * @author Ghaylan Saada
 */
internal class ValidatableStringReferenceProvider: PsiReferenceProvider() {
	
	/**
	 * Attaches discriminator / subtype-name PSI references when [element] is a matching host.
	 *
	 * No side effects beyond returning reference instances for the platform.
	 *
	 * @param element candidate string template
	 * @param context unused platform processing context
	 * @return zero or one [PropertyRefPsiReference] / [ConstraintEnumLiteralPsiReference]	 
	 */
	override fun getReferencesByElement(
		element: PsiElement,
		context: ProcessingContext,
	): Array<PsiReference> {
		val stringTemplate = element as? KtStringTemplateExpression
			?: return PsiReference.EMPTY_ARRAY
		if (stringTemplate.hasInterpolation()) return PsiReference.EMPTY_ARRAY
		val argument = stringTemplate.getStrictParentOfType<KtValueArgument>()
			?: return PsiReference.EMPTY_ARRAY
		val argName = argument.getArgumentName()?.asName?.asString()
		
		return when (argName) {
			"discriminator" -> discriminatorRefs(stringTemplate, argument)
			"name" -> subtypeNameRefs(stringTemplate, argument)
			else -> PsiReference.EMPTY_ARRAY
		}
	}
	
	/**
	 * Builds a property reference for `@Validatable(discriminator = "…")` against [owner].
	 *
	 * @param stringTemplate discriminator string host
	 * @param argument enclosing value argument
	 * @return one [PropertyRefPsiReference], or empty when not on a class-level `@Validatable`	 
	 */
	private fun discriminatorRefs(
		stringTemplate: KtStringTemplateExpression,
		argument: KtValueArgument,
	): Array<PsiReference> {
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>()
			?: return PsiReference.EMPTY_ARRAY
		if (!ValidatableAnnotationAnalysis.isValidatable(annotation)) return PsiReference.EMPTY_ARRAY
		val owner = annotation.getStrictParentOfType<KtClass>()
			?: return PsiReference.EMPTY_ARRAY
		if (!owner.annotationEntries.contains(annotation)) return PsiReference.EMPTY_ARRAY
		val raw = stringTemplate.entries.joinToString("") { it.text }
		val trimmed = raw.trim()
		val valueRange = ElementManipulators.getValueTextRange(stringTemplate)
		return arrayOf(
			PropertyRefPsiReference(
				element = stringTemplate,
				scope = PropertyRefObjectTypeScope(owner),
				rangeInElement = valueRange,
				soft = trimmed.isEmpty(),
			),
		)
	}
	
	/**
	 * Builds an enum-constant reference for `Subtype(name = "…")` when the discriminator is enum.
	 *
	 * @param stringTemplate subtype name string host
	 * @param argument enclosing value argument
	 * @return one [ConstraintEnumLiteralPsiReference], or empty when not an enum discriminator	 
	 */
	private fun subtypeNameRefs(
		stringTemplate: KtStringTemplateExpression,
		argument: KtValueArgument,
	): Array<PsiReference> {
		if (!isSubtypeNameArgument(argument)) return PsiReference.EMPTY_ARRAY
		val validatable = enclosingValidatable(argument)
			?: return PsiReference.EMPTY_ARRAY
		val owner = validatable.getStrictParentOfType<KtClass>()
			?: return PsiReference.EMPTY_ARRAY
		if (!owner.annotationEntries.contains(validatable)) return PsiReference.EMPTY_ARRAY
		val enumClass = ValidatableDiscriminatorSupport.discriminatorEnumClass(validatable, owner)
			?: return PsiReference.EMPTY_ARRAY
		val raw = stringTemplate.entries.joinToString("") { it.text }
		val trimmed = raw.trim()
		if (raw.isNotEmpty() && trimmed.isEmpty()) return PsiReference.EMPTY_ARRAY
		val valueRange = ElementManipulators.getValueTextRange(stringTemplate)
		return arrayOf(
			ConstraintEnumLiteralPsiReference(
				element = stringTemplate,
				enumClass = enumClass,
				rangeInElement = valueRange,
				soft = trimmed.isEmpty(),
			),
		)
	}
	
	/**
	 * Whether [argument] is the `name` of a nested `Subtype(…)` call / annotation.
	 *
	 * @param argument value argument under test
	 * @return `true` when enclosed by `Subtype`	 
	 */
	private fun isSubtypeNameArgument(argument: KtValueArgument): Boolean {
		argument.getStrictParentOfType<KtAnnotationEntry>()
			?.let { nested ->
				val short = nested.shortName?.asString()
				if (short == "Subtype") return true
			}
		argument.getStrictParentOfType<KtCallExpression>()
			?.let { call ->
				val text = call.calleeExpression?.text
					?: return@let
				if (text == "Subtype" || text.endsWith(".Subtype")) return true
			}
		return false
	}
	
	/**
	 * Walks outward from [argument] to the enclosing `@Validatable` annotation entry.
	 *
	 * @param argument nested subtype argument
	 * @return enclosing `@Validatable` entry, or `null` when none	 
	 */
	private fun enclosingValidatable(argument: KtValueArgument): KtAnnotationEntry? {
		var current: PsiElement? = argument
		while (current != null) {
			val ann = current.getStrictParentOfType<KtAnnotationEntry>()
				?: break
			if (ValidatableAnnotationAnalysis.isValidatable(ann)) return ann
			current = ann.parent
		}
		return null
	}
}
