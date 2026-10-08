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

package io.ghaylan.validata.intellij.editor.reference

import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import io.ghaylan.validata.intellij.analysis.literal.ConstraintEnumLiteralSupport
import io.ghaylan.validata.intellij.analysis.literal.RequiredWhenLiteralSupport
import io.ghaylan.validata.intellij.discovery.constraintarg.ConstraintArgAttributeDiscovery
import io.ghaylan.validata.intellij.model.ConstraintArgMetadataHost
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Attaches [ConstraintEnumLiteralPsiReference] on `@ConstraintArg(TYPED_LITERAL)` string
 * arguments whose subject type is an enum (including `java.time.Month`).
 *
 * ## What
 * Platform [PsiReferenceProvider] that discovers typed-literal metadata hosts on the enclosing
 * constraint annotation, resolves the subject enum class, and returns a single reference over
 * the string value (soft when empty, hard when non-empty).
 *
 * ## When it fires
 * For every Kotlin string template registered by [PropertyRefReferenceContributor]
 * (`psi.referenceContributor`). Returns empty unless the argument matches a `TYPED_LITERAL`
 * host at the correct [ConstraintArgTarget] (VALUE vs ELEMENT of a collection literal) and
 * the subject type resolves to an enum class.
 *
 * ## Completes / resolves
 * Resolve → enum constant PSI. Variants → all constants of that enum (used by completion and
 * reference-based lookup).
 *
 * ## What it is NOT
 * - Not a plugin.xml EP by itself — wired through [PropertyRefReferenceContributor].
 * - Not a format validator for numeric/temporal typed literals (annotator only).
 * - Not `@PropertyRef` path wiring — see [PropertyRefReferenceProvider].*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintEnumLiteralReferenceProvider: PsiReferenceProvider() {
	
	/**
	 * Builds an enum-constant reference when [element] is a typed-literal enum host.
	 *
	 * Validata: discovers `@ConstraintArg` hosts, picks VALUE vs ELEMENT target from whether
	 * the string sits inside a collection argument, resolves subject type FQCN → enum class,
	 * then attaches [ConstraintEnumLiteralPsiReference] over the value range.
	 *
	 * @param element candidate PSI (expected [KtStringTemplateExpression])
	 * @param context unused platform processing context
	 * @return one reference, or [PsiReference.EMPTY_ARRAY] when not an enum typed-literal host	 
	 */
	override fun getReferencesByElement(
		element: PsiElement,
		context: ProcessingContext,
	): Array<PsiReference> {
		val stringTemplate = element as? KtStringTemplateExpression
			?: return PsiReference.EMPTY_ARRAY
		if (stringTemplate.hasInterpolation()) return PsiReference.EMPTY_ARRAY
		val argument = stringTemplate.getStrictParentOfType<KtValueArgument>()
			?: return PsiReference.EMPTY_ARRAY
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>()
			?: return PsiReference.EMPTY_ARRAY
		val host = matchTypedLiteralHost(annotation, argument, stringTemplate)
			?: return PsiReference.EMPTY_ARRAY
		val typeFq = ConstraintEnumLiteralSupport.subjectTypeFq(annotation, host.parameterName)
		val enumClass = ConstraintEnumLiteralSupport.findEnumClass(annotation, typeFq)
			?: return PsiReference.EMPTY_ARRAY
		val raw = stringTemplate.entries.joinToString(separator = "") { it.text }
		val trimmed = raw.trim()
		if (raw.isNotEmpty() && trimmed.isEmpty()) return PsiReference.EMPTY_ARRAY
		val valueRange = ElementManipulators.getValueTextRange(stringTemplate)
		return arrayOf(
			ConstraintEnumLiteralPsiReference(
				element = stringTemplate,
				enumClass = enumClass,
				rangeInElement = valueRange,
				soft = trimmed.isEmpty(),
			),
		)
	}
	
	/**
	 * Finds the merged `@ConstraintArg(TYPED_LITERAL)` metadata host for this argument.
	 *
	 * Matches named arguments by parameter name; for the first positional argument prefers
	 * common parameter names (`value`, `values`, `factor`, `min`, `max`). Filters out
	 * `@RequiredWhen` parameters that [RequiredWhenLiteralSupport] says to skip. Distinguishes
	 * [ConstraintArgTarget.ELEMENT] when the string is inside the argument’s collection literal.
	 *
	 * @param annotation use-site constraint annotation
	 * @param argument value argument containing [stringTemplate]
	 * @param stringTemplate the string under consideration
	 * @return merged host, or `null` when no typed-literal host applies	 
	 */
	private fun matchTypedLiteralHost(
		annotation: KtAnnotationEntry,
		argument: KtValueArgument,
		stringTemplate: KtStringTemplateExpression,
	): ConstraintArgMetadataHost? {
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(annotation)
		if (hosts.isEmpty()) return null
		val name = argument.getArgumentName()?.asName?.asString()
		val matched = if (name != null) {
			hosts.filter { it.parameterName == name }
		}
		else {
			val index = annotation.valueArgumentList?.arguments?.indexOf(argument)
				?: return null
			if (index != 0) return null
			val preferred = hosts.filter {
				it.parameterName in setOf("value", "values", "factor", "min", "max")
			}
			preferred.ifEmpty { hosts.take(1) }
		}
		val afterRequiredWhen = if (RequiredWhenLiteralSupport.isRequiredWhen(annotation)) {
			matched.filterNot {
				RequiredWhenLiteralSupport.shouldSkipParameter(annotation, it.parameterName)
			}
		}
		else {
			matched
		}
		val argExpr = argument.getArgumentExpression()
		val target = if (argExpr is KtCollectionLiteralExpression && stringTemplate.getStrictParentOfType<KtCollectionLiteralExpression>() === argExpr) {
			ConstraintArgTarget.ELEMENT
		}
		else {
			ConstraintArgTarget.VALUE
		}
		val forTarget = afterRequiredWhen.filter {
			it.target == target && ConstraintArgKind.TYPED_LITERAL in it.kinds
		}
		if (forTarget.isEmpty()) return null
		return ConstraintArgMetadataHost(
			parameterName = forTarget.first().parameterName,
			kinds = forTarget.flatMap { it.kinds }
				.toSet(),
			target = target,
			message = forTarget.firstOrNull { it.message.isNotBlank() }?.message.orEmpty(),
		)
	}
}
