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
import io.ghaylan.validata.intellij.analysis.literal.RequiredWhenLiteralSupport
import io.ghaylan.validata.intellij.discovery.constraintarg.ConstraintArgAttributeDiscovery
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.model.ConstraintArgMetadataHost
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.math.BigDecimal

/**
 * Highlights constraint arguments using `@ConstraintArg` markers on metadata
 * (KSP `ConstraintArgVerifier` parity).
 *
 * ## What / highlights
 * - Strings: `NOT_BLANK`, `TYPED_LITERAL`, and string-form `POSITIVE` (e.g. `@MultipleOf`)
 * - Integer literals: `NON_NEGATIVE` / `POSITIVE` (e.g. `@Size(min = -1)`)
 * - Collection arguments: `NON_EMPTY` on [ConstraintArgTarget.VALUE]; element strings use
 *   [ConstraintArgTarget.ELEMENT] (e.g. `@In(values = [])` / `["NOPE"]`)
 * - Valid typed literals get number / temporal / enum colors ([ConstraintHighlightingColors]);
 *   invalid args use [ConstraintHighlightingColors.UNRESOLVED] (red text)
 * - Valid `REGEX` patterns are not recolored here — see `ConstraintRegexLanguageInjector`
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.annotator` with `language="kotlin"`:
 * ```
 * <annotator
 *     language="kotlin"
 *     implementationClass="…editor.annotator.ConstraintLiteralAnnotator"/>
 * ```
 *
 * ## When it fires
 * Platform visits PSI during highlighting. This annotator activates when [element] sits under
 * a [KtValueArgument] of a constraint annotation that discovery maps to one or more
 * [ConstraintArgMetadataHost]s. It then branches on whether [element] is a plain string,
 * the collection literal itself, or an integer constant / unary-signed constant.
 *
 * ## What it is NOT
 * - Not a `@PropertyRef` path annotator — see [PropertyRefAnnotator].
 * - Not a RegExp language injector — valid regex coloring is delegated to
 *   `ConstraintRegexLanguageInjector`; this class only errors on invalid pattern syntax.
 * - Not an enum PSI reference provider — navigation uses `ConstraintEnumLiteralReferenceProvider`.*
 * 
 * @author Ghaylan Saada
 */
class ConstraintLiteralAnnotator : Annotator {

	/**
	 * Validates / recolors constraint argument literals related to [element].
	 *
	 * Validata: resolves metadata hosts for the enclosing value argument, merges kinds for the
	 * VALUE vs ELEMENT target, then applies blank / typed-literal / positive / regex / numeric /
	 * non-empty checks with KSP-parity messages.
	 *
	 * @param element PSI under highlight (string, collection, or numeric expression leaf)
	 * @param holder annotation destination
	 */
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		when (element) {
			is KtStringTemplateExpression -> {
				if (element.hasInterpolation()) return
				annotateStringElement(element, holder)
			}
			is KtCollectionLiteralExpression -> annotateCollectionElement(element, holder)
			is KtConstantExpression, is KtPrefixExpression -> annotateNumericElement(element, holder)
			else -> return
		}
	}

	/**
	 * Validates / recolors a string constraint argument when [element] is a host literal.
	 *
	 * @param element string template under highlight
	 * @param holder annotation destination
	 */
	private fun annotateStringElement(element: KtStringTemplateExpression, holder: AnnotationHolder) {
		val argument = element.getStrictParentOfType<KtValueArgument>() ?: return
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return
		val matched = resolveParameterHosts(annotation, argument)
		if (matched.isEmpty()) return
		val expr = argument.getArgumentExpression() ?: return
		val target = if (isElementOfCollectionArg(element, expr)) {
			ConstraintArgTarget.ELEMENT
		} else {
			ConstraintArgTarget.VALUE
		}
		val host = mergeHosts(matched, target) ?: return
		val raw = element.entries.joinToString(separator = "") { it.text }
		val valueRange = ElementManipulators.getValueTextRange(element)
		val range = TextRange(
			element.textRange.startOffset + valueRange.startOffset,
			element.textRange.startOffset + valueRange.endOffset,
		)
		annotateString(host, annotation, raw, range, holder)
	}

	/**
	 * Validates a collection argument when [element] is the argument’s collection literal.
	 *
	 * @param element collection literal under highlight
	 * @param holder annotation destination
	 */
	private fun annotateCollectionElement(element: KtCollectionLiteralExpression, holder: AnnotationHolder) {
		val argument = element.getStrictParentOfType<KtValueArgument>() ?: return
		if (argument.getArgumentExpression() !== element) return
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return
		val matched = resolveParameterHosts(annotation, argument)
		if (matched.isEmpty()) return
		val host = mergeHosts(matched, ConstraintArgTarget.VALUE) ?: return
		annotateCollectionValue(host, annotation, element, holder)
	}

	/**
	 * Validates an integer argument when [element] is the argument expression leaf.
	 *
	 * @param element constant or unary-signed constant under highlight
	 * @param holder annotation destination
	 */
	private fun annotateNumericElement(element: PsiElement, holder: AnnotationHolder) {
		val argument = element.getStrictParentOfType<KtValueArgument>() ?: return
		val expr = argument.getArgumentExpression() ?: return
		if (element !== expr) return
		val value = parseLongLiteral(expr) ?: return
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return
		val matched = resolveParameterHosts(annotation, argument)
		if (matched.isEmpty()) return
		val host = mergeHosts(matched, ConstraintArgTarget.VALUE) ?: return
		annotateNumeric(host, annotation, value, expr.textRange, holder)
	}

	/**
	 * Discovers `@ConstraintArg` hosts for [argument] on [annotation].
	 *
	 * Named arguments match by parameter name; the first positional argument prefers common
	 * names (`value`, `factor`, `property`, `pattern`, `values`, `min`, `max`, `by`, `days`).
	 * `@RequiredWhen` parameters may be filtered out via [RequiredWhenLiteralSupport].
	 *
	 * @param annotation use-site constraint annotation
	 * @param argument value argument under inspection
	 * @return matching hosts (possibly multiple kinds / targets for the same parameter)
	 */
	private fun resolveParameterHosts(
		annotation: KtAnnotationEntry,
		argument: KtValueArgument,
	): List<ConstraintArgMetadataHost> {
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(annotation)
		if (hosts.isEmpty()) return emptyList()
		val name = argument.getArgumentName()?.asName?.asString()
		val matched = if (name != null) {
			hosts.filter { it.parameterName == name }
		} else {
			val index = annotation.valueArgumentList?.arguments?.indexOf(argument) ?: return emptyList()
			if (index != 0) return emptyList()
			val preferred = hosts.filter {
				it.parameterName in setOf("value", "factor", "property", "pattern", "values", "min", "max", "by", "days")
			}
			preferred.ifEmpty { hosts.take(1) }
		}
		if (RequiredWhenLiteralSupport.isRequiredWhen(annotation)) {
			return matched.filterNot {
				RequiredWhenLiteralSupport.shouldSkipParameter(annotation, it.parameterName)
			}
		}
		return matched
	}

	/**
	 * Merges hosts that share [target] into a single [ConstraintArgMetadataHost] with unioned
	 * kinds (and the first non-blank custom message).
	 *
	 * @param matched hosts already filtered to this parameter
	 * @param target VALUE vs ELEMENT
	 * @return merged host, or `null` when none apply to [target]
	 */
	private fun mergeHosts(
		matched: List<ConstraintArgMetadataHost>,
		target: ConstraintArgTarget,
	): ConstraintArgMetadataHost? {
		val forTarget = matched.filter { it.target == target }
		if (forTarget.isEmpty()) return null
		return ConstraintArgMetadataHost(
			parameterName = forTarget.first().parameterName,
			kinds = forTarget.flatMap { it.kinds }.toSet(),
			target = target,
			message = forTarget.firstOrNull { it.message.isNotBlank() }?.message.orEmpty(),
		)
	}

	/**
	 * True when [string] is an element of the collection literal that is the annotation argument
	 * (e.g. `"NOPE"` in `@In(values = ["NOPE"])`).
	 *
	 * @param string candidate string PSI
	 * @param argExpr the argument’s top-level expression
	 * @return `true` for ELEMENT-target string hosts
	 */
	private fun isElementOfCollectionArg(string: PsiElement, argExpr: PsiElement): Boolean {
		return argExpr is KtCollectionLiteralExpression && string.getStrictParentOfType<KtCollectionLiteralExpression>() === argExpr
	}

	/**
	 * Errors when [ConstraintArgKind.NON_EMPTY] applies and [collection] has no elements.
	 *
	 * @param host merged VALUE host
	 * @param annotation use-site annotation (for short name / message prefix)
	 * @param collection collection literal argument
	 * @param holder annotation destination
	 */
	private fun annotateCollectionValue(
		host: ConstraintArgMetadataHost,
		annotation: KtAnnotationEntry,
		collection: KtCollectionLiteralExpression,
		holder: AnnotationHolder,
	) {
		if (ConstraintArgKind.NON_EMPTY !in host.kinds) return
		if (collection.innerExpressions.isNotEmpty()) return
		val short = annotation.shortName?.asString() ?: "Constraint"
		val prefix = host.message.ifBlank { "@$short.${host.parameterName}" }
		holder.newAnnotation(HighlightSeverity.ERROR, "$prefix must not be empty")
			.range(collection.textRange)
			.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
			.create()
	}

	/**
	 * Validates a string argument against [host] kinds and recolors valid typed literals.
	 *
	 * Order: `NOT_BLANK` → `TYPED_LITERAL` (error or color) → string-form `POSITIVE` →
	 * `REGEX` syntax check (coloring left to `ConstraintRegexLanguageInjector`).
	 *
	 * @param host merged host for VALUE or ELEMENT
	 * @param annotation use-site annotation
	 * @param raw string contents (no quotes)
	 * @param range absolute range of the value text
	 * @param holder annotation destination
	 */
	private fun annotateString(
		host: ConstraintArgMetadataHost,
		annotation: KtAnnotationEntry,
		raw: String,
		range: TextRange,
		holder: AnnotationHolder,
	) {
		val short = annotation.shortName?.asString() ?: "Constraint"
		val prefix = host.message.ifBlank { "@$short.${host.parameterName}" }

		if (ConstraintArgKind.NOT_BLANK in host.kinds && raw.isBlank()) {
			holder.newAnnotation(HighlightSeverity.ERROR, "$prefix must not be blank")
				.range(range)
				.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
				.create()
			return
		}

		if (ConstraintArgKind.TYPED_LITERAL in host.kinds) {
			val typeFq = typedLiteralSubjectFq(annotation, host.parameterName)
			val enumNames = enumConstantNames(annotation, typeFq)
			val err = ConstraintLiteralFormats.typedLiteralError(raw, typeFq, enumNames)
			if (err != null) {
				holder.newAnnotation(HighlightSeverity.ERROR, "$prefix $err")
					.range(range)
					.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
					.create()
				return
			}
			colorTypedLiteral(raw, typeFq, enumNames, range, holder)
		}

		if (ConstraintArgKind.POSITIVE in host.kinds && raw.isNotBlank()) {
			val bd = try {
				BigDecimal(raw.trim().replace("_", ""))
			} catch (_: NumberFormatException) {
				null
			}
			if (bd == null || bd.signum() <= 0) {
				holder.newAnnotation(
					HighlightSeverity.ERROR,
					"$prefix must be > 0 (was '$raw')",
				)
					.range(range)
					.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
					.create()
				return
			}
			if (ConstraintArgKind.TYPED_LITERAL !in host.kinds) {
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range(range)
					.textAttributes(ConstraintHighlightingColors.NUMBER)
					.create()
			}
		}

		// Valid regex: leave coloring to ConstraintRegexLanguageInjector (HV-style multi-color).
		if (ConstraintArgKind.REGEX in host.kinds && raw.isNotBlank()) {
			try {
				java.util.regex.Pattern.compile(raw)
			} catch (ex: java.util.regex.PatternSyntaxException) {
				holder.newAnnotation(
					HighlightSeverity.ERROR,
					"$prefix is not a valid Java regex pattern: ${ex.description}",
				)
					.range(range)
					.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
					.create()
			}
		}
	}

	/**
	 * Recolors a valid typed string literal to match its subject kind (number / temporal / enum).
	 * Plain string subjects keep default string coloring.
	 *
	 * @param raw literal text
	 * @param typeFq subject type FQCN, if known
	 * @param enumNames constant names when the subject is an enum; `null` for non-enums
	 * @param range absolute value range
	 * @param holder annotation destination
	 */
	private fun colorTypedLiteral(
		raw: String,
		typeFq: String?,
		enumNames: Set<String>?,
		range: TextRange,
		holder: AnnotationHolder,
	) {
		if (typeFq == null || raw.isBlank()) return
		val key = when {
			enumNames != null || typeFq == "java.time.Month" -> ConstraintHighlightingColors.ENUM
			ConstraintLiteralFormats.isNumericType(typeFq) -> ConstraintHighlightingColors.NUMBER
			ConstraintLiteralFormats.isTemporalType(typeFq) -> ConstraintHighlightingColors.TEMPORAL
			ConstraintLiteralFormats.isStringType(typeFq) -> return
			else -> return
		}
		holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
			.range(range)
			.textAttributes(key)
			.create()
	}

	/**
	 * Validates integer annotation arguments against `NON_NEGATIVE` / `POSITIVE` kinds.
	 *
	 * Valid numeric bounds keep default number coloring (no silent recolor).
	 *
	 * @param host merged VALUE host
	 * @param annotation use-site annotation
	 * @param value parsed long value (supports unary minus / underscores via [parseLongLiteral])
	 * @param range expression text range
	 * @param holder annotation destination
	 */
	private fun annotateNumeric(
		host: ConstraintArgMetadataHost,
		annotation: KtAnnotationEntry,
		value: Long,
		range: TextRange,
		holder: AnnotationHolder,
	) {
		val short = annotation.shortName?.asString() ?: "Constraint"
		val prefix = host.message.ifBlank { "@$short.${host.parameterName}" }

		if (ConstraintArgKind.NON_NEGATIVE in host.kinds && value < 0) {
			holder.newAnnotation(
				HighlightSeverity.ERROR,
				"$prefix must be >= 0 (was $value)",
			)
				.range(range)
				.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
				.create()
			return
		}
		if (ConstraintArgKind.POSITIVE in host.kinds && value <= 0) {
			holder.newAnnotation(
				HighlightSeverity.ERROR,
				"$prefix must be > 0 (was $value)",
			)
				.range(range)
				.textAttributes(ConstraintHighlightingColors.UNRESOLVED)
				.create()
			return
		}
		// Valid numeric bounds keep default number coloring.
	}

	/**
	 * Subject type FQCN for typed-literal checks on [parameterName].
	 *
	 * @param annotation use-site constraint
	 * @param parameterName metadata parameter name
	 * @return FQCN, or `null` when the subject type cannot be resolved
	 */
	private fun typedLiteralSubjectFq(
		annotation: KtAnnotationEntry,
		parameterName: String,
	): String? = ConstraintEnumLiteralSupport.subjectTypeFq(annotation, parameterName)

	/**
	 * Parses a simple integer annotation argument (`40`, `-1`, `1_000`).
	 *
	 * @param expression argument expression
	 * @return parsed [Long], or `null` when not a supported integer form
	 */
	private fun parseLongLiteral(expression: PsiElement): Long? {
		val text = when (expression) {
			is KtConstantExpression -> expression.text
			is KtPrefixExpression -> {
				val op = expression.operationReference.getReferencedName()
				if (op != "-" && op != "+") return null
				val base = expression.baseExpression as? KtConstantExpression ?: return null
				op + base.text
			}
			else -> return null
		}
		val normalized = text.replace("_", "")
		return normalized.toLongOrNull()
	}

	/**
	 * Enum constant names for typed-literal validation / coloring when [typeFq] is an enum.
	 *
	 * Non-[Month] temporals return `null` (ISO / parse hosts). [Month] returns constant names so
	 * `"MARCH"` paints as [ConstraintHighlightingColors.ENUM]; validation still uses the temporal
	 * path in [ConstraintLiteralFormats.typedLiteralError] (checked before enum membership).
	 *
	 * @param annotation use-site constraint
	 * @param typeFq subject type FQCN
	 * @return constant name set, or `null` when not an enum catalog host
	 */
	private fun enumConstantNames(annotation: KtAnnotationEntry, typeFq: String?): Set<String>? {
		if (typeFq != null &&
			ConstraintLiteralFormats.isTemporalType(typeFq) &&
			typeFq != "java.time.Month"
		) {
			return null
		}
		val enumClass = ConstraintEnumLiteralSupport.findEnumClass(annotation, typeFq)
			?: return null
		return ConstraintEnumLiteralSupport.constantNames(enumClass)
	}
}
