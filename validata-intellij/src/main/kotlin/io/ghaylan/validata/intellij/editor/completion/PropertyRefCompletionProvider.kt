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

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.ProcessingContext
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Completion inside constraint string literals for:
 * - `@PropertyRef` path segments ([PropertyRefPsiReference])
 * - Enum typed-literal constants ([ConstraintEnumLiteralPsiReference])
 *
 * ## What
 * Platform [CompletionProvider] that finds the enclosing string template, selects the active
 * Validata PSI reference under the caret, and feeds that reference’s [PsiReference.getVariants]
 * into the completion result set with a prefix matcher for the typed segment text.
 *
 * ## When it fires
 * Registered by [PropertyRefCompletionContributor] for basic completion inside
 * [KtStringTemplateExpression]. Only produces items when the string already has Validata
 * references attached by the reference contributor — it does not rediscover hosts.
 *
 * ## Completes
 * Sibling / element member names for property refs; enum constant names for typed literals.
 * Prefix is the substring of the active reference range up to the caret so filtering works
 * mid-segment.
 *
 * ## What it is NOT
 * - Not a plugin.xml EP by itself — wired through [PropertyRefCompletionContributor].
 * - Not responsible for autopopup policy — see [PropertyRefCompletionConfidence] and
 *   `PropertyRefTypedHandler`.
 * - Does not complete arbitrary Kotlin identifiers outside Validata string hosts.*
 * 
 * @author Ghaylan Saada
 */
internal class PropertyRefCompletionProvider: CompletionProvider<CompletionParameters>() {
	
	/**
	 * Adds Validata lookup elements for the caret’s string-template reference.
	 *
	 * Validata: prefers [PropertyRefPsiReference] when present (selecting the segment under /
	 * after the caret); otherwise uses the first [ConstraintEnumLiteralPsiReference]. Prefix
	 * matching restarts on any prefix change so letter-by-letter typing stays live.
	 *
	 * @param parameters platform completion parameters (position, offset, original file)
	 * @param context unused processing context
	 * @param result completion result set to populate	 
	 */
	override fun addCompletions(
		parameters: CompletionParameters,
		context: ProcessingContext,
		result: CompletionResultSet,
	) {
		val stringTemplate = findStringTemplate(parameters.position)
			?: return
		val offsetInFile = parameters.offset
		val propertyRefs = stringTemplate.references.filterIsInstance<PropertyRefPsiReference>()
		if (propertyRefs.isNotEmpty()) {
			val active = selectActiveReference(propertyRefs, offsetInFile)
				?: return
			addVariants(active, active.absoluteRange, offsetInFile, parameters.originalFile.text, result)
			return
		}
		val enumRef = stringTemplate.references.filterIsInstance<ConstraintEnumLiteralPsiReference>()
			.firstOrNull()
			?: return
		addVariants(enumRef, enumRef.absoluteRange, offsetInFile, parameters.originalFile.text, result)
	}
	
	/**
	 * Copies [ref] variants into [result] with a caret-relative prefix matcher.
	 *
	 * Accepts [LookupElementBuilder], plain [String], or falls back to `toString()` for other
	 * variant objects returned by the reference.
	 *
	 * @param ref active Validata reference
	 * @param absoluteRange file range of the reference text
	 * @param offsetInFile caret offset in the file
	 * @param fileText original file text for prefix slicing
	 * @param result completion result set	 
	 */
	private fun addVariants(
		ref: PsiReference,
		absoluteRange: TextRange,
		offsetInFile: Int,
		fileText: String,
		result: CompletionResultSet,
	) {
		val prefix = prefixFor(absoluteRange, offsetInFile, fileText)
		val prefixed = result.withPrefixMatcher(prefix)
		prefixed.restartCompletionOnAnyPrefixChange()
		for (variant in ref.variants) {
			when (variant) {
				is LookupElementBuilder -> prefixed.addElement(variant)
				is String -> prefixed.addElement(LookupElementBuilder.create(variant))
				else -> {
					val text = variant.toString()
					if (text.isNotEmpty()) {
						prefixed.addElement(LookupElementBuilder.create(text))
					}
				}
			}
		}
	}
	
	/**
	 * Picks the segment reference under the caret; if the caret sits on a `.`, prefers the
	 * next segment (often the soft empty trailing segment).
	 *
	 * @param refs all property-ref segment references on the host string
	 * @param offsetInFile caret offset
	 * @return the reference that should supply variants, or `null` if [refs] is empty	 
	 */
	private fun selectActiveReference(
		refs: List<PropertyRefPsiReference>,
		offsetInFile: Int,
	): PropertyRefPsiReference? {
		val containing = refs.filter { ref ->
			val range = ref.absoluteRange
			offsetInFile in range.startOffset..range.endOffset
		}
		if (containing.isNotEmpty()) return containing.last()
		val next = refs.firstOrNull { it.absoluteRange.startOffset >= offsetInFile }
		return next
			?: refs.lastOrNull()
	}
	
	/**
	 * Text before the caret within [absoluteRange], used as the completion prefix.
	 *
	 * @param absoluteRange reference range in the file
	 * @param offsetInFile caret offset
	 * @param fileText full file text
	 * @return prefix string, or `""` when the caret is outside the range / file	 
	 */
	private fun prefixFor(
		absoluteRange: TextRange,
		offsetInFile: Int,
		fileText: String,
	): String {
		val start = absoluteRange.startOffset
		if (offsetInFile < start || offsetInFile > fileText.length) return ""
		val end = minOf(offsetInFile, absoluteRange.endOffset)
		if (end < start) return ""
		return fileText.substring(start, end)
	}
	
	/**
	 * Resolves the [KtStringTemplateExpression] containing the completion [position].
	 *
	 * @param position completion PSI position (often a leaf inside the string)
	 * @return enclosing string template, or `null`	 
	 */
	private fun findStringTemplate(position: PsiElement): KtStringTemplateExpression? = position as? KtStringTemplateExpression
		?: position.getStrictParentOfType()
}
