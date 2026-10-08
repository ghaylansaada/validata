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

package io.ghaylan.validata.intellij.support

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Shared PSI helpers for IntelliJ platform tests (light fixtures).
 *
 * Keep assertions in the test class — this object only finds / walks PSI.*
 * 
 * @author Ghaylan Saada
 */
internal object PlatformTestSupport {
	
	/**
	 * First string template whose **exact** content (without quotes) equals [content].
	 */
	fun findExactStringLiteral(
		fixture: CodeInsightTestFixture,
		content: String,
	): KtStringTemplateExpression {
		val matches = collectStringLiterals(fixture).filter { literalContent(it) == content }
		check(matches.size == 1) {
			"expected exactly one string literal [$content], found ${matches.size} in ${fixture.file.name}"
		}
		return matches.single()
	}
	
	/**
	 * First string template whose content **contains** [fragment] (for soft/empty hosts).
	 */
	fun findStringLiteralContaining(
		fixture: CodeInsightTestFixture,
		fragment: String,
	): KtStringTemplateExpression {
		val matches = collectStringLiterals(fixture).filter { literalContent(it).contains(fragment) }
		check(matches.size == 1) {
			"expected exactly one string literal containing [$fragment], found ${matches.size} in ${fixture.file.name}"
		}
		return matches.single()
	}
	
	fun literalContent(literal: KtStringTemplateExpression): String = literal.entries.joinToString(separator = "") { it.text }
	
	private fun collectStringLiterals(fixture: CodeInsightTestFixture): List<KtStringTemplateExpression> {
		val found = mutableListOf<KtStringTemplateExpression>()
		fixture.file.accept(object: PsiRecursiveElementVisitor() {
			override fun visitElement(element: PsiElement) {
				if (element is KtStringTemplateExpression) {
					found += element
				}
				super.visitElement(element)
			}
		})
		return found
	}
}
