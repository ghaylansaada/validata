/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ghaylan.validata.processor.integration

import com.tschuchort.compiletesting.KotlinCompilation
import io.ghaylan.validata.processor.support.KspCompileSupport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Compile-testing coverage for polymorphic `@Validatable(subtypes = …)` discovery
 * (no Jackson annotations involved), discriminator property presence, and subtype
 * assignability to the annotated parent.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class PolymorphicSubtypeProcessorTest {
	
	@Test
	@DisplayName("non-sealed interface with @Validatable.Subtype emits subtypes in the schema")
	fun nonSealedSubtypesAreEmitted() {
		val result = KspCompileSupport.compile(
			"""
			package poly

			import io.ghaylan.validata.schema.Validatable

			@Validatable(
				discriminator = "kind",
				subtypes = [
					Validatable.Subtype(name = "A", type = Alpha::class),
				],
			)
			interface Root {
				val kind: String
			}

			@Validatable
			data class Alpha(override val kind: String, val x: String?) : Root
			""".trimIndent(),
		)
		
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("Alpha::class.java")
		assertThat(text).contains("subtypes = mapOf(")
	}
	
	@Test
	@DisplayName("sealed subclass missing from @Validatable.subtypes is a compile error when subtypes are listed")
	fun sealedMismatchFails() {
		val result = KspCompileSupport.compile(
			"""
			package poly.mismatch

			import io.ghaylan.validata.schema.Validatable

			@Validatable(
				discriminator = "kind",
				subtypes = [
					Validatable.Subtype(name = "ONLY_A", type = OnlyA::class),
				],
			)
			sealed interface Root {
				val kind: String
			}

			@Validatable
			data class OnlyA(override val kind: String, val x: String?) : Root

			@Validatable
			data class MissingFromList(override val kind: String, val y: String?) : Root
			""".trimIndent(),
		)
		
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("MissingFromList")
	}
	
	@Test
	@DisplayName("@Validatable.Subtype pointing at a non-@Validatable type is a compile error")
	fun subtypeMustBeValidatable() {
		val result = KspCompileSupport.compile(
			"""
			package poly.noval

			import io.ghaylan.validata.schema.Validatable

			@Validatable(
				subtypes = [Validatable.Subtype(name = "X", type = Unmarked::class)],
			)
			interface Root

			data class Unmarked(val x: String?) : Root
			""".trimIndent(),
		)
		
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("Validatable")
	}
	
	@Test
	@DisplayName("non-blank discriminator that is not a property on the type is a compile error")
	fun discriminatorMustBeAProperty() {
		val result = KspCompileSupport.compile(
			"""
			package poly.disc

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
		
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("discriminator")
		assertThat(result.messages).contains("kind")
	}
	
	@Test
	@DisplayName("@Validatable.Subtype type that does not extend/implement the parent is a compile error")
	fun subtypeMustExtendOrImplementParent() {
		val result = KspCompileSupport.compile(
			"""
			package poly.unrelated

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
		
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("extend or implement")
		assertThat(result.messages).contains("Unrelated")
	}
	
	@Test
	@DisplayName("Subtype.name that is not an enum constant of the discriminator type fails")
	fun subtypeNameMustMatchEnumDiscriminator() {
		val result = KspCompileSupport.compile(
			"""
			package poly.enumdisc

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
		
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("NOPE")
		assertThat(result.messages).containsIgnoringCase("constant")
	}
	
	@Test
	@DisplayName("Subtype.name that is not a valid number for an Int discriminator fails")
	fun subtypeNameMustMatchIntDiscriminator() {
		val result = KspCompileSupport.compile(
			"""
			package poly.intdisc

			import io.ghaylan.validata.schema.Validatable

			@Validatable(
				discriminator = "code",
				subtypes = [Validatable.Subtype(name = "not-a-number", type = Alpha::class)],
			)
			interface Root {
				val code: Int
			}

			@Validatable
			data class Alpha(override val code: Int, val x: String?) : Root
			""".trimIndent(),
		)
		
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("not-a-number")
		assertThat(result.messages).containsIgnoringCase("number")
	}
	
	@Test
	@DisplayName("Subtype.name matching enum discriminator constant succeeds")
	fun subtypeNameMatchingEnumSucceeds() {
		val result = KspCompileSupport.compile(
			"""
			package poly.enumok

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
		
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
}
