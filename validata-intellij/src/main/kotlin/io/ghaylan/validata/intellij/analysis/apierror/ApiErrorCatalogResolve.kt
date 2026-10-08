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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import io.ghaylan.validata.intellij.analysis.apierror.ApiErrorCatalogResolve.resolveTypeReference
import io.ghaylan.validata.intellij.analysis.literal.ConstraintEnumLiteralSupport
import io.ghaylan.validata.intellij.contract.OpenApiPresentationFqns
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Resolves `@ApiError` catalog types and classifies `code` literals for paint / errors.
 *
 * IDE twin of KSP `ApiErrorCatalogVerifier` classify rules. Extracted from
 * [ApiErrorCatalogAnalysis] so site walking stays separate from resolve / paint classification.
 * Pure analysis — no PSI mutation and no [AnnotationHolder] writes.
 *
 * Catalog must be an enum class implementing `ConstraintErrorDefinition`
 * ([OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION]).*
 * 
 * @author Ghaylan Saada
 */
internal object ApiErrorCatalogResolve {
	
	/**
	 * Max enum constant names listed in an invalid-code diagnostic before truncating with `…`.	 
	 */
	const val MAX_NAMES_IN_MESSAGE = 12
	
	/**
	 * Classifies one parsed [ApiErrorCatalogCodeSite] into a
	 * [ApiErrorCatalogAnalysis.CodeLiteralKind].
	 *
	 * No side effects. Order: missing/unreadable code → blank → missing catalog → unresolved
	 * catalog → non-enum / non-`ConstraintErrorDefinition` → enum membership.
	 *
	 * @param site parsed `@ApiError` catalog/code pair
	 * @return paint / error kind for the site	 
	 */
	fun classify(site: ApiErrorCatalogCodeSite): ApiErrorCatalogAnalysis.CodeLiteralKind {
		val codeExpr = site.codeExpression
			?: return ApiErrorCatalogAnalysis.CodeLiteralKind.Unresolved
		val code = rawString(codeExpr)
			?: return ApiErrorCatalogAnalysis.CodeLiteralKind.Unresolved
		if (code.isBlank()) {
			return ApiErrorCatalogAnalysis.CodeLiteralKind.Blank(codeExpr, site.annotationName)
		}
		val catalogExpr = site.catalogExpression
			?: return ApiErrorCatalogAnalysis.CodeLiteralKind.BadCatalog(
				highlightElement = site.anchor,
				message = "${site.annotationName} ${site.catalogArgName} is required — pass an enum class " + "that implements ${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}.",
			)
		val catalog = resolveCatalog(site)
			?: return ApiErrorCatalogAnalysis.CodeLiteralKind.Unresolved
		if (!isEnumClass(catalog)) {
			return ApiErrorCatalogAnalysis.CodeLiteralKind.BadCatalog(
				highlightElement = catalogExpr,
				message = "${site.annotationName} ${site.catalogArgName} must be an enum class implementing " + "${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}, but was '${
					catalogName(catalog)
				}'.",
			)
		}
		if (!implementsConstraintErrorDefinition(catalog)) {
			return ApiErrorCatalogAnalysis.CodeLiteralKind.BadCatalog(
				highlightElement = catalogExpr,
				message = "${site.annotationName} catalog '${catalogName(catalog)}' must implement " + "${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}.",
			)
		}
		val names = ConstraintEnumLiteralSupport.constantNames(catalog)
		return if (code in names) {
			ApiErrorCatalogAnalysis.CodeLiteralKind.EnumValid(codeExpr, catalog)
		}
		else {
			val preview = names.take(MAX_NAMES_IN_MESSAGE)
				.joinToString(", ")
			val suffix = if (names.size > MAX_NAMES_IN_MESSAGE) ", …" else ""
			val catalogLabel = catalogName(catalog)
			ApiErrorCatalogAnalysis.CodeLiteralKind.EnumInvalid(
				codeExpr = codeExpr,
				enumClass = catalog,
				code = code,
				message = "${site.annotationName} code '$code' is not a constant of enum '$catalogLabel'. " + "Allowed: [$preview$suffix].",
			)
		}
	}
	
	/**
	 * Resolves the catalog type expression on [site] to a class PSI element.
	 *
	 * No side effects.
	 *
	 * @param site site whose `catalog` argument should be resolved
	 * @return enum / class PSI, or `null` when the expression is missing or unresolved	 
	 */
	fun resolveCatalog(site: ApiErrorCatalogCodeSite): PsiElement? {
		val catalogExpr = site.catalogExpression
			?: return null
		return resolveTypeExpression(catalogExpr)
	}
	
	/**
	 * Unquoted string content of [expr], or `null` when interpolated.
	 *
	 * No side effects.
	 *
	 * @param expr host string template
	 * @return concatenated entry text, or `null` when the template has interpolation	 
	 */
	fun rawString(expr: KtStringTemplateExpression): String? {
		if (expr.hasInterpolation()) return null
		return expr.entries.joinToString("") { it.text }
	}
	
	/**
	 * Resolves a Kotlin type / class-literal expression to a class PSI element.
	 *
	 * Prefers `X::class` via [mainReference], then same-file short name, then
	 * [PropertyRefAttributeDiscovery.findClassesByShortName]. Falls back to
	 * [resolveTypeReference] when no class literal is present. No side effects.
	 *
	 * @param expression `Foo::class` or type reference expression
	 * @return resolved [KtClass] / [PsiClass] (or other resolve target), or `null`	 
	 */
	private fun resolveTypeExpression(expression: KtExpression): PsiElement? {
		val literal = expression as? KtClassLiteralExpression
			?: expression.children.filterIsInstance<KtClassLiteralExpression>()
				.firstOrNull()
			?: return resolveTypeReference(expression)
		val receiver = literal.receiverExpression
			?: return null
		receiver.mainReference?.resolve()
			?.let { return it }
		val name = when (receiver) {
			is KtNameReferenceExpression -> receiver.getReferencedName()
			is KtDotQualifiedExpression -> (receiver.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
			else -> null
		}
			?: return null
		return literal.containingKtFile.declarations.filterIsInstance<KtClass>()
			.firstOrNull { it.name == name }
			?: PropertyRefAttributeDiscovery.pickBestClass(
				PropertyRefAttributeDiscovery.findClassesByShortName(
					literal.project,
					name,
					literal.resolveScope,
				),
				preferredPackage = literal.containingKtFile.packageFqName.asString()
					.takeIf { it.isNotEmpty() },
			)
	}
	
	/**
	 * Resolves a non-class-literal type reference (name or qualified name) to a class PSI.
	 *
	 * No side effects.
	 *
	 * @param expr name or dot-qualified type reference
	 * @return resolved [KtClassOrObject] / [PsiClass], or the raw resolve target when not a class	 
	 */
	private fun resolveTypeReference(expr: KtExpression): PsiElement? {
		val target = when (expr) {
			is KtNameReferenceExpression -> expr.mainReference.resolve()
			is KtDotQualifiedExpression -> {
				val selector = expr.selectorExpression as? KtNameReferenceExpression
				selector?.mainReference?.resolve()
			}
			
			else -> null
		}
		return when (target) {
			is KtClassOrObject -> target
			is PsiClass -> target
			else -> target
		}
	}
	
	/**
	 * Whether [catalog] is a Kotlin or Java enum class.
	 *
	 * No side effects.
	 *
	 * @param catalog resolved catalog type PSI
	 * @return `true` when [catalog] is an enum	 
	 */
	private fun isEnumClass(catalog: PsiElement): Boolean = when (catalog) {
		is KtClass -> catalog.isEnum()
		is PsiClass -> catalog.isEnum
		else -> false
	}
	
	/**
	 * Whether [catalog] implements `ConstraintErrorDefinition` via a supertype walk.
	 *
	 * Cycle-safe. Matches [OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION]. No side effects.
	 *
	 * @param catalog resolved enum catalog PSI
	 * @return `true` when the interface appears in the hierarchy	 
	 */
	private fun implementsConstraintErrorDefinition(catalog: PsiElement): Boolean {
		val target = OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION
		val visited = HashSet<PsiElement>()
		fun walk(type: PsiElement): Boolean {
			if (!visited.add(type)) return false
			val fq = when (type) {
				is KtClass -> type.fqName?.asString()
				is PsiClass -> type.qualifiedName
				else -> null
			}
			if (fq == target) return true
			when (type) {
				is KtClass -> {
					for (entry in type.superTypeListEntries) {
						val resolved = entry.typeAsUserType?.referenceExpression?.mainReference?.resolve()
							?: continue
						if (walk(resolved)) return true
					}
				}
				
				is PsiClass -> {
					for (superType in type.supers) {
						if (walk(superType)) return true
					}
				}
			}
			return false
		}
		return walk(catalog)
	}
	
	/**
	 * Display name for diagnostics (FQCN when available, else simple name).
	 *
	 * No side effects.
	 *
	 * @param catalog resolved catalog type PSI
	 * @return human-readable type label	 
	 */
	private fun catalogName(catalog: PsiElement): String = when (catalog) {
		is KtClass -> catalog.fqName?.asString()
			?: catalog.name
			?: "unknown"
		
		is PsiClass -> catalog.qualifiedName
			?: catalog.name
			?: "unknown"
		
		else -> catalog.toString()
	}
}
