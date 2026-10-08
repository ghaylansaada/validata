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
 * Phase 4 platform tests: `@Distinct(by=…)` element-scope property references.
 *
 * @author Ghaylan Saada
 */
class ElementScopeReferenceTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("resolves @Distinct(by=…) against collection element type")
	fun testResolveElementEmail() {
		configureWithStubs(
			"""
			package test.element
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class UserDto(val email: String, val name: String)
			data class Batch(
			  val users: List<@Distinct(by = ["email"]) UserDto>,
			)
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("email").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertEquals("email",
			(refs.single()
				.resolve() as KtNamedDeclaration).name)
	}
	
	@DisplayName("unresolved element-scope name does not resolve")
	fun testUnresolvedElementName() {
		configureWithStubs(
			"""
			package test.element
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class UserDto(val email: String)
			data class Batch(
			  val users: List<@Distinct(by = ["emal"]) UserDto>,
			)
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("emal").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertNull(refs.single()
			.resolve())
	}
	
	@DisplayName("nested path in element scope is rejected")
	fun testNestedElementPathRejected() {
		configureWithStubs(
			"""
			package test.element
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class Address(val city: String)
			data class Order(val address: Address, val id: String)
			data class Batch(
			  val orders: List<@Distinct(by = ["address.city"]) Order>,
			)
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("address.city").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(2, refs.size)
		assertNull(refs[1].resolve())
		val highlights = myFixture.doHighlighting()
		assertTrue(
			highlights.any { it.description?.contains("Nested property references are not supported") == true },
		)
	}
	
	@DisplayName("element scope on non-collection subject does not resolve")
	fun testNonCollectionSubjectUnresolved() {
		configureWithStubs(
			"""
			package test.element
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class UserDto(val email: String)
			data class Batch(
			  val user: @Distinct(by = ["email"]) UserDto,
			)
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("email").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertNull(
			"non-collection subject must not resolve element paths",
			refs.single()
				.resolve(),
		)
	}
	
	@DisplayName("Set and nullable List element scopes are supported")
	fun testSetAndNullableListSupported() {
		configureWithStubs(
			"""
			package test.element
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class UserDto(val email: String)
			data class Batch(
			  val users: Set<@Distinct(by = ["email"]) UserDto>?,
			)
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("email").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertEquals("email",
			(refs.single()
				.resolve() as KtNamedDeclaration).name)
	}
	
	@DisplayName("type-use @Distinct on List element resolves against element type")
	fun testTypeUseDistinctOnListElement() {
		configureWithStubs(
			"""
			package test.element
			import io.ghaylan.validata.constraint.annotation.Distinct
			data class User(val id: String, val name: String)
			data class Batch(
			  val users: List<@Distinct(by = ["id", "aa"]) User>,
			)
			""".trimIndent(),
		)
		val idRefs = findStringLiteralContaining("id").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, idRefs.size)
		assertEquals("id",
			(idRefs.single()
				.resolve() as KtNamedDeclaration).name)
		val aaRefs = findStringLiteralContaining("aa").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, aaRefs.size)
		assertNull("aa is not a member of User",
			aaRefs.single()
				.resolve())
	}
	
	private fun configureWithStubs(source: String) {
		addLibraryMarkers()
		myFixture.configureByFiles("DistinctStub.kt")
		myFixture.configureByText("ElementRequest.kt", source)
	}
	
}
