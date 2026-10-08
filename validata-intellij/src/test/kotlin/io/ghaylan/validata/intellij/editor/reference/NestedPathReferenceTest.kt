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
import org.junit.jupiter.api.DisplayName

/**
 * Nested dotted `@PropertyRef` paths are rejected (same-object / element single-segment only).
 * 
 * @author Ghaylan Saada
 */
class NestedPathReferenceTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("nested dotted path is highlighted as an error")
	fun testNestedPathIsHighlightedAsError() {
		configureWithStub(
			"""
			package test.nested
			import io.ghaylan.validata.constraint.annotation.Compare
			data class Address(val city: String, val zip: String)
			data class User(
			  val address: Address,
			  @Compare(ref = "address.city", operation = Compare.Operation.EQ)
			  val homeCity: String,
			)
			""".trimIndent(),
		)
		val literal = findStringLiteralContaining("address.city")
		val refs = literal.references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(2, refs.size)
		assertNull("second segment must not resolve into nested type", refs[1].resolve())
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected nested-path error, got: ${highlights.map { it.description }}",
			highlights.any { it.description?.contains("Nested property references are not supported") == true },
		)
		assertTrue(
			"first segment exists — keep PROPERTY_REF blue, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any {
				it.forcedTextAttributesKey == io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors.PROPERTY_REF
			},
		)
		assertFalse(
			"nested-path policy must not paint the whole literal full red UNRESOLVED",
			highlights.any {
				it.description?.contains("Nested property references are not supported") == true && it.forcedTextAttributesKey == io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors.UNRESOLVED
			},
		)
	}
	
	@DisplayName("flat sibling still resolves when nested paths are rejected")
	fun testFlatSiblingStillResolves() {
		configureWithStub(
			"""
			package test.nested
			import io.ghaylan.validata.constraint.annotation.Compare
			data class User(
			  val password: String,
			  @Compare(ref = "password", operation = Compare.Operation.EQ)
			  val confirm: String,
			)
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("password").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertNotNull(refs.single()
			.resolve())
		val highlights = myFixture.doHighlighting()
		assertFalse(
			highlights.any { it.description?.contains("Nested property references") == true },
		)
	}
	
	private fun configureWithStub(text: String) {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText("Nested.kt", text)
	}
	
}
