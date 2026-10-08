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

package io.ghaylan.validata.intellij.editor.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Registers [PropertyRefCompletionProvider] for basic completion inside Kotlin string
 * templates (attribute values discovered as `@PropertyRef` or enum typed-literal hosts).
 *
 * ## What
 * Platform [CompletionContributor] that extends basic completion for any PSI element inside
 * a [KtStringTemplateExpression]. The provider early-outs when no Validata references are
 * present on the host string.
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.completion.contributor` with `language="kotlin"`:
 * ```
 * <completion.contributor
 *     language="kotlin"
 *     implementationClass="…editor.completion.PropertyRefCompletionContributor"/>
 * ```
 *
 * ## When it fires
 * On basic completion invocation (Ctrl+Space / autopopup) when the caret is inside a Kotlin
 * string template. Actual items come from [PropertyRefCompletionProvider] reading attached
 * [PropertyRefPsiReference] / [ConstraintEnumLiteralPsiReference] variants.
 *
 * ## What it is NOT
 * - Not a confidence / autopopup policy class — see [PropertyRefCompletionConfidence] and
 *   `PropertyRefTypedHandler`.
 * - Not a reference contributor — references must already exist for completion to find them.
 * - Does not register smart / class-name completion types — only [CompletionType.BASIC].
 * 
 * @author Ghaylan Saada
 */
class PropertyRefCompletionContributor: CompletionContributor() {
	
	init {
		extend(CompletionType.BASIC,
			PlatformPatterns.psiElement()
				.inside(KtStringTemplateExpression::class.java),
			PropertyRefCompletionProvider())
	}
}
