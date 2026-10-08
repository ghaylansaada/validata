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

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import io.ghaylan.validata.processor.support.KspCompileSupport
import io.ghaylan.validata.processor.support.fixtures.FixtureSnippets
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Mechanism coverage for `@ConstraintArg` discovery — uses custom fixtures, not built-ins.
 *
 * Nested suites group assertions by [ConstraintArgKind].
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class ConstraintArgMechanismProcessorTest {
	
	private fun compileDto(body: String): JvmCompilationResult = KspCompileSupport.compile(
		"""
			package sample

			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class SampleDto(
				$body
			)
			""".trimIndent(),
		providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
	)
	
	@Nested
	@DisplayName("NOT_BLANK")
	inner class NotBlank {
		
		@Test
		@DisplayName("rejects blank token; default token compiles")
		fun notBlank() {
			assertThat(compileDto("""@field:TokenCheck val name: String?""").exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
			val bad = compileDto("""@field:TokenCheck(token = "") val name: String?""")
			assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		}
	}
	
	@Nested
	@DisplayName("NON_NEGATIVE / POSITIVE / TYPED_LITERAL")
	inner class BoundKinds {
		
		@Test
		@DisplayName("BoundCheck kinds")
		fun boundKinds() {
			assertThat(compileDto("""@field:BoundCheck(value = "18", min = 0, factor = 2) val age: Int?""").exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
			
			assertThat(compileDto("""@field:BoundCheck(min = -1) val age: Int?""").exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
			assertThat(compileDto("""@field:BoundCheck(factor = 0) val age: Int?""").exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
			assertThat(compileDto("""@field:BoundCheck(value = "abc") val age: Int?""").exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		}
	}
	
	@Nested
	@DisplayName("NON_EMPTY + ELEMENT")
	inner class ValuesKinds {
		
		@Test
		@DisplayName("NON_EMPTY on VALUE + NOT_BLANK/TYPED_LITERAL on ELEMENT")
		fun valuesKinds() {
			assertThat(compileDto("""@field:ValuesCheck(values = ["A"]) val role: String?""").exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
			
			assertThat(compileDto("""@field:ValuesCheck(values = []) val role: String?""").exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
			assertThat(compileDto("""@field:ValuesCheck(values = [""]) val role: String?""").exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		}
	}
	
	@Nested
	@DisplayName("REGEX")
	inner class RegexKind {
		
		@Test
		@DisplayName("rejects invalid patterns")
		fun regexKind() {
			assertThat(compileDto("""@field:PatternCheck(pattern = "[a-z]+") val name: String?""").exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
			assertThat(compileDto("""@field:PatternCheck(pattern = "(") val name: String?""").exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		}
	}
	
	@Nested
	@DisplayName("Type-use ELEMENT TYPED_LITERAL")
	inner class TypeUse {
		
		@Test
		@DisplayName("type-use ValuesCheck uses element type for TYPED_LITERAL")
		fun typeUseElementLiteral() {
			val ok = KspCompileSupport.compile(
				"""
				package sample

				${FixtureSnippets.MECHANISM_CONSTRAINTS}

				enum class Role { A, B }

				@Validatable
				data class SampleDto(
					val roles: List<@ValuesCheck(values = ["A"]) Role>,
				)
				""".trimIndent(),
				providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
			)
			assertThat(ok.exitCode).withFailMessage { ok.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			val bad = KspCompileSupport.compile(
				"""
				package sample

				${FixtureSnippets.MECHANISM_CONSTRAINTS}

				enum class Role { A, B }

				@Validatable
				data class SampleDto(
					val roles: List<@ValuesCheck(values = ["NOPE"]) Role>,
				)
				""".trimIndent(),
				providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
			)
			assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		}
	}
}
