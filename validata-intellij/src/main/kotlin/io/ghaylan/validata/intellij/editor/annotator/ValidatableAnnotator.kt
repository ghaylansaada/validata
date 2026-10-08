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
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import io.ghaylan.validata.intellij.analysis.literal.ConstraintEnumLiteralSupport
import io.ghaylan.validata.intellij.analysis.literal.ConstraintLiteralFormats
import io.ghaylan.validata.intellij.analysis.validatable.ValidatableAnnotationAnalysis
import io.ghaylan.validata.intellij.analysis.validatable.ValidatableDiscriminatorSupport
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Reports `@Validatable` polymorphism authoring mistakes that KSP also rejects at compile time,
 * and recolors valid `Subtype(name = "…")` literals like constraint typed literals.
 *
 * ## What / highlights
 * - Non-blank `discriminator = "…"` that does not name a property on the annotated type →
 *   error on the string literal (red unresolved text)
 * - Non-blank `discriminator = "…"` naming a non-scalar property → error **underline only**
 *   (default text color kept)
 * - `Subtype(type = X::class)` where `X` does not extend / implement the annotated parent →
 *   error **underline only** on the class literal (default text color kept)
 * - `Subtype(type = Parent::class)` (same type as the parent) → underline-only error
 * - `Subtype(name = "…")` that is not a valid typed literal of the discriminator property type
 *   (enum / number / temporal / string) → error on the name string (red unresolved text)
 * - Valid name literals get number / temporal / enum colors ([ConstraintHighlightingColors]);
 *   plain string discriminators keep default string coloring
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.annotator` with `language="kotlin"`:
 * ```
 * <annotator
 *     language="kotlin"
 *     implementationClass="…editor.annotator.ValidatableAnnotator"/>
 * ```
 *
 * ## When it fires
 * On Kotlin [KtAnnotationEntry] highlighting for `@Validatable` applied to a class / interface.
 * Logic lives in [ValidatableAnnotationAnalysis] (KSP `PolymorphicSubtypeReconciler` parity).
 *
 * ## What it is NOT
 * - Not a Jackson `@JsonSubTypes` checker — Validata owns its own subtype list.
 * - Not a property-path / `@Constraint` annotator.*
 * 
 * @author Ghaylan Saada
 */
class ValidatableAnnotator: Annotator {
	
	/**
	 * Emits errors for invalid `@Validatable` discriminator / subtype entries on [element],
	 * and recolors valid subtype-name typed literals.
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
		if (!ValidatableAnnotationAnalysis.isValidatable(annotation)) return
		val owner = annotation.getStrictParentOfType<KtClass>()
			?: return
		if (!owner.annotationEntries.contains(annotation)) return
		val errorHosts = HashSet<PsiElement>()
		for (finding in ValidatableAnnotationAnalysis.findings(annotation)) {
			errorHosts += finding.highlightElement
			val builder = holder.newAnnotation(HighlightSeverity.ERROR, finding.message)
				.range(highlightRange(finding.highlightElement))
			if (finding.style == ValidatableAnnotationAnalysis.HighlightStyle.UNRESOLVED) {
				builder.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
			}
			builder.create()
		}
		
		colorValidSubtypeNames(annotation, owner, errorHosts, holder)
	}
	
	/**
	 * Recolors valid `Subtype(name = "…")` strings using the discriminator property type
	 * (same keys as [ConstraintLiteralAnnotator]).
	 *
	 * Side effects: writes silent color annotations into [holder].
	 *
	 * @param annotation `@Validatable` use-site
	 * @param owner annotated class / interface
	 * @param errorHosts elements already marked as errors (skipped for recolor)
	 * @param holder annotation destination	 
	 */
	private fun colorValidSubtypeNames(
		annotation: KtAnnotationEntry,
		owner: KtClass,
		errorHosts: Set<PsiElement>,
		holder: AnnotationHolder,
	) {
		if (!ValidatableDiscriminatorSupport.isTypedLiteralScalar(annotation, owner)) return
		val typeFq = ValidatableDiscriminatorSupport.discriminatorTypeFq(annotation, owner)
			?: return
		val enumNames = enumConstantNames(annotation, typeFq)
		for ((nameExpr, raw) in ValidatableAnnotationAnalysis.subtypeNameLiterals(annotation)) {
			if (nameExpr in errorHosts) continue
			if (raw.isBlank()) continue
			val key = when {
				enumNames != null || typeFq == "java.time.Month" -> ConstraintHighlightingColors.ENUM
				ConstraintLiteralFormats.isNumericType(typeFq) -> ConstraintHighlightingColors.NUMBER
				ConstraintLiteralFormats.isTemporalType(typeFq) -> ConstraintHighlightingColors.TEMPORAL
				ConstraintLiteralFormats.isStringType(typeFq) -> continue
				else -> continue
			}
			holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
				.range(highlightRange(nameExpr))
				.textAttributes(key)
				.create()
		}
	}
	
	/**
	 * Absolute highlight range: value text inside quotes for strings, full range otherwise.
	 *
	 * @param element PSI to highlight
	 * @return range covering the unquoted value for string templates, else [PsiElement.textRange]	 
	 */
	private fun highlightRange(element: PsiElement): TextRange {
		if (element is KtStringTemplateExpression) {
			val valueRange = ElementManipulators.getValueTextRange(element)
			return TextRange(
				element.textRange.startOffset + valueRange.startOffset,
				element.textRange.startOffset + valueRange.endOffset,
			)
		}
		return element.textRange
	}
	
	/**
	 * Enum constant names for typed-literal coloring when [typeFq] is an enum.
	 *
	 * Non-[Month] temporals return `null` (ISO hosts). [Month] returns names so subtype /
	 * discriminator literals use [ConstraintHighlightingColors.ENUM].
	 *
	 * @param annotation resolve scope host for enum lookup
	 * @param typeFq discriminator property type FQCN
	 * @return constant names when [typeFq] is an enum catalog host; `null` otherwise	 
	 */
	private fun enumConstantNames(
		annotation: KtAnnotationEntry,
		typeFq: String
	): Set<String>? {
		if (ConstraintLiteralFormats.isTemporalType(typeFq) && typeFq != "java.time.Month") {
			return null
		}
		val enumClass = ConstraintEnumLiteralSupport.findEnumClass(annotation, typeFq)
			?: return null
		return ConstraintEnumLiteralSupport.constantNames(enumClass)
	}
}
