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

import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Autocomplete for `@PropertyRef` string hosts (lookup variants + typed prefix filter).
 * 
 * @author Ghaylan Saada
 */
class PropertyRefCompletionTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("completion includes sibling members for empty property-ref host")
	fun testCompletionIncludesSiblingMembers() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"CompleteRequest.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class CompleteRequest(
			  val password: String,
			  @Compare(ref = "<caret>", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val variants = myFixture.completeBasic()
		assertNotNull("expected completion variants", variants)
		val names = variants!!.map { it.lookupString }
			.toSet()
		assertTrue("expected password in $names", "password" in names)
		assertTrue("expected confirmation in $names", "confirmation" in names)
	}
	
	@DisplayName("completion filters sibling members by typed prefix")
	fun testCompletionFiltersByTypedPrefix() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"PrefixComplete.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class PrefixComplete(
			  val password: String,
			  val passcode: String,
			  val email: String,
			  @Compare(ref = "pass<caret>", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val variants = myFixture.completeBasic()
		assertNotNull(variants)
		val names = variants!!.map { it.lookupString }
			.toSet()
		assertTrue("expected password in $names", "password" in names)
		assertTrue("expected passcode in $names", "passcode" in names)
		assertFalse("email must be filtered out for prefix 'pass', got $names", "email" in names)
	}
}
