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
import io.ghaylan.validata.intellij.scope.PropertyRefSiblingScope
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * PSI reference from one path **segment** inside a `@PropertyRef` string literal to a sibling
 * property, constructor parameter, or handler method parameter in [scope].
 *
 * ## What
 * Each instance covers a contiguous [rangeInElement] of a [KtStringTemplateExpression] (the
 * segment spelling, not the surrounding quotes). Resolve looks up that spelling in [scope];
 * completion variants list every member of the same scope with the owner type as lookup type
 * text.
 *
 * ## Why
 * IntelliJ navigation (Go to Declaration), rename, and reference-based completion need a real
 * [PsiReference] per segment. Annotators (`PropertyRefAnnotator`) and completion (`PropertyRefCompletionProvider`)
 * consume these instances rather than re-implementing resolve.
 *
 * ## Wiring (not a plugin.xml EP itself)
 * Created only by `PropertyRefReferenceProvider`,
 * which is registered via `psi.referenceContributor` → `PropertyRefReferenceContributor`. Nested dotted
 * paths attach one instance per segment; only the first segment receives a non-null [scope]
 * (KSP / runtime parity: single-segment refs only).
 *
 * ## What it is NOT
 * - Not a JSONPath / deep object walker — nested scopes are never stepped into here.
 * - Not an annotator or highlighter (those read this reference’s resolve results).
 * - Not responsible for discovering whether a string is a `@PropertyRef` host — the provider
 *   decides that before constructing this class.
 *
 * Soft empty `""` hosts completion while typing; when [scope] is `null` (previous segment
 * unresolved / non-object, or a non-first dotted segment), resolve fails and variants are empty.
 *
 * @param element host string template (constraint argument value)
 * @param rangeInElement segment range inside [element] (defaults to the full value text)
 * @param soft when `true`, unresolved is not treated as a hard broken reference by the platform
 * @property scope sibling or element owner to resolve against, or `null` to force failed resolve*
 * 
 * @author Ghaylan Saada
 */
internal class PropertyRefPsiReference(
	element: KtStringTemplateExpression,
	private val scope: PropertyRefSiblingScope?,
	rangeInElement: TextRange = ElementManipulators.getValueTextRange(element),
	soft: Boolean = false,
): PsiReferenceBase<KtStringTemplateExpression>(element, rangeInElement, soft), PsiPolyVariantReference {
	
	/**
	 * Single-result resolve for Go to Declaration and related IDE actions.
	 *
	 * Validata: returns the first (and only) [multiResolve] hit — the Kotlin property /
	 * parameter named by this segment in [scope] — or `null` when the segment is blank,
	 * [scope] is missing, or the name is unknown.
	 *
	 * @return target declaration, or `null` when unresolved	 
	 */
	override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element
	
	/**
	 * Poly-variant resolve used by the platform when incomplete code is allowed.
	 *
	 * Validata: trims the segment text and asks [PropertyRefSiblingScope.findMember]. At most
	 * one result is ever returned (sibling names are unique in a given scope).
	 *
	 * @param incompleteCode ignored — Validata does not soften lookup for incomplete identifiers
	 * @return zero or one [PsiElementResolveResult] wrapping the member PSI	 
	 */
	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
		val owner = scope
			?: return ResolveResult.EMPTY_ARRAY
		val name = value.trim()
		if (name.isEmpty()) return ResolveResult.EMPTY_ARRAY
		val target = owner.findMember(name)
			?: return ResolveResult.EMPTY_ARRAY
		return arrayOf(PsiElementResolveResult(target))
	}
	
	/**
	 * Completion candidates for this segment’s caret position.
	 *
	 * Validata: builds [LookupElementBuilder] entries from [PropertyRefSiblingScope.listMembers],
	 * using each member’s icon and [PropertyRefSiblingScope.typeText] as the type label (e.g.
	 * DTO class name or `"parameters"` for flat handler params).
	 *
	 * @return lookup elements, or empty when [scope] is `null` or has no named members	 
	 */
	override fun getVariants(): Array<Any> {
		val owner = scope
			?: return emptyArray()
		val typeLabel = owner.typeText()
		return owner.listMembers()
			.mapNotNull { member ->
				val name = member.name
					?: return@mapNotNull null
				LookupElementBuilder.create(member, name)
					.withIcon(member.getIcon(0))
					.withTypeText(typeLabel, true)
			}
			.toTypedArray()
	}
	
	/**
	 * Applies a rename of the referenced member into this string segment.
	 *
	 * Validata: updates only [rangeInElement] via [ElementManipulators.handleContentChange]
	 * so dotted multi-segment hosts (even if invalid) keep other segments intact.
	 *
	 * @param newElementName new Kotlin member name to write into the string
	 * @return the mutated host [PsiElement]	 
	 */
	override fun handleElementRename(newElementName: String): PsiElement {
		return ElementManipulators.handleContentChange(element, rangeInElement, newElementName)
	}
}
