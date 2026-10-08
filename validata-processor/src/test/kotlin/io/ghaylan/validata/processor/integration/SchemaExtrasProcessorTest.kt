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
 * Schema-builder edge cases: `@JsonIgnore`, non-scalar discriminator, element-ref happy path.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class SchemaExtrasProcessorTest {
	
	@Test
	@DisplayName("@JsonIgnore property is omitted from generated schema")
	fun jsonIgnoreProperty_omittedFromSchema() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import com.fasterxml.jackson.annotation.JsonIgnore
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class UserDto(
				val name: String?,
				@JsonIgnore
				val secret: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val src = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("UserDtoSchema") }
			.readText()
		assertThat(src).contains("\"name\"")
		assertThat(src).doesNotContain("\"secret\"")
	}
	
	@Test
	@DisplayName("non-scalar discriminator property fails KSP")
	fun discriminatorNonScalarType_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package poly

			import io.ghaylan.validata.schema.Validatable

			@Validatable(
				discriminator = "tags",
				subtypes = [
					Validatable.Subtype(name = "A", type = Alpha::class),
				],
			)
			interface Root {
				val tags: List<String>
			}

			@Validatable
			data class Alpha(override val tags: List<String>, val x: String?) : Root
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("scalar")
	}
	
	@Test
	@DisplayName("type-use @Distinct(by) on List element DTO compiles")
	fun distinctOnDtoList_validElementRef_compiles() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.Distinct
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Item(val email: String?)

			@Validatable
			data class Basket(
				val items: List<@Distinct(by = ["email"]) Item>?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("DistinctConstraint")
		assertThat(text).contains("email")
	}
}
