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
import io.ghaylan.validata.intellij.analysis.apierror.ApiErrorCatalogAnalysis
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.editor.reference.ApiErrorCatalogReferenceProvider
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Reports `@ApiError` catalog mistakes and paints enum `code` strings like
 * `@In(["ADMIN"])`.
 *
 * ## What / highlights
 * - Blank `code = "…"` → unresolved error (red text)
 * - `catalog` missing, not an enum, or not implementing `ConstraintErrorDefinition` → error
 * - Enum catalog whose constants do not include `code` → unresolved error (red text)
 * - Valid enum `code` → [ConstraintHighlightingColors.ENUM] (platform constant color, not string-green)
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.annotator` with `language="kotlin"`:
 * ```
 * <annotator
 *     language="kotlin"
 *     implementationClass="…editor.annotator.ApiErrorCatalogAnnotator"/>
 * ```
 *
 * ## When it fires
 * On [KtAnnotationEntry] for catalog-level `@ApiError` mistakes (missing / invalid catalog),
 * and on [KtStringTemplateExpression] for `code = …` blank / unknown-constant / enum color.
 * Catalog and code paths are split so a highlight pass does not paint the same literal twice.
 *
 * ## What it is NOT
 * - Not a runtime OpenAPI / envelope checker.
 * - Does not require `:validata-openapi` on unrelated modules — absent presentation annotations
 *   make this annotator a no-op.
 * - Completion / Ctrl+Click come from [ApiErrorCatalogReferenceProvider] (enum PSI references).*
 * 
 * @author Ghaylan Saada
 */
class ApiErrorCatalogAnnotator : Annotator {

	/**
	 * Emits catalog errors and enum colors for `@ApiError` sites related to [element].
	 *
	 * @param element PSI under highlight ([KtAnnotationEntry] or [KtStringTemplateExpression])
	 * @param holder annotation destination
	 */
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		when (element) {
			is KtAnnotationEntry -> annotateCatalogOnly(element, holder)
			is KtStringTemplateExpression -> annotateCodeString(element, holder)
		}
	}

	/**
	 * Reports catalog-level errors (missing / non-enum / non-implementing catalog) for one
	 * `@ApiError` [annotation]. Code-string errors and enum colors are handled only in
	 * [annotateCodeString] so a highlight pass does not paint the same literal twice.
	 *
	 * @param annotation presentation annotation entry
	 * @param holder annotation destination
	 */
	private fun annotateCatalogOnly(annotation: KtAnnotationEntry, holder: AnnotationHolder) {
		if (!ApiErrorCatalogAnalysis.isPresentationRoot(annotation)) return
		for (finding in ApiErrorCatalogAnalysis.findings(annotation)) {
			// Code-string findings are owned by [annotateCodeString].
			if (finding.highlightElement is KtStringTemplateExpression) continue
			holder.newAnnotation(HighlightSeverity.ERROR, finding.message)
				.range(highlightRange(finding.highlightElement))
				.create()
		}
	}

	/**
	 * Classifies and paints a single `code = "…"` string host when it belongs to `@ApiError`.
	 *
	 * Side effects: writes annotations into [holder].
	 *
	 * @param codeExpr candidate string template
	 * @param holder annotation destination
	 */
	private fun annotateCodeString(codeExpr: KtStringTemplateExpression, holder: AnnotationHolder) {
		if (codeExpr.hasInterpolation()) return
		when (val kind = ApiErrorCatalogAnalysis.classifyCodeLiteral(codeExpr) ?: return) {
			is ApiErrorCatalogAnalysis.CodeLiteralKind.Blank -> {
				holder.newAnnotation(
					HighlightSeverity.ERROR,
					"${kind.annotationName} code must not be blank.",
				)
					.range(highlightRange(codeExpr))
					.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
					.create()
			}
			is ApiErrorCatalogAnalysis.CodeLiteralKind.EnumInvalid -> {
				holder.newAnnotation(HighlightSeverity.ERROR, kind.message)
					.range(highlightRange(codeExpr))
					.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
					.create()
			}
			is ApiErrorCatalogAnalysis.CodeLiteralKind.EnumValid -> {
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range(highlightRange(codeExpr))
					.textAttributes(ConstraintHighlightingColors.ENUM)
					.create()
			}
			is ApiErrorCatalogAnalysis.CodeLiteralKind.BadCatalog,
			is ApiErrorCatalogAnalysis.CodeLiteralKind.Unresolved -> Unit
		}
	}

	/**
	 * Absolute highlight range: value text inside quotes for strings, full range otherwise.
	 *
	 * @param element PSI to highlight
	 * @return range covering the unquoted value for string templates, else [PsiElement.textRange]
	 */
	private fun highlightRange(element: PsiElement): TextRange {
		val string = element as? KtStringTemplateExpression
		if (string != null && !string.hasInterpolation()) {
			val valueRange = ElementManipulators.getValueTextRange(string)
			return TextRange(
				string.textRange.startOffset + valueRange.startOffset,
				string.textRange.startOffset + valueRange.endOffset,
			)
		}
		return element.textRange
	}
}
