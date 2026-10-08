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
 * IDE diagnostics for `@Validatable` discriminator property presence and subtype assignability.
 * 
 * @author Ghaylan Saada
 */
class ValidatableAnnotatorTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("missing discriminator property is highlighted as an error")
	fun testMissingDiscriminatorPropertyIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"MissingDisc.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			@Validatable(
			  discriminator = "kind",
			  subtypes = [Validatable.Subtype(name = "A", type = Alpha::class)],
			)
			interface Root

			@Validatable
			data class Alpha(val x: String?) : Root
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected discriminator error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("discriminator") == true && it.description?.contains("kind") == true
			},
		)
	}
	
	@DisplayName("subtype that does not extend the parent is highlighted as an error")
	fun testUnrelatedSubtypeIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Unrelated.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			@Validatable(
			  discriminator = "kind",
			  subtypes = [Validatable.Subtype(name = "X", type = Unrelated::class)],
			)
			interface Root {
			  val kind: String
			}

			@Validatable
			data class Unrelated(val kind: String, val x: String?)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected extend/implement error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("extend or implement") == true && it.description?.contains("Unrelated") == true
			},
		)
	}
	
	@DisplayName("valid discriminator + subtype hierarchy has no Validatable error")
	fun testValidPolymorphicRootHasNoError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Ok.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			enum class Kind { A, B }

			@Validatable(
			  discriminator = "kind",
			  subtypes = [Validatable.Subtype(name = "A", type = Alpha::class)],
			)
			interface Root {
			  val kind: Kind
			}

			@Validatable
			data class Alpha(override val kind: Kind, val x: String?) : Root
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected no Validatable polymorphism errors, got: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("discriminator") == true || it.description?.contains("extend or implement") == true || it.description?.contains(
					"Subtype name") == true || it.description?.contains("unknown enum") == true
			},
		)
	}
	
	@DisplayName("Subtype.name that is not an enum constant of the discriminator is an error")
	fun testInvalidEnumSubtypeNameIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"BadEnumName.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			enum class Kind { A, B }

			@Validatable(
			  discriminator = "kind",
			  subtypes = [Validatable.Subtype(name = "NOPE", type = Alpha::class)],
			)
			interface Root {
			  val kind: Kind
			}

			@Validatable
			data class Alpha(override val kind: Kind, val x: String?) : Root
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected enum subtype name error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("NOPE") == true || it.description?.contains("unknown enum") == true || it.description?.contains("not a constant") == true
			},
		)
	}
	
	@DisplayName("Subtype.name that is not a number for an Int discriminator is an error")
	fun testInvalidIntSubtypeNameIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"BadIntName.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			@Validatable(
			  discriminator = "code",
			  subtypes = [Validatable.Subtype(name = "abc", type = Alpha::class)],
			)
			interface Root {
			  val code: Int
			}

			@Validatable
			data class Alpha(override val code: Int, val x: String?) : Root
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected numeric subtype name error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("number") == true || it.description?.contains("abc") == true
			},
		)
	}
	
	@DisplayName("non-scalar discriminator type is highlighted as an error")
	fun testNonScalarDiscriminatorIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"NonScalarDisc.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			@Validatable(
			  discriminator = "tags",
			  subtypes = [Validatable.Subtype(name = "A", type = Alpha::class)],
			)
			interface Root {
			  val tags: List<String>
			}

			@Validatable
			data class Alpha(override val tags: List<String>, val x: String?) : Root
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected non-scalar discriminator error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("discriminator") == true && (it.description?.contains("scalar") == true || it.description?.contains("tags") == true)
			},
		)
	}
	
	@DisplayName("Subtype type equal to the Validatable parent is an error")
	fun testSelfSubtypeIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"SelfSubtype.kt",
			"""
			package test.validatable
			import io.ghaylan.validata.schema.Validatable

			@Validatable(
			  discriminator = "kind",
			  subtypes = [Validatable.Subtype(name = "ROOT", type = Root::class)],
			)
			interface Root {
			  val kind: String
			}
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected self-subtype error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("must not be the same type") == true || it.description?.contains("same type as the @Validatable parent") == true
			},
		)
	}
}
