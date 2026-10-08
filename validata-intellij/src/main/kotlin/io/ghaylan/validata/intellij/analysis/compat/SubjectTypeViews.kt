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
import io.ghaylan.validata.intellij.analysis.compat.SubjectTypeViews.BUILTIN_SHORT_NAMES
import io.ghaylan.validata.intellij.typing.ValidatorTypeView
import io.ghaylan.validata.schema.types.BuiltinTypeShortNames
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Builds [ValidatorTypeView]s for annotated subjects and for `ConstraintValidator<V, C>` type args.
 *
 * Type FQCNs, short-name fallbacks, and builtin assignable supers come from schema
 * ([TypeNames], [BuiltinTypeShortNames], [KnownTypes]) — this object only walks PSI.
 *
 * @author Ghaylan Saada
 */
internal object SubjectTypeViews {

	private val BUILTIN_SHORT_NAMES: Map<String, String> = BuiltinTypeShortNames.ALL

	fun ofDeclaration(declaration: KtNamedDeclaration): ValidatorTypeView? {
		val typeElement = when (declaration) {
			is KtProperty -> declaration.typeReference?.typeElement
			is KtParameter -> declaration.typeReference?.typeElement
			else -> null
		} ?: return null
		return ofTypeElement(typeElement)
	}

	fun ofTypeElement(typeElement: KtTypeElement): ValidatorTypeView? {
		val userType = unwrapNullable(typeElement) as? KtUserType ?: return null
		val fqName = fqNameOf(userType) ?: return null
		val args = userType.typeArgumentsAsTypes.mapNotNull { typeRef ->
			val elem = typeRef?.typeElement ?: return@mapNotNull ValidatorTypeView.WILDCARD
			ofTypeElement(elem) ?: ValidatorTypeView.WILDCARD
		}
		return typeView(fqName, args)
	}

	/**
	 * Builds a [ValidatorTypeView] for a known FQCN (validator `V` or subject), with builtin supers.
	 */
	fun typeView(
		qualifiedName: String,
		typeArguments: List<ValidatorTypeView> = emptyList(),
	): ValidatorTypeView {
		val q = KnownTypes.canonicalize(qualifiedName)
		if (KnownTypes.isArrayFqcn(q)) {
			val elem = typeArguments.firstOrNull()
				?: KnownTypes.primitiveArrayElementFqcn(q)?.let { typeView(it) }
			return ValidatorTypeView(
				qualifiedName = TypeNames.ARRAY_KOTLIN,
				isArray = true,
				arrayElement = elem,
				assignableSupertypes = KnownTypes.builtinSupertypes(q),
			)
		}
		return ValidatorTypeView(
			qualifiedName = q,
			typeArguments = typeArguments,
			assignableSupertypes = KnownTypes.builtinSupertypes(q),
		)
	}

	fun displayNames(types: List<ValidatorTypeView>): String =
		types.map { displayName(it) }.distinct().joinToString(", ")

	fun displayName(type: ValidatorTypeView): String {
		if (type.isWildcard) return "*"
		val simple = type.qualifiedName.substringAfterLast('.')
		if (type.typeArguments.isEmpty() && type.arrayElement == null) return simple
		if (type.isArray) {
			val elem = type.arrayElement?.let { displayName(it) } ?: "*"
			return "Array<$elem>"
		}
		if (type.typeArguments.isEmpty()) return simple
		val args = type.typeArguments.joinToString(", ") { displayName(it) }
		return "$simple<$args>"
	}

	private fun fqNameOf(userType: KtUserType): String? {
		when (val resolved = userType.referenceExpression?.mainReference?.resolve()) {
			is KtClassOrObject -> resolved.fqName?.asString()?.let { return it }
			is PsiClass -> resolved.qualifiedName?.let { return it }
		}
		val rendered = userType.text.trim().substringBefore('<').removeSuffix("?")
		if ('.' in rendered && rendered.all { it.isLetterOrDigit() || it == '.' || it == '_' }) {
			return rendered
		}
		val shortName = userType.referencedName ?: return null
		return BUILTIN_SHORT_NAMES[shortName]
	}

	private fun unwrapNullable(typeElement: KtTypeElement): KtTypeElement = when (typeElement) {
		is KtNullableType -> typeElement.innerType ?: typeElement
		else -> typeElement
	}
}
