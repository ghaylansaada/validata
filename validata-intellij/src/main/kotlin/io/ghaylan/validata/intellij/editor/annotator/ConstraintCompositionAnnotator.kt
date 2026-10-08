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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import io.ghaylan.validata.intellij.discovery.composition.CompositionAnalysis
import io.ghaylan.validata.intellij.discovery.composition.CompositionModeView
import io.ghaylan.validata.intellij.discovery.composition.ConstraintCompositionDiscovery
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Kotlin annotator for illegal `@ConstraintComposition(OR)` authoring on composed annotation
 * declarations (KSP T6 parity).
 *
 * Reports errors on the `@ConstraintComposition` entry when OR has fewer than two leaf
 * `@Constraint` members, includes presence (`Required` / `RequiredWhen`), or nests another
 * composed annotation (v1). AND / unmarked composition is not inspected here.
 *
 * Registered in `plugin.xml` as `com.intellij.annotator` with `language="kotlin"`.
 *
 * Not a use-site subject-type checker ([ConstraintSubjectTypeAnnotator]).
 * Not a leaf discovery API — see [ConstraintCompositionDiscovery].*
 * 
 * @author Ghaylan Saada
 */
class ConstraintCompositionAnnotator: Annotator {
	
	/**
	 * Reports OR composition authoring errors when [element] is `@ConstraintComposition` on an
	 * annotation class in OR mode.
	 *
	 * Side effects: may write error annotations into [holder]. No-op for non-composition
	 * entries, non-annotation owners, AND mode, or when [element] is not a [KtAnnotationEntry].
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
		if (!ConstraintCompositionDiscovery.isConstraintComposition(annotation)) return
		val owner = annotation.getStrictParentOfType<KtClass>()
			?: return
		if (!owner.isAnnotation()) return
		if (!owner.annotationEntries.contains(annotation)) return
		val analysis = ConstraintCompositionDiscovery.analyzeDeclaration(owner)
		if (analysis.mode != CompositionModeView.OR) return
		val composedName = owner.name
			?: "composed"
		val range = annotation.calleeExpression?.textRange
			?: annotation.textRange
		reportOrIssues(composedName, range, analysis, holder)
	}
	
	/**
	 * Emits the first applicable OR authoring error for [analysis] onto [holder].
	 *
	 * Precedence: nested composed → fewer than two leaves → presence members. Side effects:
	 * creates at most one error annotation on [range] (presence may still fire after leaf-count
	 * passes).
	 *
	 * @param composedName simple name of the composed annotation class (fallback `"composed"`)
	 * @param range highlight range on the `@ConstraintComposition` entry
	 * @param analysis declaration-site composition result in OR mode
	 * @param holder annotation destination	 
	 */
	private fun reportOrIssues(
		composedName: String,
		range: TextRange,
		analysis: CompositionAnalysis,
		holder: AnnotationHolder,
	) {
		val nested = analysis.leaves.filter { it.isNestedComposed }
		if (nested.isNotEmpty()) {
			holder.newAnnotation(
				HighlightSeverity.ERROR,
				"@$composedName uses @ConstraintComposition(OR) but nests composed annotation " + "'${nested.joinToString { it.shortName }}'. OR members must be leaf @Constraint " + "annotations (v1).",
			)
				.range(range)
				.create()
			return
		}
		val constraintLeaves = analysis.constraintLeaves
		if (constraintLeaves.size < 2) {
			holder.newAnnotation(
				HighlightSeverity.ERROR,
				"@$composedName uses @ConstraintComposition(OR) but declares ${constraintLeaves.size} " + "leaf @Constraint member(s); OR requires at least 2.",
			)
				.range(range)
				.create()
			return
		}
		val presence = constraintLeaves.filter { it.isPresence }
		if (presence.isNotEmpty()) {
			holder.newAnnotation(
				HighlightSeverity.ERROR,
				"@$composedName uses @ConstraintComposition(OR) but includes presence constraint(s) " + "${presence.joinToString { it.shortName }}. Presence must stay outside OR.",
			)
				.range(range)
				.create()
		}
	}
}
