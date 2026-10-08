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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAnnotationMatcher
import io.ghaylan.validata.intellij.model.PropertyRefHostAttribute
import io.ghaylan.validata.intellij.path.PropertyPathSegment
import io.ghaylan.validata.intellij.path.PropertyPathSegments
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.scope.PropertyRefElementTypeResolver
import io.ghaylan.validata.intellij.scope.PropertyRefOwnerResolver
import io.ghaylan.validata.intellij.scope.PropertyRefSiblingScope
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Attaches one [PropertyRefPsiReference] per path segment in matching `@PropertyRef`
 * annotation arguments (sibling or element scope).
 *
 * ## What
 * Platform [PsiReferenceProvider] that inspects a [KtStringTemplateExpression], decides whether
 * it is a Validata `@PropertyRef` host, resolves the root [PropertyRefSiblingScope], and returns
 * segment references for navigation / rename / variants.
 *
 * ## When it fires
 * Invoked by the platform for Kotlin string templates inside annotation entries registered by
 * [PropertyRefReferenceContributor] (`psi.referenceContributor` in `plugin.xml`). Early-outs
 * when the string has interpolation, is whitespace-only (non-empty raw but blank after trim),
 * or is not a matched `@PropertyRef` attribute.
 *
 * ## Resolve behaviour (Validata terms)
 * - Whitespace-only → no references (`@PropertyRef` blank skip).
 * - Empty `""` → one **soft** reference on the scope root (completion while typing).
 * - `"password"` → one **hard** reference against sibling or element scope.
 * - Element scope (`@Distinct(by=…)`) resolves against the subject’s collection element type.
 * - Handler method parameters use flat sibling parameter names.
 * - Nested dotted paths (`"address.city"`) are **not** supported — only the first segment gets
 *   a non-null scope; later segments are attached with `scope = null` so they fail resolve
 *   (annotator then reports nested refs as errors). Soft empty trailing segments still host
 *   completion caret positions.
 *
 * ## What it is NOT
 * - Not a plugin.xml extension by itself — only used through [PropertyRefReferenceContributor].
 * - Not an annotator (highlighting / errors live in `PropertyRefAnnotator`).
 * - Not enum typed-literal wiring — see [ConstraintEnumLiteralReferenceProvider].*
 * 
 * @author Ghaylan Saada
 */
internal class PropertyRefReferenceProvider : PsiReferenceProvider() {

	/**
	 * Builds `@PropertyRef` segment references for [element] when it is a qualifying host.
	 *
	 * Validata: matches the enclosing annotation argument via
	 * [PropertyRefAnnotationMatcher.matchAttribute], resolves sibling vs element root scope,
	 * then either returns a soft empty-string reference or one reference per path segment.
	 *
	 * @param element candidate PSI (expected [KtStringTemplateExpression])
	 * @param context unused platform processing context
	 * @return segment references, or [PsiReference.EMPTY_ARRAY] when not a Validata host
	 */
	override fun getReferencesByElement(
		element: PsiElement,
		context: ProcessingContext,
	): Array<PsiReference> {
		val stringTemplate = element as? KtStringTemplateExpression ?: return PsiReference.EMPTY_ARRAY
		if (stringTemplate.hasInterpolation()) return PsiReference.EMPTY_ARRAY

		val raw = stringTemplate.entries.joinToString(separator = "") { it.text }
		val trimmed = raw.trim()
		if (raw.isNotEmpty() && trimmed.isEmpty()) return PsiReference.EMPTY_ARRAY

		val argument = stringTemplate.getStrictParentOfType<KtValueArgument>() ?: return PsiReference.EMPTY_ARRAY
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return PsiReference.EMPTY_ARRAY
		val attribute = PropertyRefAnnotationMatcher.matchAttribute(annotation, argument)
			?: return PsiReference.EMPTY_ARRAY

		val rootScope = resolveRootScope(attribute, annotation)
		val valueRange = ElementManipulators.getValueTextRange(stringTemplate)

		if (trimmed.isEmpty()) {
			return arrayOf(
				PropertyRefPsiReference(
					element = stringTemplate,
					scope = rootScope,
					rangeInElement = valueRange,
					soft = true,
				),
			)
		}

		val segments = PropertyPathSegments.segmentsWithRanges(raw)
		if (segments.isEmpty()) return PsiReference.EMPTY_ARRAY

		return buildSegmentReferences(stringTemplate, rootScope, valueRange, segments)
	}

	/**
	 * Chooses the root sibling/element scope from the matched `@PropertyRef` attribute.
	 *
	 * @param attribute discovered host attribute (scope + compatibility metadata)
	 * @param annotation use-site constraint annotation entry
	 * @return resolve scope, or `null` when the annotated subject cannot be resolved
	 */
	private fun resolveRootScope(
		attribute: PropertyRefHostAttribute,
		annotation: KtAnnotationEntry,
	): PropertyRefSiblingScope? =
		when (attribute.scope) {
			PropertyRefScope.SIBLING ->
				PropertyRefOwnerResolver.resolveSiblingScope(annotation)
			PropertyRefScope.ELEMENT ->
				PropertyRefElementTypeResolver.resolveElementScope(annotation)
		}

	/**
	 * Creates one [PropertyRefPsiReference] per [segments] entry.
	 *
	 * Nested paths are invalid — never step into object members after the first segment.
	 * Non-first segments receive `scope = null` so resolve fails until the annotator explains
	 * the nested-path error.
	 *
	 * @param stringTemplate host literal
	 * @param rootScope scope for the first segment only
	 * @param valueRange absolute value range inside the string template (quotes excluded)
	 * @param segments parsed path segments with ranges relative to the value text
	 * @return references in segment order
	 */
	private fun buildSegmentReferences(
		stringTemplate: KtStringTemplateExpression,
		rootScope: PropertyRefSiblingScope?,
		valueRange: TextRange,
		segments: List<PropertyPathSegment>,
	): Array<PsiReference> {
		val refs = ArrayList<PsiReference>(segments.size)
		// Nested paths are invalid — never step into object members after the first segment.
		var scope: PropertyRefSiblingScope? = rootScope

		for ((index, segment) in segments.withIndex()) {
			val rangeInElement = TextRange(
				valueRange.startOffset + segment.rangeInValue.startOffset,
				valueRange.startOffset + segment.rangeInValue.endOffset,
			)
			refs += PropertyRefPsiReference(
				element = stringTemplate,
				scope = if (index == 0) scope else null,
				rangeInElement = rangeInElement,
				soft = segment.soft,
			)

			if (segment.name.isEmpty()) break
			if (index == segments.lastIndex) break
			// Do not resolve nested scopes (KSP / runtime parity: single-segment refs only).
			scope = null
		}

		return refs.toTypedArray()
	}
}
