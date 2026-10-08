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
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Parsed `@ApiError` catalog/code argument pair ready for classify / paint / reference.
 *
 * Immutable snapshot of one use-site. Does not resolve types or validate the code string —
 * callers pass this to [ApiErrorCatalogResolve].
 *
 * @property anchor annotation entry used as the site anchor (fallback highlight when catalog is
 *    missing)
 * @property annotationName short label for diagnostics (always `"@ApiError"` from the factory)
 * @property catalogArgName argument name of the catalog class literal (`"catalog"`)
 * @property codeExpression `code = "…"` string template when present; factory requires it
 * @property catalogExpression `catalog = X::class` (or type ref) when present; may be `null`*
 * 
 * @author Ghaylan Saada
 */
internal data class ApiErrorCatalogCodeSite(
	val anchor: PsiElement,
	val annotationName: String,
	val catalogArgName: String,
	val codeExpression: KtStringTemplateExpression?,
	val catalogExpression: KtExpression?,
) {
	
	/**
	 * Factory helpers for [ApiErrorCatalogCodeSite] from `@ApiError` PSI.	 
	 */
	companion object {
		
		/**
		 * Builds a site from an `@ApiError` annotation entry.
		 *
		 * No side effects. Requires a string `code` argument (named or first positional).
		 * `catalog` may be absent — classify reports [ApiErrorCatalogAnalysis.CodeLiteralKind.BadCatalog].
		 *
		 * @param entry `@ApiError` use-site
		 * @return site when a `code` string argument exists; `null` otherwise		 
		 */
		fun fromApiErrorAnnotation(entry: KtAnnotationEntry): ApiErrorCatalogCodeSite? {
			val args = entry.valueArguments.filterIsInstance<KtValueArgument>()
			val codeExpr = codeExprFromArgs(args)
				?: return null
			val catalogExpr = args.firstOrNull { it.getArgumentName()?.asName?.asString() == "catalog" }
				?.getArgumentExpression()
			return ApiErrorCatalogCodeSite(
				anchor = entry,
				annotationName = "@ApiError",
				catalogArgName = "catalog",
				codeExpression = codeExpr,
				catalogExpression = catalogExpr,
			)
		}
		
		/**
		 * Picks the `code` string argument from [args] (named `code`, else first positional).
		 *
		 * No side effects.
		 *
		 * @param args value arguments of an `@ApiError` entry
		 * @return string template for `code`, or `null` when absent / not a string template		 
		 */
		private fun codeExprFromArgs(args: List<KtValueArgument>): KtStringTemplateExpression? {
			val codeArg = args.firstOrNull { it.getArgumentName()?.asName?.asString() == "code" }
				?: args.firstOrNull { it.getArgumentName() == null }
			return codeArg?.getArgumentExpression() as? KtStringTemplateExpression
		}
	}
}
