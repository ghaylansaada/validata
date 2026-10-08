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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiNamedElement
import io.ghaylan.validata.intellij.analysis.compat.ConstraintSubjectTypeResolver
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass

/**
 * Resolves enum classes / constants for `@ConstraintArg(TYPED_LITERAL)` string hosts
 * (`@In(["ADMIN"])`, `@RequiredWhen(value = "PHONE")`, `@Min("MARCH")` on `Month`).
 *
 * **What.** Given an annotation use-site and a subject / gate type FQCN, finds the enum class
 * PSI (Kotlin or Java) and lists / looks up constant names for completion and verification.
 *
 * **Why.** Typed-literal args are **strings** in source (`"ADMIN"`) but must name real enum
 * constants (or `Month` names / ordinals). The IDE needs the same membership set KSP checks.
 *
 * **How it fits.** [subjectTypeFq] chooses gate type for `@RequiredWhen` `value`/`values`, else
 * the direct annotated subject. [findEnumClass] + [listConstants] / [findConstant] /
 * [constantNames] feed [ConstraintLiteralFormats.typedLiteralError] and completions.
 *
 * **Not.** Not a general type resolver. Does not validate numeric / temporal / string literals
 * — see [ConstraintLiteralFormats]. Does not parse annotation argument PSI beyond what callers
 * pass as [typeFq].
 *
 * **KSP / runtime parity.** Aligns with processor typed-literal enum membership: numeric and
 * string subjects are not enum hosts; non-`Month` temporals are not constant-name hosts;
 * `java.time.Month` is the special temporal enum. Short-name discovery goes through
 * [PropertyRefAttributeDiscovery.findClassesByShortName] within the annotation’s resolve scope.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintEnumLiteralSupport {

	/**
	 * Subject type FQCN for a typed-literal parameter (gate type for RequiredWhen value/values).
	 *
	 * For `@RequiredWhen`, parameters `value` and `values` are typed against the sibling named
	 * by `property` ([RequiredWhenLiteralSupport.gateTypeFqName]), not the annotated subject.
	 * Every other annotation / parameter uses [ConstraintSubjectTypeResolver.fqName].
	 *
	 * @param annotation constraint annotation use-site
	 * @param parameterName annotation parameter being typed (`"value"`, `"values"`, `"min"`, …)
	 * @return FQCN of the type that governs the literal, or `null` when the subject / gate
	 *   cannot be resolved
	 */
	fun subjectTypeFq(annotation: KtAnnotationEntry, parameterName: String): String? {
		if (RequiredWhenLiteralSupport.isRequiredWhen(annotation) &&
			parameterName in setOf("value", "values")
		) {
			return RequiredWhenLiteralSupport.gateTypeFqName(annotation)
		}
		return ConstraintSubjectTypeResolver.fqName(annotation)
	}

	/**
	 * Enum class PSI for [typeFq], or `null` when not an enum / unresolved.
	 * Includes temporal enums such as `java.time.Month`.
	 *
	 * Rejects numeric and string FQCNs immediately. Rejects temporal FQCNs other than
	 * `java.time.Month` (those use ISO parse rules, not constant names). Searches classes by
	 * short name in [annotation]'s project / resolve scope and matches FQCN (or Kotlin short
	 * name for local fixtures).
	 *
	 * @param annotation use-site providing project and resolve scope
	 * @param typeFq fully qualified subject / gate type, or `null`
	 * @return Kotlin [KtClass] or Java [PsiClass] enum, or `null` when [typeFq] is null, not an
	 *   enum host, or no matching enum class is found
	 */
	fun findEnumClass(annotation: KtAnnotationEntry, typeFq: String?): PsiElement? {
		if (typeFq == null) return null
		if (ConstraintLiteralFormats.isNumericType(typeFq) ||
			ConstraintLiteralFormats.isStringType(typeFq)
		) {
			return null
		}
		// Non-enum temporals (LocalDate, Duration, …) are not constant-name hosts.
		if (ConstraintLiteralFormats.isTemporalType(typeFq) && typeFq != "java.time.Month") {
			return null
		}
		val short = typeFq.substringAfterLast('.')
		val classes = PropertyRefAttributeDiscovery.findClassesByShortName(
			annotation.project,
			short,
			annotation.resolveScope,
		)
		for (cls in classes) {
			when (cls) {
				is KtClass -> {
					if (cls.isEnum() &&
						(cls.fqName?.asString() == typeFq || cls.name == short)
					) {
						return cls
					}
				}
				is PsiClass -> {
					if (cls.isEnum && cls.qualifiedName == typeFq) {
						return cls
					}
				}
			}
		}
		return null
	}

	/**
	 * All named enum constants of [enumClass] (Kotlin entries or Java [PsiEnumConstant]s).
	 *
	 * @param enumClass result of [findEnumClass] (or equivalent enum PSI)
	 * @return constant elements in declaration order when available; empty list when the
	 *   element is not a Kotlin / Java enum or has no body / fields
	 */
	fun listConstants(enumClass: PsiElement): List<PsiNamedElement> =
		when (enumClass) {
			is KtClass -> enumClass.body?.enumEntries.orEmpty()
			is PsiClass -> enumClass.fields.filterIsInstance<PsiEnumConstant>()
			else -> emptyList()
		}

	/**
	 * Looks up a single enum constant by exact name (after trim).
	 *
	 * @param enumClass enum PSI from [findEnumClass]
	 * @param name constant name as written in the string literal (whitespace trimmed)
	 * @return matching named element, or `null` when [name] is blank / empty after trim or no
	 *   constant has that exact name (case-sensitive)
	 */
	fun findConstant(enumClass: PsiElement, name: String): PsiNamedElement? {
		val trimmed = name.trim()
		if (trimmed.isEmpty()) return null
		return listConstants(enumClass).firstOrNull { it.name == trimmed }
	}

	/**
	 * Set of enum constant names for membership checks / diagnostics.
	 *
	 * @param enumClass enum PSI from [findEnumClass]
	 * @return set of non-null [PsiNamedElement.name] values; empty when [listConstants] is empty
	 */
	fun constantNames(enumClass: PsiElement): Set<String> =
		listConstants(enumClass).mapNotNull { it.name }.toSet()
}
