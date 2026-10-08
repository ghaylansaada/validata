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

import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Weak warning when `@Constraint` usage cannot resolve `{Name}Constraint` metadata for PropertyRef DX.
 * 
 * @author Ghaylan Saada
 */
class ConstraintMetadataUnresolvedAnnotatorTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("missing {Name}Constraint metadata is a weak warning")
	fun testMissingConventionMetadataIsWeakWarning() {
		addLibraryMarkers()
		myFixture.configureByText(
			"BrokenMeta.kt",
			"""
			package test.meta
			import io.ghaylan.validata.constraint.Constraint

			@Constraint(validatedBy = [])
			annotation class BrokenRef(val property: String)

			data class Holder(
			  @BrokenRef("password")
			  val password: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected unresolved metadata weak warning, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("BrokenRef") == true && it.description?.contains("metadata") == true
			},
		)
	}
	
	@DisplayName("present convention metadata has no metadata warning")
	fun testPresentConventionMetadataHasNoMetadataWarning() {
		addLibraryMarkers()
		myFixture.configureByText(
			"OkMeta.kt",
			"""
			package test.meta
			import io.ghaylan.validata.constraint.Constraint

			@Constraint(validatedBy = [])
			annotation class Hint(val property: String)

			data class HintConstraint(val property: String)

			data class Holder(
			  @Hint("password")
			  val password: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"must not warn about metadata when HintConstraint exists: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("Cannot resolve generated metadata") == true
			},
		)
	}
}
