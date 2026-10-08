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

package io.ghaylan.validata.intellij.discovery.constraintarg

import com.intellij.psi.PsiAnnotation
import io.ghaylan.validata.intellij.model.ConstraintArgMetadataHost
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.jetbrains.kotlin.psi.*

/**
 * Parses `@ConstraintArg` marker attributes into [ConstraintArgMetadataHost] fragments.
 *
 * Shared by [ConstraintArgAttributeDiscovery] for Kotlin PSI, Java PSI, and text-fallback
 * paths so kind/target/message decoding stays in one place.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintArgMarkerParser {
	
	/** Matches `@ConstraintArg(…)` / `ConstraintArg(…)` bodies for stub / nested-array text.	 */
	val CONSTRAINT_ARG_BODY: Regex = Regex("""@?ConstraintArg\s*\(([^)]*)\)""")
	
	/** Enum simple names accepted in `kinds` / free-form text.	 */
	val KIND_TOKEN: Regex = Regex("""\b(NOT_BLANK|NON_EMPTY|TYPED_LITERAL|NON_NEGATIVE|POSITIVE|REGEX)\b""")
	
	/**
	 * Builds one host from a single Kotlin `@ConstraintArg` entry.
	 *
	 * @param argAnn The `@ConstraintArg` annotation entry.
	 * @param parameterName Host parameter name.
	 * @return Host, or `null` when kinds cannot be parsed.	 
	 */
	fun parseKtOne(
		argAnn: KtAnnotationEntry,
		parameterName: String,
	): ConstraintArgMetadataHost? {
		val kinds = parseKtKinds(argAnn)
		if (kinds.isEmpty()) return null
		val target = parseKtTarget(argAnn)
		val message = stringLiteral(namedArgument(argAnn, "message")?.getArgumentExpression()).orEmpty()
		return ConstraintArgMetadataHost(parameterName, kinds, target, message)
	}
	
	/**
	 * Builds one host from a single PSI `@ConstraintArg` annotation.
	 *
	 * @param annotation PSI `@ConstraintArg`.
	 * @param parameterName Host parameter name.
	 * @return Host, or `null` when kinds cannot be parsed.	 
	 */
	fun parsePsiOne(
		annotation: PsiAnnotation,
		parameterName: String
	): ConstraintArgMetadataHost? {
		val kinds = parsePsiKinds(annotation)
		if (kinds.isEmpty()) return null
		val target = parsePsiTarget(annotation)
		val messageAttr = annotation.findAttributeValue("message")?.text?.trim()
			?.trim('"')
			.orEmpty()
		return ConstraintArgMetadataHost(parameterName, kinds, target, messageAttr)
	}
	
	/**
	 * Parses kind/target hosts from free-form `@ConstraintArg(…)` text (nested arrays / stubs).
	 *
	 * @param text Attribute or nested-array source fragment.
	 * @param parameterName Host parameter name.	 
	 */
	fun parseHostsFromText(
		text: String,
		parameterName: String
	): List<ConstraintArgMetadataHost> {
		val out = ArrayList<ConstraintArgMetadataHost>()
		CONSTRAINT_ARG_BODY.findAll(text)
			.forEach { match ->
				val body = match.groupValues[1]
				val kinds = KIND_TOKEN.findAll(body)
					.mapNotNull { toKind(it.value) }
					.toSet()
				if (kinds.isNotEmpty()) {
					val target = if (body.contains("ELEMENT")) {
						ConstraintArgTarget.ELEMENT
					}
					else {
						ConstraintArgTarget.VALUE
					}
					out += ConstraintArgMetadataHost(parameterName, kinds, target)
				}
			}
		return out
	}
	
	/**
	 * Reads `kinds` (or positional enum values) from a Kotlin `@ConstraintArg`.
	 *
	 * @param argAnn The `@ConstraintArg` entry.
	 * @return Set of recognized [ConstraintArgKind]s (may be empty).	 
	 */
	fun parseKtKinds(argAnn: KtAnnotationEntry): Set<ConstraintArgKind> {
		val names = mutableListOf<String>()
		val kindsArg = namedArgument(argAnn, "kinds")
		if (kindsArg != null) {
			val expr = kindsArg.getArgumentExpression()
			if (expr is KtCollectionLiteralExpression) {
				expr.innerExpressions.forEach { enumSimpleName(it)?.let { n -> names += n } }
			}
			else {
				enumSimpleName(expr)?.let { names += it }
			}
		}
		else {
			argAnn.valueArguments.forEach { arg ->
				val ktArg = arg as? KtValueArgument
					?: return@forEach
				val n = ktArg.getArgumentName()?.asName?.asString()
				if (n == "message" || n == "target") return@forEach
				if (n != null && n != "kinds" && n != "value") return@forEach
				enumSimpleName(ktArg.getArgumentExpression())?.let { names += it }
			}
		}
		return names.mapNotNull { toKind(it) }
			.toSet()
	}
	
	/**
	 * Reads `target` from a Kotlin `@ConstraintArg`.
	 *
	 * @param argAnn The `@ConstraintArg` entry.
	 * @return [ConstraintArgTarget.ELEMENT] when the enum is `ELEMENT`; otherwise `VALUE`.	 
	 */
	fun parseKtTarget(argAnn: KtAnnotationEntry): ConstraintArgTarget {
		val expr = namedArgument(argAnn, "target")?.getArgumentExpression()
			?: return ConstraintArgTarget.VALUE
		return when (enumSimpleName(expr)) {
			"ELEMENT" -> ConstraintArgTarget.ELEMENT
			else -> ConstraintArgTarget.VALUE
		}
	}
	
	/**
	 * Reads kind enum tokens from PSI `kinds` or `value` attribute text via regex.
	 *
	 * @param annotation PSI `@ConstraintArg`.
	 * @return Recognized kinds (may be empty when attributes are absent or unparseable).	 
	 */
	fun parsePsiKinds(annotation: PsiAnnotation): Set<ConstraintArgKind> {
		val value = annotation.findAttributeValue("kinds")
			?: annotation.findAttributeValue("value")
			?: return emptySet()
		val text = value.text
			?: return emptySet()
		return KIND_TOKEN.findAll(text)
			.mapNotNull { toKind(it.value) }
			.toSet()
	}
	
	/**
	 * Reads `target` from PSI attribute text.
	 *
	 * @param annotation PSI `@ConstraintArg`.
	 * @return [ConstraintArgTarget.ELEMENT] if the text contains `ELEMENT`; else `VALUE`.	 
	 */
	fun parsePsiTarget(annotation: PsiAnnotation): ConstraintArgTarget {
		val text = annotation.findAttributeValue("target")?.text.orEmpty()
		return if (text.contains("ELEMENT")) ConstraintArgTarget.ELEMENT else ConstraintArgTarget.VALUE
	}
	
	/**
	 * Maps an enum simple name to [ConstraintArgKind].
	 *
	 * @param name Token such as `NOT_BLANK` / `REGEX`.
	 * @return Matching kind, or `null` for unknown tokens.	 
	 */
	fun toKind(name: String): ConstraintArgKind? = when (name) {
		"NOT_BLANK" -> ConstraintArgKind.NOT_BLANK
		"NON_EMPTY" -> ConstraintArgKind.NON_EMPTY
		"TYPED_LITERAL" -> ConstraintArgKind.TYPED_LITERAL
		"NON_NEGATIVE" -> ConstraintArgKind.NON_NEGATIVE
		"POSITIVE" -> ConstraintArgKind.POSITIVE
		"REGEX" -> ConstraintArgKind.REGEX
		else -> null
	}
	
	/**
	 * Finds a named value argument on a Kotlin annotation entry.
	 *
	 * @param entry Annotation whose arguments are searched.
	 * @param name Argument name (`kinds`, `target`, `message`, …).
	 * @return Matching [KtValueArgument], or `null`.	 
	 */
	fun namedArgument(
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
	
	/**
	 * Extracts an enum entry’s simple name from a Kotlin expression.
	 *
	 * @param expression Argument expression, or `null`.
	 * @return Simple name, or `null` when absent / blank.	 
	 */
	fun enumSimpleName(expression: KtExpression?): String? {
		if (expression == null) return null
		return when (expression) {
			is KtNameReferenceExpression -> expression.getReferencedName()
			is KtDotQualifiedExpression -> (expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
				?: expression.text.substringAfterLast('.')
			
			else -> expression.text.substringAfterLast('.')
				.takeIf { it.isNotBlank() }
		}
	}
	
	/**
	 * Unwraps a Kotlin string literal expression to its contents.
	 *
	 * @param expression Expression expected to be a string literal.
	 * @return Unquoted contents, or `null` when not a simple quoted literal.	 
	 */
	fun stringLiteral(expression: KtExpression?): String? {
		val text = expression?.text
			?: return null
		if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) {
			return text.substring(1, text.length - 1)
		}
		return null
	}
}
