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
import io.ghaylan.validata.intellij.analysis.compat.ConstraintSubjectTypeResolver
import io.ghaylan.validata.intellij.analysis.compat.SubjectTypeViews
import io.ghaylan.validata.intellij.analysis.compat.ValidatorTypeCompatibility
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByMissingValidatedBy
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByNotAConstraint
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByOk
import io.ghaylan.validata.intellij.discovery.composition.ConstraintCompositionDiscovery
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAnnotationMatcher
import io.ghaylan.validata.intellij.discovery.validatedby.ConstraintValidatedByDiscovery
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScalarCompatibility
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Flags constraint annotations placed on a subject type they cannot validate.
 *
 * ## What
 * Use-site annotator for Kotlin [KtAnnotationEntry]s that carry Validata `@Constraint`, or
 * composed non-`@Constraint` annotations with nested leaf constraints. Reports errors on the
 * annotation name (callee) when `validatedBy` is missing, when no validator’s `V` fits the
 * annotated subject, when a composed leaf cannot fit, or when an optional
 * `@PropertyRef(compatibility = …)` subject family gate fails for open `V` types.
 *
 * ## Author contract (discovered — no allowlists)
 * 1. `@Constraint(validatedBy = [MyValidator::class, …])`, **or**
 * 2. Composed annotation (nested leaf `@Constraint`s; optional `@ConstraintComposition`)
 * 3. Each leaf validator extends `ConstraintValidator<V, C>` with a **narrow** `V` when possible
 * 4. Optional: metadata `@PropertyRef(compatibility = …)` tightens subject/leaf rules when
 *    `V` is open (`Comparable<*>`, `Any`, …)
 *
 * ## Checks (in order)
 * 1. Ordinary `@Constraint`: at least one `validatedBy` `V` fits the subject
 * 2. Composed: **every** leaf must have at least one `V` that fits (AND and OR)
 * 3. When a sibling `@PropertyRef` compatibility is not [PropertyRefCompatibilityKind.NONE],
 *    [PropertyRefScalarCompatibility.isSubjectCompatible] must also pass
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.annotator` with `language="kotlin"`.
 *
 * ## When it fires
 * On every Kotlin annotation entry during highlighting. Non-constraints that are not composed
 * are ignored. Subject type resolution failures silently skip (incomplete code / unresolved types).
 *
 * ## What it is NOT
 * - Not a path-segment annotator — see [PropertyRefAnnotator].
 * - Not a literal / `@ConstraintArg` checker — see [ConstraintLiteralAnnotator].
 * - Not a metadata-resolution warning — see [ConstraintMetadataUnresolvedAnnotator].
 * - Not OR authoring rules (`<2` members / presence) — see [ConstraintCompositionAnnotator].*
 * 
 * @author Ghaylan Saada
 */
class ConstraintSubjectTypeAnnotator: Annotator {
	
	/**
	 * Validates that [element] (when a constraint or composed annotation) accepts its annotated
	 * subject.
	 *
	 * @param element PSI under highlight ([KtAnnotationEntry] expected)
	 * @param holder annotation destination	 
	 */
	override fun annotate(
		element: PsiElement,
		holder: AnnotationHolder
	) {
		val annotation = element as? KtAnnotationEntry
			?: return
		val shortName = annotation.shortName?.asString()
			?: return
		val range = annotation.calleeExpression?.textRange
			?: annotation.textRange
		
		when (val validatedBy = ConstraintValidatedByDiscovery.analyze(annotation)) {
			ConstraintValidatedByNotAConstraint -> {
				annotateComposedSubject(annotation, shortName, holder, range)
				return
			}
			
			ConstraintValidatedByMissingValidatedBy -> {
				holder.newAnnotation(
					HighlightSeverity.ERROR,
					"@$shortName must declare @Constraint(validatedBy = […]) with " + "ConstraintValidator<V, C> classes that define accepted subject types.",
				)
					.range(range)
					.create()
				return
			}
			
			is ConstraintValidatedByOk -> {
				val subjectView = ConstraintSubjectTypeResolver.typeView(annotation)
					?: return
				if (!ValidatorTypeCompatibility.anyFits(subjectView, validatedBy.valueTypes)) {
					val expected = SubjectTypeViews.displayNames(validatedBy.valueTypes)
					holder.newAnnotation(
						HighlightSeverity.ERROR,
						"@$shortName cannot be applied to '${SubjectTypeViews.displayName(subjectView)}' " + "— no validator in validatedBy accepts this type (expected: $expected).",
					)
						.range(range)
						.create()
					return
				}
			}
		}
		
		annotatePropertyRefSubjectGate(annotation, shortName, holder, range)
	}
	
	/**
	 * Subject-type check for composed annotations: every leaf must accept the subject.
	 *
	 * @param annotation use-site composed annotation
	 * @param shortName annotation short name for messages
	 * @param holder annotation destination
	 * @param range highlight range	 
	 */
	private fun annotateComposedSubject(
		annotation: KtAnnotationEntry,
		shortName: String,
		holder: AnnotationHolder,
		range: com.intellij.openapi.util.TextRange,
	) {
		val composition = ConstraintCompositionDiscovery.analyzeUsage(annotation)
			?: return
		if (!composition.isComposed || composition.constraintLeaves.isEmpty()) return
		val subjectView = ConstraintSubjectTypeResolver.typeView(annotation)
			?: return
		val unfit = composition.constraintLeaves.filter { leaf ->
			when (val vb = leaf.validatedBy) {
				is ConstraintValidatedByOk -> !ValidatorTypeCompatibility.anyFits(subjectView, vb.valueTypes)
				else -> true
			}
		}
		if (unfit.isEmpty()) return
		val subjectName = SubjectTypeViews.displayName(subjectView)
		val leafNames = unfit.joinToString { it.shortName }
		holder.newAnnotation(
			HighlightSeverity.ERROR,
			"@$shortName cannot be applied to '$subjectName' — leaf constraint(s) " + "[$leafNames] have no validator that accepts this type.",
		)
			.range(range)
			.create()
	}
	
	/**
	 * Extra subject-family gate for open `V` (e.g. `@Compare` / `Comparable<*>` +
	 * `COMPARABLE_FAMILY`).
	 *
	 * @param annotation use-site constraint annotation
	 * @param shortName annotation short name for messages
	 * @param holder annotation destination
	 * @param range highlight range (callee / annotation text)	 
	 */
	private fun annotatePropertyRefSubjectGate(
		annotation: KtAnnotationEntry,
		shortName: String,
		holder: AnnotationHolder,
		range: com.intellij.openapi.util.TextRange,
	) {
		val attribute = PropertyRefAnnotationMatcher.matchAnnotationCompatibility(annotation)
			?: return
		if (attribute.compatibilityKind == PropertyRefCompatibilityKind.NONE) return
		val subjectKind = ConstraintSubjectTypeResolver.scalarKind(annotation)
			?: return
		
		if (PropertyRefScalarCompatibility.isSubjectCompatible(attribute.compatibilityKind, subjectKind)) {
			return
		}
		val message = PropertyRefScalarCompatibility.subjectMismatchMessage(
			annotationSimpleName = shortName,
			kind = attribute.compatibilityKind,
			subjectKind = subjectKind,
		)
		holder.newAnnotation(HighlightSeverity.ERROR, message)
			.range(range)
			.create()
	}
}
