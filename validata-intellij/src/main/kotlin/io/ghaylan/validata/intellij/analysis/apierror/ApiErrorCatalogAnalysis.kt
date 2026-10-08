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

package io.ghaylan.validata.intellij.analysis.apierror

import com.intellij.psi.PsiElement
import io.ghaylan.validata.intellij.contract.OpenApiPresentationFqns
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * IDE twin of KSP `ApiErrorCatalogVerifier` for presentation annotations.
 *
 * **What.** Walks `@ApiError` use-sites and classifies each `code = "…"` literal
 * (blank / enum-valid / enum-invalid / bad catalog).
 *
 * Site parsing lives in [ApiErrorCatalogCodeSite]; classify / resolve in [ApiErrorCatalogResolve].
 *
 * **Not.** Not a runtime OpenAPI check. No-ops when presentation annotations are absent from
 * the project classpath.*
 * 
 * @author Ghaylan Saada
 */
internal object ApiErrorCatalogAnalysis {
	
	/**
	 * How an `@ApiError` `code = "…"` literal should be painted / treated.
	 */
	sealed class CodeLiteralKind {
		
		data class Blank(
			val codeExpr: KtStringTemplateExpression,
			val annotationName: String,
		): CodeLiteralKind()
		
		data class EnumValid(
			val codeExpr: KtStringTemplateExpression,
			val enumClass: PsiElement,
		): CodeLiteralKind()
		
		data class EnumInvalid(
			val codeExpr: KtStringTemplateExpression,
			val enumClass: PsiElement,
			val code: String,
			val message: String,
		): CodeLiteralKind()
		
		data class BadCatalog(
			val highlightElement: PsiElement,
			val message: String,
		): CodeLiteralKind()
		
		data object Unresolved: CodeLiteralKind()
	}
	
	/**
	 * One diagnostic for the annotator to paint.
	 *
	 * @property message user-facing error text (kept close to KSP wording)
	 * @property highlightElement PSI range host (usually the `code` string)
	 */
	data class Finding(
		val message: String,
		val highlightElement: PsiElement,
	)
	
	/** Whether [annotation] is a presentation annotation that may contain catalog sites.	 */
	fun isPresentationRoot(annotation: KtAnnotationEntry): Boolean = isApiError(annotation)
	
	/** Whether [annotation] is `@ApiError`.	 */
	fun isApiError(annotation: KtAnnotationEntry): Boolean = matchesFqOrShort(annotation, OpenApiPresentationFqns.API_ERROR, "ApiError")
	
	/** Collects catalog findings for [annotation] and nested sites.	 */
	fun findings(annotation: KtAnnotationEntry): List<Finding> {
		if (!isPresentationRoot(annotation)) return emptyList()
		val out = ArrayList<Finding>()
		for (site in collectSites(annotation)) {
			when (val kind = ApiErrorCatalogResolve.classify(site)) {
				is CodeLiteralKind.Blank -> out += Finding("${site.annotationName} code must not be blank.", kind.codeExpr)
				is CodeLiteralKind.EnumInvalid -> out += Finding(kind.message, kind.codeExpr)
				is CodeLiteralKind.BadCatalog -> out += Finding(kind.message, kind.highlightElement)
				else -> Unit
			}
		}
		return out
	}
	
	/** Classifies a single `code` string literal for coloring / errors / references.	 */
	fun classifyCodeLiteral(codeExpr: KtStringTemplateExpression): CodeLiteralKind? {
		val site = locateCodeSite(codeExpr)
			?: return null
		return ApiErrorCatalogResolve.classify(site)
	}
	
	/**
	 * Resolved enum class for the catalog of a site that hosts [codeExpr], or `null` when
	 * unresolved / not a valid enum catalog.	 
	 */
	fun enumCatalogForCodeLiteral(codeExpr: KtStringTemplateExpression): PsiElement? = when (val kind = classifyCodeLiteral(codeExpr)) {
		is CodeLiteralKind.EnumValid -> kind.enumClass
		is CodeLiteralKind.EnumInvalid -> kind.enumClass
		else -> null
	}
	
	private fun collectSites(annotation: KtAnnotationEntry): List<ApiErrorCatalogCodeSite> {
		val out = ArrayList<ApiErrorCatalogCodeSite>()
		if (isApiError(annotation)) {
			ApiErrorCatalogCodeSite.fromApiErrorAnnotation(annotation)
				?.let { out += it }
		}
		return out
	}
	
	private fun locateCodeSite(codeExpr: KtStringTemplateExpression): ApiErrorCatalogCodeSite? {
		val argument = codeExpr.getStrictParentOfType<KtValueArgument>()
			?: return null
		val name = argument.getArgumentName()?.asName?.asString()
		if (name != null && name != "code") return null
		var annotationProbe: PsiElement? = argument
		while (annotationProbe != null) {
			val annotation = annotationProbe.getStrictParentOfType<KtAnnotationEntry>()
				?: break
			when {
				isApiError(annotation) -> return ApiErrorCatalogCodeSite.fromApiErrorAnnotation(annotation)
			}
			annotationProbe = annotation.parent
		}
		return null
	}
	
	private fun matchesFqOrShort(
		annotation: KtAnnotationEntry,
		fqcn: String,
		shortName: String,
	): Boolean {
		val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
		val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
		if (fq == fqcn) return true
		if (annotation.shortName?.asString() != shortName) return false
		return fq == null || fq.endsWith(".$shortName")
	}
}
