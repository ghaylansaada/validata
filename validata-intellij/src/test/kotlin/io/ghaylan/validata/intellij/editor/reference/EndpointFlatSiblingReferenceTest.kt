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
import org.jetbrains.kotlin.psi.KtParameter
import org.junit.jupiter.api.DisplayName

/**
 * Flat sibling refs among handler method parameters (query / header / path).
 *
 * Parity: `docs/PROPERTY_REFERENCES.md` §4.3 — no nested object graph on flat params.
 * 
 * @author Ghaylan Saada
 */
class EndpointFlatSiblingReferenceTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("resolves flat sibling against handler method parameters")
	fun testResolveSiblingMethodParameter() {
		configureLookup()
		val refs = findStringLiteralContaining("tenant").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		val resolved = refs.single()
			.resolve() as? KtParameter
		assertNotNull(resolved)
		assertEquals("tenant", resolved!!.name)
	}
	
	@DisplayName("unresolved flat method parameter does not resolve")
	fun testUnresolvedMethodParameter() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"BadLookup.kt",
			"""
			package test.endpoint
			import io.ghaylan.validata.constraint.annotation.Compare
			class C {
			  fun lookup(
			    @Compare(ref = "tennt", operation = Compare.Operation.GT) q: String?,
			    tenant: String?,
			  ) = Unit
			}
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("tennt").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		assertNull(refs.single()
			.resolve())
	}
	
	@DisplayName("completion lists handler method parameter names")
	fun testCompletionListsMethodParameters() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"CompleteLookup.kt",
			"""
			package test.endpoint
			import io.ghaylan.validata.constraint.annotation.Compare
			class C {
			  fun lookup(
			    userId: String?,
			    @Compare(ref = "<caret>", operation = Compare.Operation.GT) q: String?,
			    tenant: String?,
			  ) = Unit
			}
			""".trimIndent(),
		)
		val variants = myFixture.completeBasic()
		assertNotNull(variants)
		val names = variants!!.map { it.lookupString }
			.toSet()
		assertTrue("expected tenant in $names", "tenant" in names)
		assertTrue("expected userId in $names", "userId" in names)
		assertTrue("expected q in $names", "q" in names)
	}
	
	@DisplayName("nested path on flat params does not resolve the second segment")
	fun testNestedPathOnFlatParamsDoesNotResolveSecondSegment() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"NestedFlat.kt",
			"""
			package test.endpoint
			import io.ghaylan.validata.constraint.annotation.Compare
			class C {
			  fun lookup(
			    @Compare(ref = "tenant.name", operation = Compare.Operation.GT) q: String?,
			    tenant: String?,
			  ) = Unit
			}
			""".trimIndent(),
		)
		val refs = findStringLiteralContaining("tenant.name").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(2, refs.size)
		assertEquals("tenant", (refs[0].resolve() as KtNamedDeclaration).name)
		assertNull("flat params must not step into nested paths", refs[1].resolve())
	}
	
	@DisplayName("resolves @RequiredWhen(ref = …) against handler method parameters")
	fun testResolveRequiredWhenPropertyOnMethodParameter() {
		addLibraryMarkers()
		myFixture.configureByFiles("RequiredWhenStub.kt")
		myFixture.configureByText(
			"RequiredWhenLookup.kt",
			"""
			package test.endpoint
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			class UserController {
			  fun lookup(
			    @RequiredWhen(ref = "tenant", condition = RequiredWhen.Condition.EQ, value = "55")
			    q: Int?,
			    tenant: Int,
			  ) = Unit
			}
			""".trimIndent(),
		)
		val refs = findExactStringLiteral("tenant").references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals(1, refs.size)
		val resolved = refs.single()
			.resolve() as? KtParameter
		assertNotNull(resolved)
		assertEquals("tenant", resolved!!.name)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"handler RequiredWhen.ref should use PROPERTY_REF blue, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any {
				it.forcedTextAttributesKey == io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors.PROPERTY_REF
			},
		)
	}
	
	private fun configureLookup() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"Lookup.kt",
			"""
			package test.endpoint
			import io.ghaylan.validata.constraint.annotation.Compare
			class UserController {
			  fun lookup(
			    userId: String?,
			    @Compare(ref = "tenant", operation = Compare.Operation.GT) q: String?,
			    tenant: String?,
			  ) = Unit
			}
			""".trimIndent(),
		)
	}
	
}
