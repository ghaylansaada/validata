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

package io.ghaylan.validata.intellij.analysis.literal

import io.ghaylan.validata.intellij.analysis.compat.SubjectTypeViews
import io.ghaylan.validata.intellij.contract.PropertyRefLibraryFqns
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import io.ghaylan.validata.intellij.scope.PropertyRefOwnerResolver
import org.jetbrains.kotlin.psi.*

/**
 * Gate-aware helpers for `@RequiredWhen` IDE literal checks (KSP `RequiredWhenArgSupport` parity).
 *
 * **What.** Detects `@RequiredWhen`, decides which of `value` / `values` are active for the
 * chosen `condition`, and resolves the **gate** sibling type named by `ref` for typed
 * literal validation.
 *
 * **Why.** `value` / `values` are typed against the sibling `ref` type — not the annotated
 * subject — and only when `condition` uses those arguments (`EQ` / `NE` / `GT` / `LT` / `GTE` / `LTE` vs `IN` /
 * `NIN`). Checking the wrong type or flagging inactive parameters is a false positive.
 *
 * **How it fits.** [ConstraintEnumLiteralSupport.subjectTypeFq] and literal inspections call
 * [shouldSkipParameter] / [gateTypeFqName]. Sibling lookup uses
 * [PropertyRefOwnerResolver.resolveSiblingScope] + [SubjectTypeViews.ofDeclaration].
 *
 * **Not.** Not a full `@RequiredWhen` semantic validator (existence of `ref`, condition
 * enum validity beyond name extraction, etc.). Does not handle non-string `ref` args or
 * interpolated string templates (those return `null` / skip).
 *
 * **KSP / runtime parity.** Same condition→parameter matrix as `RequiredWhenArgSupport`:
 * `value` only for `EQ` / `NE` / `GT` / `LT` / `GTE` / `LTE`; `values` only for `IN` / `NIN`. Gate type is
 * the named sibling’s declared type FQCN.*
 * 
 * @author Ghaylan Saada
 */
internal object RequiredWhenLiteralSupport {

	/**
	 * Conditions that consume the single `value` argument.
	 */
	private val VALUE_CONDITIONS = setOf("EQ", "NE", "GT", "LT", "GTE", "LTE")

	/**
	 * Conditions that consume the `values` collection argument.
	 */
	private val VALUES_CONDITIONS = setOf("IN", "NIN")

	/**
	 * Whether [annotation] is a `@RequiredWhen` use-site (FQCN, else short-name fallback).
	 *
	 * @param annotation any Kotlin annotation entry
	 * @return `true` when the annotation resolves to [PropertyRefLibraryFqns.REQUIRED_WHEN]
	 *   or (when unresolved) short name is `"RequiredWhen"`
	 */
	fun isRequiredWhen(annotation: KtAnnotationEntry): Boolean {
		if (annotation.shortName?.asString() != "RequiredWhen") return false
		val declaration = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
		val fq = declaration?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
		// Unresolved short name (light tests) still counts; a resolved alien FQCN does not.
		return fq == null || fq == PropertyRefLibraryFqns.REQUIRED_WHEN
	}

	/**
	 * Whether typed-literal / presence checks should ignore [parameterName] for this use-site.
	 *
	 * When `condition` cannot be read, returns `false` (do not skip — let other checks run).
	 * Unknown parameter names are never skipped.
	 *
	 * @param annotation expected to be `@RequiredWhen` (callers typically gate with
	 *   [isRequiredWhen] first)
	 * @param parameterName annotation parameter under inspection (`"value"`, `"values"`, …)
	 * @return `true` when the parameter is inactive for the current `condition`; `false` when
	 *   the parameter is active, [parameterName] is unrelated, or `condition` is unresolved
	 */
	fun shouldSkipParameter(annotation: KtAnnotationEntry, parameterName: String): Boolean {
		val condition = conditionName(annotation) ?: return false
		return when (parameterName) {
			"value" -> condition !in VALUE_CONDITIONS
			"values" -> condition !in VALUES_CONDITIONS
			else -> false
		}
	}

	/**
	 * FQCN of the gate sibling named by `ref`, or `null` when unresolved.
	 *
	 * Resolves sibling scope from the annotation owner, finds the member named by the
	 * non-blank `ref` string argument, then takes [SubjectTypeViews.ofDeclaration]'s
	 * qualified name (dropping `"*"`).
	 *
	 * @param annotation `@RequiredWhen` use-site
	 * @return gate type FQCN, or `null` when `ref` is missing / blank / interpolated,
	 *   sibling scope cannot be resolved, the named member is missing, or the member type
	 *   cannot be classified
	 */
	fun gateTypeFqName(annotation: KtAnnotationEntry): String? {
		val gateName = stringArg(annotation, "ref") ?: return null
		if (gateName.isBlank()) return null
		val scope = PropertyRefOwnerResolver.resolveSiblingScope(annotation) ?: return null
		val member = scope.findMember(gateName) ?: return null
		return SubjectTypeViews.ofDeclaration(member)?.qualifiedName?.takeUnless { it == "*" }
	}

	/**
	 * Extracts the `condition` enum constant simple name from a named argument.
	 *
	 * Accepts bare references (`EQ`), qualified forms (`Condition.EQ`), and falls back
	 * to text after the last `.`. Backticks are stripped.
	 *
	 * @param annotation annotation with a `condition` argument
	 * @return condition simple name, or `null` when the named argument / expression is missing
	 *   or blank after normalize
	 */
	private fun conditionName(annotation: KtAnnotationEntry): String? {
		val arg = namedArgument(annotation, "condition") ?: return null
		val expr = arg.getArgumentExpression() ?: return null
		val raw = when (expr) {
			is KtNameReferenceExpression -> expr.getReferencedName()
			is KtDotQualifiedExpression ->
				(expr.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
					?: expr.text.substringAfterLast('.')
			else -> expr.text.substringAfterLast('.')
		}
		return raw.trim().removeSurrounding("`").takeIf { it.isNotBlank() }
	}

	/**
	 * Constant string content of a named annotation argument (no interpolation).
	 *
	 * @param annotation annotation entry
	 * @param name argument name (`"ref"`, …)
	 * @return concatenated template entry text, or `null` when the argument is missing, not a
	 *   string template, or has interpolation
	 */
	private fun stringArg(annotation: KtAnnotationEntry, name: String): String? {
		val arg = namedArgument(annotation, name) ?: return null
		val expr = arg.getArgumentExpression() as? KtStringTemplateExpression ?: return null
		if (expr.hasInterpolation()) return null
		return expr.entries.joinToString("") { it.text }
	}

	/**
	 * Finds a named value argument on [annotation].
	 *
	 * @param annotation annotation entry
	 * @param name exact argument name
	 * @return matching [KtValueArgument], or `null` when absent (positional-only args are not
	 *   matched)
	 */
	private fun namedArgument(annotation: KtAnnotationEntry, name: String): KtValueArgument? =
		annotation.valueArguments.filterIsInstance<KtValueArgument>().firstOrNull {
			it.getArgumentName()?.asName?.asString() == name
		}
}
