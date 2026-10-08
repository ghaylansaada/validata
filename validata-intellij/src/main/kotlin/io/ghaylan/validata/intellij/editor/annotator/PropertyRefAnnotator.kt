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

package io.ghaylan.validata.intellij.editor.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import io.ghaylan.validata.intellij.analysis.compat.PropertyRefScalarKinds
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAnnotationMatcher
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.scope.PropertyRefOwnerResolver
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScalarCompatibility
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Kotlin annotator for `@PropertyRef` path segments inside constraint string literals.
 *
 * Paints resolved segments with [ConstraintHighlightingColors.PROPERTY_REF], unresolved names
 * with [ConstraintHighlightingColors.UNRESOLVED], and reports self-reference, nested paths, and
 * scalar-kind mismatches as errors (underline-only when the name already resolved). Soft / empty
 * segments are ignored. Consumes [PropertyRefPsiReference] instances from the reference
 * contributor — does not attach references itself.
 *
 * Registered in `plugin.xml` as `com.intellij.annotator` with `language="kotlin"`.
 *
 * Not a subject-type / `validatedBy` checker ([ConstraintSubjectTypeAnnotator]).
 * Not a `@ConstraintArg` literal checker ([ConstraintLiteralAnnotator]).*
 * 
 * @author Ghaylan Saada
 */
class PropertyRefAnnotator : Annotator {

	/**
	 * Highlights and validates `@PropertyRef` segments on [element] when applicable.
	 *
	 * Side effects: writes silent info, error, and text-attribute annotations into [holder].
	 * No-op when [element] is not a non-interpolated [KtStringTemplateExpression] carrying
	 * hard [PropertyRefPsiReference]s.
	 *
	 * @param element PSI under highlight (string templates only are considered)
	 * @param holder annotation destination
	 */
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		if (element !is KtStringTemplateExpression) return
		if (element.hasInterpolation()) return

		val refs = element.references.filterIsInstance<PropertyRefPsiReference>()
		if (refs.isEmpty()) return

		val hardRefs = refs.filter { !it.isSoft && it.value.trim().isNotEmpty() }
		val paint = paintHardSegments(element, refs, holder)
		if (hardRefs.size > 1) {
			reportNestedPath(element, holder)
			return
		}
		if (!paint.selfReference && paint.allHardResolved && paint.lastHardResolved != null) {
			annotateTypeCompatibility(element, paint.lastHardResolved, holder)
		}
	}

	/**
	 * Outcome of painting hard path segments on one string template.
	 *
	 * @property allHardResolved `true` when every hard non-empty segment resolved
	 * @property lastHardResolved last hard segment that resolved and was not a self-reference;
	 *    used for scalar compatibility when the path is a single segment
	 * @property selfReference `true` when any hard segment named the annotated sibling subject
	 */
	private data class SegmentPaint(
		val allHardResolved: Boolean,
		val lastHardResolved: PropertyRefPsiReference?,
		val selfReference: Boolean,
	)

	/**
	 * Paints each hard [PropertyRefPsiReference] on [element] and aggregates resolve outcome.
	 *
	 * Side effects: creates silent PROPERTY_REF, unresolved-red, and self-ref error annotations
	 * on [holder]. Soft / empty segments are skipped.
	 *
	 * @param element host string template
	 * @param refs all property-ref references on [element] (soft and hard)
	 * @param holder annotation destination
	 * @return paint outcome for nested-path / compatibility follow-up
	 */
	private fun paintHardSegments(
		element: KtStringTemplateExpression,
		refs: List<PropertyRefPsiReference>,
		holder: AnnotationHolder,
	): SegmentPaint {
		var allHardResolved = true
		var lastHardResolved: PropertyRefPsiReference? = null
		var selfReference = false
		for (ref in refs) {
			if (ref.isSoft) continue
			val name = ref.value.trim()
			if (name.isEmpty()) continue
			val range = ref.absoluteRange
			val resolved = ref.resolve()
			if (resolved != null) {
				// Keep instance-field color even when a semantic rule fails below.
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range(range)
					.textAttributes(ConstraintHighlightingColors.PROPERTY_REF)
					.create()
				if (isSiblingSelfReference(element, name, resolved)) {
					selfReference = true
					holder.newAnnotation(
						HighlightSeverity.ERROR,
						"Constraint cannot reference the annotated property itself ('$name')",
					).range(range).create()
				} else {
					lastHardResolved = ref
				}
			} else {
				allHardResolved = false
				holder.newAnnotation(HighlightSeverity.ERROR, "Cannot resolve property '$name'")
					.range(range)
					.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
					.create()
			}
		}
		return SegmentPaint(allHardResolved, lastHardResolved, selfReference)
	}

	/**
	 * Reports that nested dotted `@PropertyRef` paths are unsupported.
	 *
	 * Side effects: creates one error annotation spanning [element].
	 *
	 * @param element host string with more than one hard segment
	 * @param holder annotation destination
	 */
	private fun reportNestedPath(element: KtStringTemplateExpression, holder: AnnotationHolder) {
		holder.newAnnotation(
			HighlightSeverity.ERROR,
			"Nested property references are not supported — use a same-object field name " +
				"(or a single field of the collection element for @Distinct(by = …))",
		).range(element.textRange).create()
	}

	/**
	 * Whether a sibling-scope `@PropertyRef` illegally names the annotated member itself.
	 *
	 * Element-scope paths (`@Distinct(by=…)`) are relative to the element type and are never
	 * treated as self-references of the collection property. No side effects.
	 *
	 * @param stringTemplate host literal
	 * @param pathSegment trimmed segment spelling
	 * @param resolved PSI target of the segment reference
	 * @return `true` when the path points at the annotated sibling subject
	 */
	private fun isSiblingSelfReference(
		stringTemplate: KtStringTemplateExpression,
		pathSegment: String,
		resolved: PsiElement,
	): Boolean {
		val argument = stringTemplate.getStrictParentOfType<KtValueArgument>() ?: return false
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return false
		val attribute = PropertyRefAnnotationMatcher.matchAttribute(annotation, argument) ?: return false
		if (attribute.scope != PropertyRefScope.SIBLING) return false

		val subject = PropertyRefOwnerResolver.findAnnotatedSubject(annotation) as? KtNamedDeclaration
			?: return false
		val subjectName = subject.name ?: return false
		if (pathSegment == subjectName) return true
		val resolvedDecl = resolved as? KtNamedDeclaration ?: return false
		return resolvedDecl == subject || resolvedDecl.navigationElement == subject.navigationElement
	}

	/**
	 * Errors when `@PropertyRef(compatibility = …)` rejects the annotated subject vs leaf kind.
	 *
	 * Uses underline-only error highlighting so the resolved name keeps
	 * [ConstraintHighlightingColors.PROPERTY_REF]. Skips when compatibility is
	 * [PropertyRefCompatibilityKind.NONE] or kinds cannot be inferred. Side effects: may create
	 * one error annotation on [leafRef]'s range.
	 *
	 * @param stringTemplate host literal
	 * @param leafRef the single hard resolved segment reference
	 * @param holder annotation destination
	 */
	private fun annotateTypeCompatibility(
		stringTemplate: KtStringTemplateExpression,
		leafRef: PropertyRefPsiReference,
		holder: AnnotationHolder,
	) {
		val argument = stringTemplate.getStrictParentOfType<KtValueArgument>() ?: return
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return
		val attribute = PropertyRefAnnotationMatcher.matchAttribute(annotation, argument) ?: return
		if (attribute.compatibilityKind == PropertyRefCompatibilityKind.NONE) return

		val subject = PropertyRefOwnerResolver.findAnnotatedSubject(annotation) as? KtNamedDeclaration
			?: return
		val leaf = leafRef.resolve() as? KtNamedDeclaration ?: return

		val annotatedKind = PropertyRefScalarKinds.kindOf(subject) ?: return
		val referencedKind = PropertyRefScalarKinds.kindOf(leaf) ?: return

		if (PropertyRefScalarCompatibility.isCompatible(
				attribute.compatibilityKind,
				annotatedKind,
				referencedKind,
			)
		) {
			return
		}

		val message = PropertyRefScalarCompatibility.mismatchMessage(
			attribute.compatibilityKind,
			annotatedKind,
			referencedKind,
		)
		holder.newAnnotation(HighlightSeverity.ERROR, message)
			.range(leafRef.absoluteRange)
			.create()
	}
}
