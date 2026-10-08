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

package io.ghaylan.validata.intellij.analysis.validatable

import com.intellij.psi.PsiElement
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * PSI helpers for walking `@Validatable(subtypes = …)` bindings and subtype assignability.
 *
 * Extracted from [ValidatableAnnotationAnalysis] so check methods stay short and binding / resolve
 * logic lives in one place.*
 * 
 * @author Ghaylan Saada
 */
internal object ValidatableSubtypeBindings {
	
	/**
	 * Walks a `subtypes = […]` expression and pairs each `name = "…"` string with its text.
	 *
	 * @param expression value of the `subtypes` argument (usually a collection literal)
	 * @return `(name expression, raw text)` pairs; interpolated / non-string names omitted	 
	 */
	fun subtypeNameBindings(expression: KtExpression): List<Pair<KtExpression, String>> {
		val out = ArrayList<Pair<KtExpression, String>>()
		fun visit(node: PsiElement) {
			when (node) {
				is KtAnnotationEntry -> {
					nameArgumentExpression(node)?.let { nameExpr ->
						val text = stringContent(nameExpr)
							?: return@let
						out += nameExpr to text
					}
				}
				
				is KtCallExpression -> {
					if (looksLikeSubtypeCall(node)) {
						nameArgumentExpression(node)?.let { nameExpr ->
							val text = stringContent(nameExpr)
								?: return@let
							out += nameExpr to text
						}
					}
				}
			}
			node.children.forEach(::visit)
		}
		when (expression) {
			is KtCollectionLiteralExpression -> expression.innerExpressions.forEach(::visit)
			else -> visit(expression)
		}
		return out
	}
	
	/**
	 * Walks a `subtypes = […]` expression and pairs each `type = X::class` with its resolve target.
	 *
	 * @param expression value of the `subtypes` argument
	 * @return `(type expression, resolved PSI)` pairs	 
	 */
	fun subtypeTypeBindings(expression: KtExpression): List<Pair<KtExpression, PsiElement?>> {
		val out = ArrayList<Pair<KtExpression, PsiElement?>>()
		fun visit(node: PsiElement) {
			when (node) {
				is KtAnnotationEntry -> {
					typeArgumentExpression(node)?.let { typeExpr ->
						out += typeExpr to resolveTypeExpression(typeExpr)
					}
				}
				
				is KtCallExpression -> {
					if (looksLikeSubtypeCall(node)) {
						typeArgumentExpression(node)?.let { typeExpr ->
							out += typeExpr to resolveTypeExpression(typeExpr)
						}
					}
				}
			}
			node.children.forEach(::visit)
		}
		when (expression) {
			is KtCollectionLiteralExpression -> expression.innerExpressions.forEach(::visit)
			else -> visit(expression)
		}
		return out
	}
	
	/**
	 * Resolves a Kotlin super-type list entry to its [KtClassOrObject] declaration.
	 *
	 * @param entry `A()`, `A`, or other [KtSuperTypeListEntry] form
	 * @return resolved class / interface / object, or `null` when unresolved	 
	 */
	fun resolveSuperType(entry: KtSuperTypeListEntry): KtClassOrObject? {
		val userType = when (entry) {
			is KtSuperTypeCallEntry -> entry.typeAsUserType
			is KtSuperTypeEntry -> entry.typeAsUserType
			else -> entry.typeAsUserType
		}
			?: return null
		return userType.referenceExpression?.mainReference?.resolve() as? KtClassOrObject
	}
	
	/**
	 * Whether [child] and [parent] are the same type (identity or equal FQCN).
	 */
	fun sameType(
		child: KtClassOrObject,
		parent: KtClass
	): Boolean {
		if (child == parent) return true
		return child is KtClass && child.fqName != null && child.fqName == parent.fqName
	}
	
	/**
	 * Whether [child] extends or implements [parent] via a Kotlin PSI super-type walk.
	 */
	fun extendsOrImplements(
		child: KtClassOrObject,
		parent: KtClass
	): Boolean {
		val visited = HashSet<PsiElement>()
		fun walk(type: KtClassOrObject): Boolean {
			if (!visited.add(type)) return false
			if (sameType(type, parent)) return true
			for (entry in type.superTypeListEntries) {
				val superType = resolveSuperType(entry)
					?: continue
				if (walk(superType)) return true
			}
			return false
		}
		return walk(child)
	}
	
	private fun nameArgumentExpression(entry: KtAnnotationEntry): KtExpression? {
		val named = entry.valueArguments.filterIsInstance<KtValueArgument>()
			.firstOrNull { it.getArgumentName()?.asName?.asString() == "name" }
		val arg = named
			?: entry.valueArguments.getOrNull(0) as? KtValueArgument
		return arg?.getArgumentExpression()
	}
	
	private fun nameArgumentExpression(call: KtCallExpression): KtExpression? {
		val named = call.valueArguments.firstOrNull { it.getArgumentName()?.asName?.asString() == "name" }
		val arg = named
			?: call.valueArguments.getOrNull(0)
		return arg?.getArgumentExpression()
	}
	
	private fun typeArgumentExpression(entry: KtAnnotationEntry): KtExpression? {
		val named = entry.valueArguments.filterIsInstance<KtValueArgument>()
			.firstOrNull { it.getArgumentName()?.asName?.asString() == "type" }
		val arg = named
			?: entry.valueArguments.getOrNull(1) as? KtValueArgument
		return arg?.getArgumentExpression()
	}
	
	private fun typeArgumentExpression(call: KtCallExpression): KtExpression? {
		val named = call.valueArguments.firstOrNull { it.getArgumentName()?.asName?.asString() == "type" }
		val arg = named
			?: call.valueArguments.getOrNull(1)
		return arg?.getArgumentExpression()
	}
	
	private fun stringContent(expression: KtExpression): String? {
		val template = expression as? KtStringTemplateExpression
			?: return null
		if (template.hasInterpolation()) return null
		return template.entries.joinToString("") { it.text }
	}
	
	private fun looksLikeSubtypeCall(call: KtCallExpression): Boolean {
		val text = call.calleeExpression?.text
			?: return false
		return text == "Subtype" || text.endsWith(".Subtype") || text == "Validatable.Subtype"
	}
	
	private fun resolveTypeExpression(expression: KtExpression): PsiElement? {
		val literal = expression as? KtClassLiteralExpression
			?: expression.children.filterIsInstance<KtClassLiteralExpression>()
				.firstOrNull()
			?: return null
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
}
