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
import io.ghaylan.validata.processor.support.fixtures.FixtureSnippets
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Schema generation mechanisms: IR emission, naming, cascade — not built-in constraint matrices.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class SchemaProcessorTest {
	
	@Nested
	@DisplayName("Happy path generation")
	inner class Generation {
		
		@Test
		@DisplayName("trivial @Validatable DTO generates an ObjectSchemaModule that compiles")
		fun trivialDtoGenerates() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class TrivialDto(
					val name: String?
				)
				""".trimIndent(),
			)
			
			assertThat(result.exitCode).withFailMessage { result.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			assertThat(KspCompileSupport.generatedSourceText(result)).contains("ObjectSchemaModule")
				.contains("TrivialDto")
		}
		
		@Test
		@DisplayName("@get:JsonProperty becomes the external name in generated PropertySpec")
		fun jsonPropertyExternalName() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import com.fasterxml.jackson.annotation.JsonProperty
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class NamedDto(
					@get:JsonProperty("full_name")
					val fullName: String?
				)
				""".trimIndent(),
			)
			
			assertThat(result.exitCode).withFailMessage { result.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			val src = KspCompileSupport.generatedSourceText(result)
			assertThat(src).contains("declaredName = \"fullName\"")
			assertThat(src).contains("externalName = \"full_name\"")
		}

		@Test
		@DisplayName("validata.jackson.naming=SNAKE_CASE sets externalName without @JsonProperty")
		fun snakeCaseExternalName() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class NamedDto(
					val firstName: String?,
					val minAge: Int?,
				)
				""".trimIndent(),
				kspOptions = mapOf("validata.jackson.naming" to "SNAKE_CASE"),
			)

			assertThat(result.exitCode).withFailMessage { result.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			val src = KspCompileSupport.generatedSourceText(result)
			assertThat(src).contains("declaredName = \"firstName\"")
			assertThat(src).contains("externalName = \"first_name\"")
			assertThat(src).contains("declaredName = \"minAge\"")
			assertThat(src).contains("externalName = \"min_age\"")
			assertThat(src).contains("object NamedDto_")
			assertThat(src).contains("const val FIRST_NAME: String = \"first_name\"")
			assertThat(src).contains("const val MIN_AGE: String = \"min_age\"")
		}

		@Test
		@DisplayName("@JsonProperty wins over validata.jackson.naming=SNAKE_CASE")
		fun jsonPropertyWinsOverSnakeCase() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import com.fasterxml.jackson.annotation.JsonProperty
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class NamedDto(
					@get:JsonProperty("full_name")
					val firstName: String?,
				)
				""".trimIndent(),
				kspOptions = mapOf("validata.jackson.naming" to "SNAKE_CASE"),
			)

			assertThat(result.exitCode).withFailMessage { result.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			val src = KspCompileSupport.generatedSourceText(result)
			assertThat(src).contains("externalName = \"full_name\"")
			assertThat(src).doesNotContain("externalName = \"first_name\"")
		}
		
		@Test
		@DisplayName("custom constraint metadata is emitted as a literal constructor call")
		fun customConstraintLiteral() {
			val result = KspCompileSupport.compile(
				"""
				package sample

				${FixtureSnippets.MECHANISM_CONSTRAINTS}

				@Validatable
				data class FlaggedDto(
					@field:TokenCheck(token = "abc")
					val name: String?
				)
				""".trimIndent(),
				providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
			)
			
			assertThat(result.exitCode).withFailMessage { result.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			val src = KspCompileSupport.generatedSourceText(result)
			assertThat(src).contains("TokenCheckConstraint(")
			assertThat(src).contains("token = \"abc\"")
		}
	}
	
	@Nested
	@DisplayName("Compile-time diagnostics")
	inner class Diagnostics {
		
		@Test
		@DisplayName("unknown sibling PropertyRef is a compile error")
		fun badSiblingReference() {
			val result = KspCompileSupport.compile(
				"""
				package sample

				${FixtureSnippets.MECHANISM_CONSTRAINTS}

				@Validatable
				data class PairDto(
					val password: String?,
					@field:MatchesSibling(property = "passwrod")
					val confirm: String?
				)
				""".trimIndent(),
				providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
			)
			
			assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
			assertThat(result.messages).contains("passwrod")
		}
		
		@Test
		@DisplayName("unmarked cascade target is a compile error")
		fun unmarkedCascade() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.schema.Validatable

				data class Nested(val city: String?)

				@Validatable
				data class Outer(
					val nested: Nested?
				)
				""".trimIndent(),
			)
			
			assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
			assertThat(result.messages).contains("not @Validatable")
			assertThat(result.messages).contains("@NoCascade")
		}
	}
	
	@Nested
	@DisplayName("Shape branches (array / map / list)")
	inner class ShapeBranches {
		
		@Test
		@DisplayName("List, Array, and Map properties emit iterable/map shape IR")
		fun collectionAndMapShapes() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class ContainersDto(
					val tags: List<String>?,
					val scores: IntArray?,
					val labels: Map<String, String>?,
				)
				""".trimIndent(),
			)
			
			assertThat(result.exitCode).withFailMessage { result.messages }
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			val src = KspCompileSupport.generatedSources(result)
				.first { it.name.contains("ContainersDtoSchema") }
				.readText()
			assertThat(src).contains("IterableShape")
			assertThat(src).contains("MapShape")
		}
	}
}
