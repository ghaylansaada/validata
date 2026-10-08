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

import com.intellij.psi.PsiNamedElement
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.support.PropertyRefLightFixtures
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Enum typed-literal strings: resolve / complete / navigate to enum entries.
 * 
 * @author Ghaylan Saada

 */
class ConstraintEnumLiteralReferenceTest : ValidataLightPlatformTestCase() {


	@DisplayName("resolves @In enum constant string to enum entry")
	fun testResolveInEnumConstant() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"EnumNav.kt",
			"""
			package test.enumlit
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN, USER }
			data class Box(
			  @field:In(values = ["ADMIN"])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val literal = findStringLiteralContaining("ADMIN")
		val refs = literal.references.filterIsInstance<ConstraintEnumLiteralPsiReference>()
		assertEquals(1, refs.size)
		val resolved = refs.single().resolve() as? PsiNamedElement
		assertNotNull("expected enum constant resolve, got ${refs.single().resolve()}", resolved)
		assertEquals("ADMIN", resolved!!.name)
	}

	@DisplayName("completion lists enum constants for typed-literal hosts")
	fun testCompletionListsEnumConstants() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"EnumComplete.kt",
			"""
			package test.enumlit
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN, USER }
			data class Box(
			  @field:In(values = ["<caret>"])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val variants = myFixture.completeBasic()
		assertNotNull(variants)
		val names = variants!!.map { it.lookupString }.toSet()
		assertTrue("expected ADMIN in $names", "ADMIN" in names)
		assertTrue("expected USER in $names", "USER" in names)
	}

	@DisplayName("enum completion filters by typed prefix")
	fun testCompletionFiltersEnumPrefix() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"EnumPrefix.kt",
			"""
			package test.enumlit
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN, USER, GUEST }
			data class Box(
			  @field:In(values = ["US<caret>"])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val variants = myFixture.completeBasic()
		if (variants == null) {
			// Unique match may auto-insert.
			assertTrue(
				"expected USER auto-inserted, file=${myFixture.file.text}",
				myFixture.file.text.contains("USER"),
			)
			assertFalse(myFixture.file.text.contains("\"ADMIN\""))
			assertFalse(myFixture.file.text.contains("\"GUEST\""))
		} else {
			val names = variants.map { it.lookupString }.toSet()
			assertTrue("expected USER in $names", "USER" in names)
			assertFalse("ADMIN must be filtered for prefix US, got $names", "ADMIN" in names)
			assertFalse("GUEST must be filtered for prefix US, got $names", "GUEST" in names)
		}
	}

	@DisplayName("@RequiredWhen value resolves against gate property enum type")
	fun testRequiredWhenValueResolvesGateEnum() {
		addLibraryMarkers()
		myFixture.configureByFiles("RequiredWhenStub.kt")
		myFixture.configureByText(
			"RequiredWhenEnum.kt",
			"""
			package test.enumlit
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			enum class ContactType { PHONE, EMAIL }
			data class Req(
			  val contactType: ContactType?,
			  @field:RequiredWhen(
			    ref = "contactType",
			    condition = RequiredWhen.Condition.EQ,
			    value = "PHONE")
			  val phoneBackup: String?,
			)
			""".trimIndent(),
		)
		val literal = findStringLiteralContaining("PHONE")
		val refs = literal.references.filterIsInstance<ConstraintEnumLiteralPsiReference>()
		assertEquals(1, refs.size)
		val resolved = refs.single().resolve() as? PsiNamedElement
		assertNotNull("expected ContactType.PHONE, got ${refs.single().resolve()}", resolved)
		assertEquals("PHONE", resolved!!.name)
	}

	@DisplayName("resolved property-ref uses PROPERTY_REF text attributes")
	fun testResolvedPropertyRefUsesPurpleAttributes() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"PurpleRef.kt",
			"""
			package test.enumlit
			import io.ghaylan.validata.constraint.annotation.Compare
			data class PurpleRef(
			  val password: String,
			  @Compare(ref = "password", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"resolved property ref should use PROPERTY_REF attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.PROPERTY_REF },
		)
	}

}
