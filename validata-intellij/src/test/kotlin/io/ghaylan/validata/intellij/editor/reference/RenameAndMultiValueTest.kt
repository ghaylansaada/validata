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

import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.junit.jupiter.api.DisplayName

/**
 * Phase 5 platform tests: rename updates path segments; multi-value `by = […]` hosts.
 * 
 * @author Ghaylan Saada
 */
class RenameAndMultiValueTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("rename updates a flat property-ref segment")
	fun testRenameFlatSegment() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"RenameFlat.kt",
			"""
			package test.rename
			import io.ghaylan.validata.constraint.annotation.Compare
			data class RegisterRequest(
			  val password: String,
			  @Compare(ref = "password", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val passwordProp = findStringLiteralContaining("password").references.filterIsInstance<PropertyRefPsiReference>()
			.single()
			.resolve() as KtNamedDeclaration
		myFixture.renameElement(passwordProp, "secret")
		assertTrue(
			"rename should rewrite the annotation string",
			myFixture.file.text.contains("@Compare(ref = \"secret\", operation = Compare.Operation.EQ)"),
		)
		assertFalse(myFixture.file.text.contains("@Compare(ref = \"password\", operation = Compare.Operation.EQ)"))
	}
	
	@DisplayName("rename updates flat sibling property-ref")
	fun testRenameFlatSibling() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"RenameFlat.kt",
			"""
			package test.rename
			import io.ghaylan.validata.constraint.annotation.Compare
			data class User(
			  val password: String,
			  @Compare(ref = "password", operation = Compare.Operation.EQ)
			  val confirm: String,
			)
			""".trimIndent(),
		)
		val ref = findStringLiteralContaining("password").references.filterIsInstance<PropertyRefPsiReference>()
			.single()
		val prop = ref.resolve() as KtNamedDeclaration
		assertEquals("password", prop.name)
		myFixture.renameElement(prop, "secret")
		assertTrue(myFixture.file.text.contains("@Compare(ref = \"secret\", operation = Compare.Operation.EQ)"))
		assertFalse(myFixture.file.text.contains("@Compare(ref = \"password\", operation = Compare.Operation.EQ)"))
	}
	
	@DisplayName("each string in @Distinct(by=[…]) has its own reference")
	fun testMultiValueByEachStringHasReference() {
		addLibraryMarkers()
		myFixture.configureByFiles("DistinctStub.kt")
		myFixture.configureByText(
			"MultiBy.kt",
			"""
			package test.rename
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class UserDto(val email: String, val name: String)
			data class Batch(
			  val users: List<@Distinct(by = ["email", "name"]) UserDto>,
			)
			""".trimIndent(),
		)
		val emailRefs = findStringLiteralContaining("email").references.filterIsInstance<PropertyRefPsiReference>()
		val nameRefs = findStringLiteralContaining("name").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, emailRefs.size)
		assertEquals(1, nameRefs.size)
		assertEquals("email",
			(emailRefs.single()
				.resolve() as KtNamedDeclaration).name)
		assertEquals("name",
			(nameRefs.single()
				.resolve() as KtNamedDeclaration).name)
	}
	
}
