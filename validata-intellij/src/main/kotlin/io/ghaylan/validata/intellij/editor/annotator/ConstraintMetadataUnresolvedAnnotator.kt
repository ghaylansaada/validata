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
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Warns when a use-site annotation carries `@Constraint` but its generated
 * `{Name}Constraint` metadata class cannot be resolved — discovery cannot find
 * `@PropertyRef` hosts, so path DX is silent.
 *
 * ## What
 * Emits a [HighlightSeverity.WEAK_WARNING] on the annotation callee when
 * [PropertyRefAttributeDiscovery.unresolvedConstraintMetadataMessage] returns a message.
 * That happens when the constraint is recognized but its metadata type is missing /
 * unresolved in the IDE indexes.
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.annotator` with `language="kotlin"`:
 * ```
 * <annotator
 *     language="kotlin"
 *     implementationClass="…editor.annotator.ConstraintMetadataUnresolvedAnnotator"/>
 * ```
 *
 * ## When it fires
 * On Kotlin [KtAnnotationEntry] highlighting. No annotation is created when discovery returns
 * `null` (ordinary annotations, or constraints whose metadata resolves — even if that
 * metadata has no `@PropertyRef` fields).
 *
 * ## What it is NOT
 * - Does **not** warn for ordinary annotations or for constraints whose metadata resolves but
 *   has no `@PropertyRef` fields (e.g. a message-only custom constraint).
 * - Not a subject-type or path-segment checker — see [ConstraintSubjectTypeAnnotator] /
 *   [PropertyRefAnnotator].
 * - Not an error-level gate; unresolved metadata is a weak warning so incomplete projects
 *   stay editable.*
 * 
 * @author Ghaylan Saada
 */
class ConstraintMetadataUnresolvedAnnotator: Annotator {
	
	/**
	 * Reports a weak warning when constraint metadata cannot be resolved for [element].
	 *
	 * Validata: delegates message construction entirely to
	 * [PropertyRefAttributeDiscovery.unresolvedConstraintMetadataMessage]; highlights the
	 * callee expression range when present.
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
		val message = PropertyRefAttributeDiscovery.unresolvedConstraintMetadataMessage(annotation)
			?: return
		val range = annotation.calleeExpression?.textRange
			?: annotation.textRange
		holder.newAnnotation(HighlightSeverity.WEAK_WARNING, message)
			.range(range)
			.create()
	}
}
