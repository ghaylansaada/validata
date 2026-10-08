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
import io.ghaylan.validata.intellij.contract.PropertyRefLibraryFqns
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import io.ghaylan.validata.intellij.scope.PropertyRefClassMembers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Shared checks for `@Validatable` polymorphism metadata — IntelliJ twin of KSP
 * `PolymorphicSubtypeReconciler`.
 *
 * **What.** Walks a usage-site `@Validatable` annotation and produces [Finding]s for
 * discriminator / subtype authoring mistakes that the processor also rejects at compile time.
 *
 * Subtype binding / assignability helpers live in [ValidatableSubtypeBindings].
 *
 * **Not.** Not a Jackson `@JsonSubTypes` checker. Not a path / constraint-arg verifier.
 * Does not emit annotations itself — callers turn [Finding]s into `AnnotationHolder` entries.*
 * 
 * @author Ghaylan Saada
 */
internal object ValidatableAnnotationAnalysis {
	
	/**
	 * How `ValidatableAnnotator` paints a [Finding] range.
	 */
	enum class HighlightStyle {
		
		/** Apply unresolved-reference text attributes in addition to the error wave.		 */
		UNRESOLVED,
		
		/** Error severity + wave underline only — leave the underlying text color unchanged.		 */
		UNDERLINE,
	}
	
	/**
	 * One diagnostic produced by [findings] for the annotator to paint.
	 *
	 * @property message user-facing error text (wording kept close to KSP diagnostics)
	 * @property highlightElement PSI range host
	 * @property style whether to recolor as unresolved or only underline
	 */
	data class Finding(
		val message: String,
		val highlightElement: PsiElement,
		val style: HighlightStyle = HighlightStyle.UNRESOLVED,
	)
	
	/**
	 * Runs all `@Validatable` polymorphism checks for [annotation] when it sits on a Kotlin
	 * class / interface / object.	 
	 */
	fun findings(annotation: KtAnnotationEntry): List<Finding> {
		if (!isValidatable(annotation)) return emptyList()
		val owner = annotation.getStrictParentOfType<KtClass>()
			?: return emptyList()
		if (!owner.annotationEntries.contains(annotation)) return emptyList()
		val out = ArrayList<Finding>()
		checkDiscriminator(annotation, owner, out)
		checkSubtypeInheritance(annotation, owner, out)
		checkSubtypeNames(annotation, owner, out)
		return out
	}
	
	/**
	 * Whether [annotation] is `@Validatable`.
	 */
	fun isValidatable(annotation: KtAnnotationEntry): Boolean {
		val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
		val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
		if (fq == PropertyRefLibraryFqns.VALIDATABLE) return true
		return annotation.shortName?.asString() == "Validatable" && fq == null
	}
	
	/**
	 * Collects every `Subtype(name = "…")` string under [annotation]'s `subtypes` argument.
	 */
	fun subtypeNameLiterals(annotation: KtAnnotationEntry): List<Pair<KtExpression, String>> {
		val arg = namedArgument(annotation, "subtypes")
			?: return emptyList()
		val expr = arg.getArgumentExpression()
			?: return emptyList()
		return ValidatableSubtypeBindings.subtypeNameBindings(expr)
	}
	
	/**
	 * Declared + inherited Kotlin property names on [owner].
	 */
	internal fun allPropertyNames(owner: KtClassOrObject): Set<String> {
		val names = LinkedHashSet<String>()
		val visited = HashSet<PsiElement>()
		fun walk(type: KtClassOrObject) {
			if (!visited.add(type)) return
			PropertyRefClassMembers.listMembers(type)
				.mapNotNullTo(names) { it.name }
			for (entry in type.superTypeListEntries) {
				ValidatableSubtypeBindings.resolveSuperType(entry)
					?.let { walk(it) }
			}
		}
		walk(owner)
		return names
	}
	
	/**
	 * Whether [child] extends or implements [parent] via a Kotlin PSI super-type walk.
	 */
	internal fun extendsOrImplements(
		child: KtClassOrObject,
		parent: KtClass
	): Boolean = ValidatableSubtypeBindings.extendsOrImplements(child, parent)
	
	private fun checkDiscriminator(
		annotation: KtAnnotationEntry,
		owner: KtClass,
		out: MutableList<Finding>,
	) {
		val arg = namedArgument(annotation, "discriminator")
			?: return
		val expr = arg.getArgumentExpression() as? KtStringTemplateExpression
			?: return
		if (expr.hasInterpolation()) return
		val name = expr.entries.joinToString("") { it.text }
			.trim()
		if (name.isEmpty()) return
		
		if (name !in allPropertyNames(owner)) {
			out += Finding(
				message = "@Validatable on '${owner.name}' declares discriminator '$name', " + "but that type has no property named '$name'. " + "Add the property (or fix the discriminator spelling).",
				highlightElement = expr,
			)
			return
		}
		if (!ValidatableDiscriminatorSupport.isTypedLiteralScalar(annotation, owner)) {
			val typeFq = ValidatableDiscriminatorSupport.discriminatorTypeFq(annotation, owner)
				?: "unknown"
			out += Finding(
				message = "@Validatable on '${owner.name}' declares discriminator '$name' of type " + "'$typeFq', but only scalar types are allowed (String, number, temporal, or enum).",
				highlightElement = expr,
				style = HighlightStyle.UNDERLINE,
			)
		}
	}
	
	private fun checkSubtypeInheritance(
		annotation: KtAnnotationEntry,
		owner: KtClass,
		out: MutableList<Finding>,
	) {
		val arg = namedArgument(annotation, "subtypes")
			?: return
		val expr = arg.getArgumentExpression()
			?: return
		for ((typeExpr, resolved) in ValidatableSubtypeBindings.subtypeTypeBindings(expr)) {
			val child = resolved as? KtClassOrObject
				?: continue
			if (ValidatableSubtypeBindings.sameType(child, owner)) {
				out += Finding(
					message = "@Validatable.Subtype type must not be the same type as the " + "@Validatable parent '${owner.name}'.",
					highlightElement = typeExpr,
					style = HighlightStyle.UNDERLINE,
				)
				continue
			}
			if (!ValidatableSubtypeBindings.extendsOrImplements(child, owner)) {
				out += Finding(
					message = "@Validatable.Subtype type '${child.name}' must extend or implement " + "'${owner.name}' (the @Validatable parent).",
					highlightElement = typeExpr,
					style = HighlightStyle.UNDERLINE,
				)
			}
		}
	}
	
	private fun checkSubtypeNames(
		annotation: KtAnnotationEntry,
		owner: KtClass,
		out: MutableList<Finding>,
	) {
		if (ValidatableDiscriminatorSupport.discriminatorName(annotation) == null) return
		if (!ValidatableDiscriminatorSupport.isTypedLiteralScalar(annotation, owner)) return
		for ((nameExpr, raw) in subtypeNameLiterals(annotation)) {
			val err = ValidatableDiscriminatorSupport.subtypeNameError(annotation, owner, raw)
				?: continue
			out += Finding(
				message = "@Validatable.Subtype name $err",
				highlightElement = nameExpr,
			)
		}
	}
	
	private fun namedArgument(
		entry: KtAnnotationEntry,
		name: String
	): KtValueArgument? {
		entry.valueArguments.forEach { arg ->
			val ktArg = arg as? KtValueArgument
				?: return@forEach
			if (ktArg.getArgumentName()?.asName?.asString() == name) return ktArg
		}
		return null
	}
}
