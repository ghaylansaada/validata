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

package io.ghaylan.validata.intellij.editor.annotator

import com.intellij.lang.annotation.HighlightSeverity
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Unresolved / self-reference highlighting for `@PropertyRef` string hosts.
 *
 * Type-mismatch highlights live in [TypeCompatibilityHighlightTest].
 * 
 * @author Ghaylan Saada
 */
class PropertyRefHighlightTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("unresolved property typo is highlighted")
	fun testUnresolvedTypoIsHighlighted() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"HighlightTypo.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class HighlightTypo(
			  val password: String,
			  @Compare(ref = "passw", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected unresolved property highlight, got: ${highlights.map { it.description }}",
			highlights.any { it.description?.contains("Cannot resolve property 'passw'") == true },
		)
		assertTrue(
			"unresolved name must use full red UNRESOLVED text, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any {
				it.description?.contains("Cannot resolve property 'passw'") == true && it.forcedTextAttributesKey == ConstraintHighlightingColors.UNRESOLVED
			},
		)
	}
	
	@DisplayName("resolved property-ref has no unresolved error highlight")
	fun testResolvedPathHasNoErrorHighlight() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"HighlightOk.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class HighlightOk(
			  val password: String,
			  @Compare(ref = "password", operation = Compare.Operation.EQ)
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"resolved path must not show unresolved errors: ${highlights.map { it.description }}",
			highlights.none { it.description?.contains("Cannot resolve property") == true },
		)
	}
	
	@DisplayName("self-reference property-ref is underlined, keeps blue")
	fun testSelfReferenceIsHighlighted() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"SelfRefRequest.kt",
			"""
			package test.flat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class SelfRefRequest(
			  val minAge: Int?,
			  @Compare(ref = "maxAge", operation = Compare.Operation.GT)
			  val maxAge: Int?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected self-reference highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("cannot reference the annotated property itself") == true
			},
		)
		assertTrue(
			"self-ref name exists — keep PROPERTY_REF blue, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.PROPERTY_REF },
		)
		assertFalse(
			"self-ref must not paint full red UNRESOLVED text",
			highlights.any {
				it.severity === HighlightSeverity.ERROR && it.description?.contains("cannot reference the annotated property itself") == true && it.forcedTextAttributesKey == ConstraintHighlightingColors.UNRESOLVED
			},
		)
	}
}
