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

import com.intellij.openapi.vfs.VirtualFile
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.junit.jupiter.api.DisplayName

/**
 * Property-ref path resolve: flat sibling, typo, blank skip, RequiredWhen ref host.
 * 
 * @author Ghaylan Saada
 */
class PropertyRefResolveTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("resolves flat sibling @Compare(ref = \"password\", operation = Compare.Operation.EQ)")
	fun testResolvePassword() {
		openRegisterRequest()
		val literal = findExactStringLiteral("password")
		val refs = literal.references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals("expected exactly one PropertyRefPsiReference", 1, refs.size)
		val resolved = refs.single()
			.resolve() as? KtNamedDeclaration
		assertNotNull(resolved)
		assertEquals("password", resolved!!.name)
	}
	
	@DisplayName("resolves @RequiredWhen(ref = \"…\") against sibling")
	fun testResolveRequiredWhenProperty() {
		addLibraryMarkers()
		myFixture.configureByFiles("RequiredWhenStub.kt")
		myFixture.configureByText(
			"RequiredWhenRequest.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			data class RequiredWhenRequest(
			  val email: String?,
			  @RequiredWhen(ref = "email", condition = RequiredWhen.Condition.MISSING)
			  val phone: String?,
			)
			""".trimIndent(),
		)
		val literal = findExactStringLiteral("email")
		val refs = literal.references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertEquals("email",
			(refs.single()
				.resolve() as? KtNamedDeclaration)?.name)
	}
	
	@DisplayName("unresolved typo does not resolve")
	fun testUnresolvedTypo() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"TypoRequest.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class TypoRequest(
			  val password: String,
			  @Compare(ref = "passw", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val refs = findExactStringLiteral("passw").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertNull(refs.single()
			.resolve())
	}
	
	@DisplayName("whitespace-only path attaches no PropertyRefPsiReference")
	fun testBlankHasNoReference() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"BlankRequest.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class BlankRequest(
			  val password: String,
			  @Compare(ref = "   ", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val refs = findExactStringLiteral("   ").references.filterIsInstance<PropertyRefPsiReference>()
		assertTrue(refs.isEmpty())
	}
	
	@DisplayName("dotted path does not step into nested object type")
	fun testDottedPathDoesNotStepIntoNestedType() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"NestedDeferred.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class Address(val city: String)
			data class NestedDeferred(
			  val address: Address,
			  @Compare(ref = "address.city", operation = Compare.Operation.EQ)
			  val other: String,
			)
			""".trimIndent(),
		)
		val refs = findExactStringLiteral("address.city").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(2, refs.size)
		assertNull(refs[1].resolve())
	}
	
	private fun openRegisterRequest() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt", "RegisterRequest.kt")
		val virtual: VirtualFile = myFixture.findFileInTempDir("RegisterRequest.kt")
			?: error("RegisterRequest.kt missing from temp dir")
		myFixture.openFileInEditor(virtual)
	}
}
