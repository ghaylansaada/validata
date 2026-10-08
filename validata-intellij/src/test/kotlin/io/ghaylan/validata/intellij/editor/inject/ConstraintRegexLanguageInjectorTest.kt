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

import com.intellij.lang.injection.InjectedLanguageManager
import io.ghaylan.validata.intellij.support.PropertyRefLightFixtures
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.intellij.lang.regexp.RegExpLanguage
import org.junit.jupiter.api.DisplayName

/**
 * Ensures `@Regex(pattern = …)` gets RegExp language injection (HV `@Pattern(regexp)` parity).
 * 
 * @author Ghaylan Saada
 */
class ConstraintRegexLanguageInjectorTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("@Regex(pattern) injects RegExp language")
	fun testRegexPatternIsInjectedWithRegExpLanguage() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRegexConstraint(myFixture)
		myFixture.configureByText(
			"RegexInject.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Regex
			data class User(
			  @field:Regex(pattern = "^[A-Z0-9_]+${'$'}", name = "CODE")
			  val code: String?,
			)
			""".trimIndent(),
		)
		myFixture.doHighlighting()
		val patternStart = myFixture.file.text.indexOf("^[A-Z0-9_]+$")
		assertTrue("pattern literal missing", patternStart >= 0)
		val insidePattern = patternStart + 2
		val injected = InjectedLanguageManager.getInstance(project)
			.findInjectedElementAt(myFixture.file, insidePattern)
		assertNotNull("expected injected element inside @Regex(pattern)", injected)
		assertEquals(
			"expected RegExp language injection",
			RegExpLanguage.INSTANCE,
			injected!!.containingFile.language,
		)
	}
	
	@DisplayName("@Min value string is not injected as RegExp")
	fun testMinValueStringIsNotInjectedAsRegExp() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"NoRegexInject.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Min
			data class User(
			  @field:Min("2020-01-01")
			  val start: java.time.LocalDate?,
			)
			""".trimIndent(),
		)
		myFixture.doHighlighting()
		val start = myFixture.file.text.indexOf("2020-01-01")
		assertTrue(start >= 0)
		val injected = InjectedLanguageManager.getInstance(project)
			.findInjectedElementAt(myFixture.file, start + 2)
		assertTrue(
			"typed-literal strings must not get RegExp injection",
			injected == null || injected.containingFile.language != RegExpLanguage.INSTANCE,
		)
	}
}
