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

import com.intellij.util.ThreeState
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.junit.jupiter.api.DisplayName

/**
 * Autopopup must not be skipped inside `@PropertyRef` host strings (letter-by-letter completion).
 * 
 * @author Ghaylan Saada
 */
class PropertyRefCompletionConfidenceTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("autopopup confidence allows popup inside property-ref string")
	fun testAllowsAutopopupInsidePropertyRefString() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"AutopopupHost.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class AutopopupHost(
			  val username: String,
			  @Compare(ref = "usern<caret>", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val offset = myFixture.caretOffset
		val element = myFixture.file.findElementAt(offset - 1)!!
		val confidence = PropertyRefCompletionConfidence()
		assertEquals(
			"property-ref strings must allow autopopup",
			ThreeState.NO,
			confidence.shouldSkipAutopopup(myFixture.editor, element, myFixture.file, offset),
		)
		val stringTemplate = element as? KtStringTemplateExpression
			?: element.getStrictParentOfType()
		assertNotNull(stringTemplate)
		assertTrue(
			stringTemplate!!.references.any { it is PropertyRefPsiReference },
		)
	}
	
	@DisplayName("autopopup confidence is unsure outside property-ref string")
	fun testUnsureOutsidePropertyRefString() {
		myFixture.configureByText(
			"PlainString.kt",
			"""
			package test.flat
			val msg = "usern<caret>"
			""".trimIndent(),
		)
		val offset = myFixture.caretOffset
		val element = myFixture.file.findElementAt(offset - 1)!!
		val confidence = PropertyRefCompletionConfidence()
		assertEquals(
			ThreeState.UNSURE,
			confidence.shouldSkipAutopopup(myFixture.editor, element, myFixture.file, offset),
		)
	}
}
