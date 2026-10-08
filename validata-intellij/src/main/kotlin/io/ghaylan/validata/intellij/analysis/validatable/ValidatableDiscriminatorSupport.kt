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
import io.ghaylan.validata.intellij.analysis.compat.PropertyRefScalarKinds
import io.ghaylan.validata.intellij.analysis.compat.SubjectTypeViews
import io.ghaylan.validata.intellij.analysis.literal.ConstraintEnumLiteralSupport
import io.ghaylan.validata.intellij.analysis.literal.ConstraintLiteralFormats
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.scope.PropertyRefClassMembers
import io.ghaylan.validata.schema.shape.ScalarKind
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Resolves `@Validatable(discriminator = "…")` to the named property and its type —
 * IntelliJ twin of processor discriminator property lookup.
 *
 * **What.** Reads the `discriminator` string from a `@Validatable` use-site, finds the
 * matching Kotlin property on the annotated type (including supers), exposes its FQCN / enum
 * host, and answers whether that type is a typed-literal **scalar** suitable for
 * `Subtype(name = …)` checks.
 *
 * **Why.** Polymorphism diagnostics and enum references need the discriminator’s type without
 * re-implementing property lookup in every caller. Keeping it here mirrors
 * `PolymorphicSubtypeReconciler.discriminatorPropertyType` / scalar gates in KSP.
 *
 * **How it fits.** Used by [ValidatableAnnotationAnalysis] (findings + scalar gate),
 * Validatable annotators (typed-literal coloring), and Validatable string reference providers
 * (enum Ctrl+Click / completion on `Subtype(name = …)`). Depends on
 * [PropertyRefClassMembers], [SubjectTypeViews], [ConstraintLiteralFormats],
 * [ConstraintEnumLiteralSupport], and [PropertyRefScalarKinds].
 *
 * **Not.** Not a full `@Validatable` checker — see [ValidatableAnnotationAnalysis]. Does not
 * read Jackson `@JsonTypeInfo` / `@JsonSubTypes`. Does not validate the name literal itself
 * beyond exposing [subtypeNameError] as a thin wrapper over [ConstraintLiteralFormats].
 *
 * **KSP / runtime parity.** Scalar host set must stay aligned with processor
 * `TypeClassification.isScalar` plus enum class kind; typed-literal phrases with
 * `ConstraintLiteralVerifier.typedLiteralError`.
 *
 * @author Ghaylan Saada
 */
internal object ValidatableDiscriminatorSupport {

	/**
	 * Discriminator property name from a non-blank, non-interpolated `discriminator` argument.
	 *
	 * @param annotation `@Validatable` use-site
	 * @return trimmed property name, or `null` when the argument is missing, blank, not a plain
	 *   string, or interpolated
	 */
	fun discriminatorName(annotation: KtAnnotationEntry): String? {
		val arg = namedArgument(annotation, "discriminator") ?: return null
		val expr = arg.getArgumentExpression() as? KtStringTemplateExpression ?: return null
		if (expr.hasInterpolation()) return null
		return expr.entries.joinToString("") { it.text }.trim().takeIf { it.isNotEmpty() }
	}

	/**
	 * Property declaration named by [discriminatorName], searching [owner] and its supers.
	 *
	 * Depth-first walk of [PropertyRefClassMembers] on each type, then resolved super-type
	 * list entries. Cycle-safe via a visited set.
	 *
	 * @param owner annotated `@Validatable` class / interface / object
	 * @param discriminatorName Kotlin property simple name (e.g. `"kind"`)
	 * @return matching member declaration, or `null` when absent / supers unresolved
	 */
	fun findDiscriminatorProperty(
		owner: KtClassOrObject,
		discriminatorName: String,
	): KtNamedDeclaration? {
		val visited = HashSet<PsiElement>()
		fun walk(type: KtClassOrObject): KtNamedDeclaration? {
			if (!visited.add(type)) return null
			PropertyRefClassMembers.findMember(type, discriminatorName)?.let { return it }
			for (entry in type.superTypeListEntries) {
				val superType = resolveSuperType(entry) ?: continue
				walk(superType)?.let { return it }
			}
			return null
		}
		return walk(owner)
	}

	/**
	 * Fully qualified type name of the discriminator property on [owner].
	 *
	 * Combines [discriminatorName] → [findDiscriminatorProperty] →
	 * [SubjectTypeViews.ofDeclaration]. Nullability is stripped by the type view; a `"*"`
	 * wildcard FQCN is treated as unresolved.
	 *
	 * @param annotation `@Validatable` use-site (supplies the discriminator string)
	 * @param owner annotated class
	 * @return type FQCN, or `null` when discriminator / property / type cannot be resolved
	 */
	fun discriminatorTypeFq(annotation: KtAnnotationEntry, owner: KtClass): String? {
		val name = discriminatorName(annotation) ?: return null
		val prop = findDiscriminatorProperty(owner, name) ?: return null
		return SubjectTypeViews.ofDeclaration(prop)?.qualifiedName?.takeUnless { it == "*" }
	}

	/**
	 * Whether the discriminator property is a typed-literal **scalar** host.
	 *
	 * Accepts string / number / temporal FQCNs ([ConstraintLiteralFormats]), enum classes
	 * ([ConstraintEnumLiteralSupport]), and remaining [PropertyRefScalarKinds] that are not
	 * [ScalarKind.OTHER] (e.g. `UUID`, `Boolean`, `Char`). Collections and nested object types return
	 * `false` so authors get a non-scalar discriminator error instead of silent name checks.
	 *
	 * @param annotation `@Validatable` use-site
	 * @param owner annotated class
	 * @return `true` when [discriminatorTypeFq] resolves to an allowed scalar host
	 */
	fun isTypedLiteralScalar(annotation: KtAnnotationEntry, owner: KtClass): Boolean {
		val typeFq = discriminatorTypeFq(annotation, owner) ?: return false
		if (ConstraintLiteralFormats.isStringType(typeFq)) return true
		if (ConstraintLiteralFormats.isNumericType(typeFq)) return true
		if (ConstraintLiteralFormats.isTemporalType(typeFq)) return true
		if (ConstraintEnumLiteralSupport.findEnumClass(annotation, typeFq) != null) return true
		// UUID / Boolean / Char via ScalarKind — still scalar for discriminator presence
		val kind = PropertyRefScalarKinds.scalarKind(typeFq)
		return kind != ScalarKind.OTHER
	}

	/**
	 * Typed-literal error phrase for a `Subtype(name = …)` string against the discriminator
	 * type.
	 *
	 * Delegates to [ConstraintLiteralFormats.typedLiteralError] with enum constant names when
	 * the discriminator is an enum. Same rules as KSP
	 * `ConstraintLiteralVerifier.typedLiteralError`.
	 *
	 * @param annotation `@Validatable` use-site
	 * @param owner annotated class
	 * @param raw subtype name literal text (no quotes)
	 * @return short error suffix for the diagnostic, or `null` when OK / discriminator type
	 *   unresolved (caller should not report a format error in that case)
	 */
	fun subtypeNameError(
		annotation: KtAnnotationEntry,
		owner: KtClass,
		raw: String,
	): String? {
		val typeFq = discriminatorTypeFq(annotation, owner) ?: return null
		val enumClass = ConstraintEnumLiteralSupport.findEnumClass(annotation, typeFq)
		val enumNames = enumClass?.let { ConstraintEnumLiteralSupport.constantNames(it) }
		return ConstraintLiteralFormats.typedLiteralError(raw, typeFq, enumNames)
	}

	/**
	 * Enum class PSI for the discriminator property when it is an enum host.
	 *
	 * Used by Validatable string reference providers to attach [ConstraintEnumLiteralPsiReference] on `Subtype(name = "CONST")`.
	 *
	 * @param annotation `@Validatable` use-site
	 * @param owner annotated class
	 * @return enum declaration PSI, or `null` when the discriminator is not an enum / unresolved
	 */
	fun discriminatorEnumClass(
		annotation: KtAnnotationEntry,
		owner: KtClass,
	): PsiElement? {
		val typeFq = discriminatorTypeFq(annotation, owner) ?: return null
		return ConstraintEnumLiteralSupport.findEnumClass(annotation, typeFq)
	}

	/**
	 * Resolves a Kotlin super-type list entry to its [KtClassOrObject] declaration.
	 *
	 * @param entry `A()`, `A`, or other [KtSuperTypeListEntry] form
	 * @return resolved class / interface / object, or `null` when unresolved
	 */
	private fun resolveSuperType(entry: KtSuperTypeListEntry): KtClassOrObject? {
		val userType = when (entry) {
			is KtSuperTypeCallEntry -> entry.typeAsUserType
			is KtSuperTypeEntry -> entry.typeAsUserType
			else -> entry.typeAsUserType
		} ?: return null
		return userType.referenceExpression?.mainReference?.resolve() as? KtClassOrObject
	}

	/**
	 * Named value argument on [entry], or `null` when absent.
	 *
	 * @param entry annotation whose arguments are searched
	 * @param name parameter name (typically `"discriminator"`)
	 * @return matching [KtValueArgument], or `null`
	 */
	private fun namedArgument(entry: KtAnnotationEntry, name: String): KtValueArgument? {
		entry.valueArguments.forEach { arg ->
			val ktArg = arg as? KtValueArgument ?: return@forEach
			if (ktArg.getArgumentName()?.asName?.asString() == name) return ktArg
		}
		return null
	}
}
