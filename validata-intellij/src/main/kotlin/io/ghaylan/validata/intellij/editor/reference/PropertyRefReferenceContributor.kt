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

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import io.ghaylan.validata.intellij.editor.completion.PropertyRefCompletionContributor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Registers Validata PSI reference providers for Kotlin string templates that may host
 * `@PropertyRef` paths or enum typed-literal constant names.
 *
 * ## What
 * Platform [PsiReferenceContributor] entry point. Binds
 * [PropertyRefReferenceProvider], [ConstraintEnumLiteralReferenceProvider],
 * [ValidatableStringReferenceProvider], and [ApiErrorCatalogReferenceProvider] to Kotlin
 * [KtStringTemplateExpression]s that sit inside a [KtAnnotationEntry]; each provider early-outs
 * when the string is not a matching Validata host.
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.psi.referenceContributor` with `language="kotlin"`:
 * ```
 * <psi.referenceContributor
 *     language="kotlin"
 *     implementation="…editor.reference.PropertyRefReferenceContributor"/>
 * ```
 * (EP bean [com.intellij.psi.impl.source.resolve.reference.PsiReferenceContributorEP]
 * binds the XML attribute `implementation`, not `implementationClass`.)
 * Without this EP, Go to Declaration / rename / reference-based variants never attach to
 * constraint string arguments.
 *
 * ## When it fires
 * Once at contributor registration time via [registerReferenceProviders]. Actual reference
 * construction happens later when the platform asks providers for a given string element.
 *
 * ## What it is NOT
 * - Not a resolver itself — it only wires providers.
 * - Not a completion contributor (see [PropertyRefCompletionContributor]).
 * - Does not filter hosts itself; providers perform discovery matching.*
 * 
 * @author Ghaylan Saada
 */
class PropertyRefReferenceContributor: PsiReferenceContributor() {
	
	/**
	 * Registers both Validata string-template reference providers.
	 *
	 * Validata: one pattern for all Kotlin string templates; [PropertyRefReferenceProvider]
	 * handles `@PropertyRef` paths, [ConstraintEnumLiteralReferenceProvider] handles
	 * `@ConstraintArg(TYPED_LITERAL)` enum names, [ValidatableStringReferenceProvider] handles
	 * `@Validatable` discriminator / subtype-name strings, and [ApiErrorCatalogReferenceProvider]
	 * handles `@ApiError(code = …)` enum catalogs. Each returns empty when unmatched.
	 *
	 * @param registrar platform registrar for language-scoped providers	 
	 */
	override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
		val stringInAnnotation = PlatformPatterns.psiElement(KtStringTemplateExpression::class.java)
			.inside(KtAnnotationEntry::class.java)
		registrar.registerReferenceProvider(stringInAnnotation, PropertyRefReferenceProvider())
		registrar.registerReferenceProvider(stringInAnnotation, ConstraintEnumLiteralReferenceProvider())
		registrar.registerReferenceProvider(stringInAnnotation, ValidatableStringReferenceProvider())
		registrar.registerReferenceProvider(stringInAnnotation, ApiErrorCatalogReferenceProvider())
	}
}
