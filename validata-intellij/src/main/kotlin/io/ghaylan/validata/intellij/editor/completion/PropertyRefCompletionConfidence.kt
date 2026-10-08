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

package io.ghaylan.validata.intellij.editor.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Allows autopopup completion inside property-ref and enum typed-literal string hosts.
 *
 * ## What
 * Platform [CompletionConfidence] that answers “should we skip autopopup here?”. For Validata
 * hosts it returns [ThreeState.NO] (do **not** skip), so letter-by-letter completion can open
 * inside what IntelliJ normally treats as an ordinary string.
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.completion.confidence` with `language="kotlin"` and
 * `order="first"` so [ThreeState.NO] wins over the platform’s `SkipAutopopupInStrings`:
 * ```
 * <completion.confidence
 *     language="kotlin"
 *     order="first"
 *     implementationClass="…editor.completion.PropertyRefCompletionConfidence"/>
 * ```
 * See also `PropertyRefTypedHandler`, which actively schedules the popup on identifier / `.`
 * keystrokes once confidence permits it.
 *
 * ## When it fires
 * Before autopopup decisions for Kotlin files. Inspects the context element’s enclosing
 * [KtStringTemplateExpression] and checks for attached Validata references.
 *
 * ## What it is NOT
 * - Not a completion item provider — see [PropertyRefCompletionContributor].
 * - Does not force popup by itself; returns [ThreeState.UNSURE] outside Validata hosts so
 *   other confidence contributors keep their normal string behaviour.*
 * 
 * @author Ghaylan Saada
 */
class PropertyRefCompletionConfidence: CompletionConfidence() {
	
	/**
	 * Autopopup skip decision.
	 *
	 * Validata: delegates to [decide] on [contextElement]; [editor] / [psiFile] / [offset]
	 * are unused beyond platform signature requirements.
	 *
	 * @param editor active editor
	 * @param contextElement PSI under / near the caret
	 * @param psiFile containing file
	 * @param offset caret offset
	 * @return [ThreeState.NO] inside Validata string hosts; otherwise [ThreeState.UNSURE]	 
	 */
	override fun shouldSkipAutopopup(
		editor: Editor,
		contextElement: PsiElement,
		psiFile: PsiFile,
		offset: Int,
	): ThreeState = decide(contextElement)
	
	/**
	 * Shared host detection for [shouldSkipAutopopup].
	 *
	 * @param contextElement PSI near the caret
	 * @return [ThreeState.NO] when a Validata reference is present; [ThreeState.UNSURE] otherwise	 
	 */
	private fun decide(contextElement: PsiElement): ThreeState {
		val stringTemplate = contextElement as? KtStringTemplateExpression
			?: contextElement.getStrictParentOfType()
			?: return ThreeState.UNSURE
		val refs = stringTemplate.references
		val host = refs.any {
			it is PropertyRefPsiReference || it is ConstraintEnumLiteralPsiReference
		}
		return if (host) ThreeState.NO else ThreeState.UNSURE
	}
}
