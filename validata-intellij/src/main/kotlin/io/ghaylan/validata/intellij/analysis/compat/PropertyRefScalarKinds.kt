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

package io.ghaylan.validata.intellij.analysis.compat

import com.intellij.psi.PsiClass
import io.ghaylan.validata.intellij.analysis.compat.PropertyRefScalarKinds.BUILTIN_SHORT_NAMES
import io.ghaylan.validata.intellij.analysis.compat.PropertyRefScalarKinds.fqNameOf
import io.ghaylan.validata.schema.ref.PropertyRefScalarCompatibility
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.types.BuiltinTypeShortNames
import io.ghaylan.validata.schema.types.ScalarKinds
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Maps Kotlin / Java leaf types to [ScalarKind] — delegates to schema [ScalarKinds]
 * (parity with KSP `TypeClassification.scalarKind`).
 *
 * **What.** Classifies a property / parameter’s declared type into the shared kind vocabulary
 * used by schema [PropertyRefScalarCompatibility]. Also exposes FQCN extraction for typed-literal / enum hosts.
 *
 * **Why.** Cross-field `@PropertyRef` inspections need the same coarse families as KSP.
 *
 * **How it fits.** [kindOf] feeds subject / referenced-leaf checks; [fqNameOfDeclaration] and
 * [scalarKind] support [ConstraintSubjectTypeResolver] and literal verifiers.
 *
 * **Not.** Not a full type shape (no iterable / map / object walk). Does not distinguish enum
 * leaves — those fall through to [ScalarKind.OTHER] here; enum membership for `TYPED_LITERAL` is
 * handled by `ConstraintEnumLiteralSupport`. Nullability is ignored.
 *
 * @author Ghaylan Saada
 */
internal object PropertyRefScalarKinds {

	/**
	 * Well-known short names → FQCN when PSI resolve is unavailable (light test fixtures).
	 *
	 * Smaller than [SubjectTypeViews]’s map: only leaf scalars / temporals needed for kind
	 * classification, not collection or `Any` / `Comparable` carriers.
	 */
	private val BUILTIN_SHORT_NAMES: Map<String, String> = BuiltinTypeShortNames.SCALAR_LEAVES

	/**
	 * Scalar kind of [declaration]'s declared type, or `null` when the type is missing /
	 * unrecognizable (caller should skip the compatibility check).
	 *
	 * @param declaration property or constructor / method parameter with a declared type
	 * @return [ScalarKind], or `null` when [declaration] has no usable type element or FQCN
	 */
	fun kindOf(declaration: KtNamedDeclaration): ScalarKind? {
		val typeElement = typeElementOf(declaration) ?: return null
		val fqName = fqNameOf(typeElement) ?: return null
		return scalarKind(fqName)
	}

	/**
	 * Fully qualified type name of [declaration], or `null` when unresolved.
	 *
	 * Nullability is stripped; type arguments are not included (leaf FQCN only).
	 *
	 * @param declaration property or parameter
	 * @return FQCN string, or `null` when the declaration is not a property / parameter, has no
	 *   type, or [fqNameOf] cannot classify the type element
	 */
	fun fqNameOfDeclaration(declaration: KtNamedDeclaration): String? {
		val typeElement = typeElementOf(declaration) ?: return null
		return fqNameOf(typeElement)
	}

	/**
	 * Maps a FQCN to a [ScalarKind] — delegates to schema [ScalarKinds].
	 *
	 * Always returns a kind; unknown leaves become [ScalarKind.OTHER] rather than `null` so
	 * callers can still run SAME_SCALAR_KIND subject gates (which reject OTHER).
	 *
	 * @param qName fully qualified Kotlin or Java type name (boxed or unboxed)
	 * @return matching [ScalarKind]
	 */
	fun scalarKind(qName: String): ScalarKind =
		ScalarKinds.of(qName)

	/**
	 * Declared type element of a property or parameter.
	 *
	 * @param declaration any named declaration
	 * @return type element, or `null` for unsupported declaration kinds / missing type refs
	 */
	private fun typeElementOf(declaration: KtNamedDeclaration): KtTypeElement? =
		when (declaration) {
			is KtProperty -> declaration.typeReference?.typeElement
			is KtParameter -> declaration.typeReference?.typeElement
			else -> null
		}

	/**
	 * Resolves [typeElement] to a FQCN via PSI, qualified text, or [BUILTIN_SHORT_NAMES].
	 *
	 * @param typeElement possibly nullable type element
	 * @return FQCN, or `null` when not a user type / unresolved and not a known short name
	 */
	private fun fqNameOf(typeElement: KtTypeElement): String? {
		val userType = unwrapNullable(typeElement) as? KtUserType ?: return null
		when (val resolved = userType.referenceExpression?.mainReference?.resolve()) {
			is KtClassOrObject -> resolved.fqName?.asString()?.let { return it }
			is PsiClass -> resolved.qualifiedName?.let { return it }
		}
		// Qualified reference text: java.math.BigInteger
		val rendered = userType.text.trim().removeSuffix("?")
		if ('.' in rendered && rendered.all { it.isLetterOrDigit() || it == '.' || it == '_' }) {
			return rendered
		}
		val shortName = userType.referencedName ?: return null
		return BUILTIN_SHORT_NAMES[shortName]
	}

	/**
	 * Strips a single layer of Kotlin nullability (`T?` → `T`).
	 *
	 * @param typeElement possibly nullable type element
	 * @return inner type for [KtNullableType], otherwise [typeElement] unchanged
	 */
	private fun unwrapNullable(typeElement: KtTypeElement): KtTypeElement =
		when (typeElement) {
			is KtNullableType -> typeElement.innerType ?: typeElement
			else -> typeElement
		}
}
