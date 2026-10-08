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

package io.ghaylan.validata.intellij.editor.inject

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import io.ghaylan.validata.intellij.analysis.literal.RequiredWhenLiteralSupport
import io.ghaylan.validata.intellij.discovery.constraintarg.ConstraintArgAttributeDiscovery
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Injects IntelliJ’s RegExp language into string arguments marked `@ConstraintArg(REGEX)`
 * (e.g. `@Regex(pattern = "^[0-9]{5}$")`), matching Jakarta `@Pattern(regexp = …)` coloring.
 *
 * ## What
 * Platform [MultiHostInjector] that, for qualifying [KtStringTemplateExpression] hosts,
 * injects [RegExpLanguage] over the string’s value range so the editor applies regex
 * syntax highlighting, inspections, and fragment editing.
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.multiHostInjector`:
 * ```
 * <multiHostInjector
 *     implementation="…editor.inject.ConstraintRegexLanguageInjector"/>
 * ```
 *
 * ## When it fires
 * Platform calls [getLanguagesToInject] for elements returned by [elementsToInjectIn]
 * (Kotlin string templates). Injection proceeds only when [isRegexPatternHost] is true:
 * scalar VALUE argument with `REGEX` kind, no interpolation, not a collection element.
 *
 * ## What it is NOT
 * - Not a regex **validity** annotator — invalid patterns are errored by
 *   `ConstraintLiteralAnnotator`; valid ones are
 *   left uncolored by that annotator so this injector’s multi-color RegExp host wins.
 * - Not a typed-handler or completion helper — see [PropertyRefTypedHandler].
 * - Does not inject into interpolated templates (`"$x"`) or non-regex constraint strings.*
 * 
 * @author Ghaylan Saada
 */
class ConstraintRegexLanguageInjector : MultiHostInjector {

	/**
	 * Injects [RegExpLanguage] into [context] when it is a `@ConstraintArg(REGEX)` host.
	 *
	 * Validata: registers a single place covering the string value range (quotes excluded).
	 * No prefix/suffix wrappers — the pattern text is the fragment as authored.
	 *
	 * @param registrar injection registrar
	 * @param context candidate host element ([KtStringTemplateExpression] expected)
	 */
	override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
		val string = context as? KtStringTemplateExpression ?: return
		if (string.hasInterpolation()) return
		if (!isRegexPatternHost(string)) return

		val host = string as? PsiLanguageInjectionHost ?: return
		val inner = ElementManipulators.getValueTextRange(string)
		if (inner.isEmpty) return

		registrar
			.startInjecting(RegExpLanguage.INSTANCE)
			.addPlace(null, null, host, inner)
			.doneInjecting()
	}

	/**
	 * Declares which PSI element types this injector considers.
	 *
	 * Validata: only Kotlin string templates — constraint arguments are authored as strings.
	 *
	 * @return singleton list of [KtStringTemplateExpression]
	 */
	override fun elementsToInjectIn(): List<Class<out PsiElement>> =
		listOf(KtStringTemplateExpression::class.java)

	/**
	 * True when [string] is a VALUE (not collection-element) argument whose metadata host
	 * includes [ConstraintArgKind.REGEX].
	 *
	 * Matches named `pattern` (or first positional) hosts via
	 * [ConstraintArgAttributeDiscovery]; skips `@RequiredWhen` parameters that
	 * [RequiredWhenLiteralSupport] excludes. Rejects strings inside collection literals —
	 * regex patterns are scalar args (`pattern = "…"`), not `values = ["…"]` elements.
	 *
	 * @param string candidate string template
	 * @return `true` when RegExp injection should apply
	 */
	private fun isRegexPatternHost(string: KtStringTemplateExpression): Boolean {
		val argument = string.getStrictParentOfType<KtValueArgument>() ?: return false
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return false
		val argExpr = argument.getArgumentExpression() ?: return false
		// Regex patterns are scalar args (`pattern = "…"`), not `values = ["…"]` elements.
		if (argExpr is KtCollectionLiteralExpression) return false
		if (string.getStrictParentOfType<KtCollectionLiteralExpression>() != null) return false

		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(annotation)
		if (hosts.isEmpty()) return false

		val name = argument.getArgumentName()?.asName?.asString()
		val matched = if (name != null) {
			hosts.filter { it.parameterName == name }
		} else {
			val index = annotation.valueArgumentList?.arguments?.indexOf(argument) ?: return false
			if (index != 0) return false
			val preferred = hosts.filter { it.parameterName == "pattern" }
			preferred.ifEmpty { hosts.take(1) }
		}
		val forValue = matched.filter {
			it.target == ConstraintArgTarget.VALUE && ConstraintArgKind.REGEX in it.kinds
		}
		if (forValue.isEmpty()) return false
		if (RequiredWhenLiteralSupport.isRequiredWhen(annotation)) {
			return forValue.none {
				RequiredWhenLiteralSupport.shouldSkipParameter(annotation, it.parameterName)
			}
		}
		return true
	}
}
