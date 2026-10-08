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
import io.ghaylan.validata.intellij.analysis.apierror.ApiErrorCatalogAnalysis
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Attaches enum-constant references on presentation `code = "…"` strings when the catalog is
 * an enum (`@ApiError(catalog=…)`).
 *
 * ## What
 * Platform [PsiReferenceProvider] that resolves the enum catalog for a code string and returns
 * a [ConstraintEnumLiteralPsiReference] (Go to Declaration + completion variants).
 *
 * ## When it fires
 * For every Kotlin string template registered by [PropertyRefReferenceContributor]. Returns
 * empty unless the string is a presentation `code` argument with an enum catalog.
 *
 * ## What it is NOT
 * - Not a catalog **validity** checker — see `ApiErrorCatalogAnnotator`.
 * - Not a plugin.xml EP by itself — wired through [PropertyRefReferenceContributor].*
 * 
 * @author Ghaylan Saada
 */
internal class ApiErrorCatalogReferenceProvider: PsiReferenceProvider() {
	
	/**
	 * Builds an enum-constant reference when [element] is an `@ApiError` code literal with an
	 * enum catalog.
	 *
	 * @param element candidate PSI (expected [KtStringTemplateExpression])
	 * @param context unused platform processing context
	 * @return one reference, or [PsiReference.EMPTY_ARRAY] when unmatched	 
	 */
	override fun getReferencesByElement(
		element: PsiElement,
		context: ProcessingContext,
	): Array<PsiReference> {
		val stringTemplate = element as? KtStringTemplateExpression
			?: return PsiReference.EMPTY_ARRAY
		if (stringTemplate.hasInterpolation()) return PsiReference.EMPTY_ARRAY
		val enumClass = ApiErrorCatalogAnalysis.enumCatalogForCodeLiteral(stringTemplate)
			?: return PsiReference.EMPTY_ARRAY
		val raw = stringTemplate.entries.joinToString(separator = "") { it.text }
		val trimmed = raw.trim()
		if (raw.isNotEmpty() && trimmed.isEmpty()) return PsiReference.EMPTY_ARRAY
		val soft = trimmed.isEmpty()
		return arrayOf(
			ConstraintEnumLiteralPsiReference(
				element = stringTemplate,
				enumClass = enumClass,
				rangeInElement = ElementManipulators.getValueTextRange(stringTemplate),
				soft = soft,
			),
		)
	}
}
