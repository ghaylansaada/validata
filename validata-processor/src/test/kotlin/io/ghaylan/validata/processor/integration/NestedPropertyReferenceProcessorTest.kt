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
import org.junit.jupiter.api.Test

/**
 * Nested path / Fields / PropertyRef compatibility mechanisms using custom fixtures.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class NestedPropertyReferenceProcessorTest {
	
	@Test
	@DisplayName("nested PropertyRef path fails; flat sibling compiles")
	fun nestedPathRejectedFlatOk() {
		val nested = KspCompileSupport.compile(
			"""
			package sample

			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class Address(val city: String?)

			@Validatable
			data class User(
				val address: Address?,
				@field:MatchesSibling(property = "address.city")
				val homeCity: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(nested.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(nested.messages).containsIgnoringCase("nested")
		val flat = KspCompileSupport.compile(
			"""
			package sample

			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class User(
				val password: String?,
				@field:MatchesSibling(property = "password")
				val confirm: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(flat.exitCode).withFailMessage { flat.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("self-reference PropertyRef fails the build")
	fun selfReferenceRejected() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class Ages(
				val minAge: Int?,
				@field:AfterSibling(property = "maxAge")
				val maxAge: Int?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("cannot reference itself")
	}
	
	@Test
	@DisplayName("COMPARABLE_FAMILY mismatch fails; matching temporals compile")
	fun typeCompatibility() {
		val mismatch = KspCompileSupport.compile(
			"""
			package sample

			import java.time.Instant
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class Window(
				val startedAt: Instant?,
				@field:AfterSibling(property = "startedAt")
				val count: Int?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(mismatch.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(mismatch.messages).containsIgnoringCase("cannot compare")
		val ok = KspCompileSupport.compile(
			"""
			package sample

			import java.time.Instant
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class Window(
				val startedAt: Instant?,
				@field:AfterSibling(property = "startedAt")
				val endedAt: Instant?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(ok.exitCode).withFailMessage { ok.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("COMPARABLE_FAMILY subject gate accepts STRING but rejects BOOLEAN")
	fun comparableFamilySubjectGate() {
		val stringSubject = KspCompileSupport.compile(
			"""
			package sample

			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class Labels(
				val other: String?,
				@field:AfterSibling(property = "other")
				val name: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(stringSubject.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		
		val booleanSubject = KspCompileSupport.compile(
			"""
			package sample

			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class Flags(
				val other: Boolean?,
				@field:AfterSibling(property = "other")
				val enabled: Boolean?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(booleanSubject.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(booleanSubject.messages).containsIgnoringCase("cannot apply")
			.contains("BOOLEAN")
	}
	
	@Test
	@DisplayName("generated Fields object contains nested path constants")
	fun fieldsObjectGenerated() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Address(val city: String?)

			@Validatable
			data class UserRequest(
				val minAge: Int?,
				val address: Address?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val fields = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("UserRequest_") }
			.readText()
		assertThat(fields).contains("object UserRequest_")
		assertThat(fields).contains("const val MIN_AGE: String = \"minAge\"")
		assertThat(fields).contains("object Address")
		assertThat(fields).contains("const val CITY: String = \"address.city\"")
	}
	
	@Test
	@DisplayName("generated Fields constants are legal annotation arguments (compile-time const)")
	fun fieldsConstantsUsableInAnnotations() {
		val ksp = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Address(val city: String?)

			@Validatable
			data class UserRequest(
				val address: Address?,
				val homeCity: String?,
			)
			""".trimIndent(),
		)
		assertThat(ksp.exitCode).withFailMessage { ksp.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val fieldsFile = KspCompileSupport.generatedSources(ksp)
			.first { it.name.contains("UserRequest_") }
		val fieldsSource = fieldsFile.readText()
		// Fields KDoc links `[UserRequest]` / `[Address]` — recompile with those types present.
		val usage = KotlinCompilation().apply {
			inheritClassPath = true
			sources = listOf(
				com.tschuchort.compiletesting.SourceFile.kotlin(
					"Address.kt",
					"""
					package sample
					data class Address(val city: String?)
					""".trimIndent(),
				),
				com.tschuchort.compiletesting.SourceFile.kotlin(
					"UserRequest.kt",
					"""
					package sample
					data class UserRequest(
						val address: Address?,
						val homeCity: String?,
					)
					""".trimIndent(),
				),
				com.tschuchort.compiletesting.SourceFile.kotlin("UserRequest_.kt", fieldsSource),
				com.tschuchort.compiletesting.SourceFile.kotlin(
					"Usage.kt",
					"""
					package sample

					import sample.ghaylan.validata.UserRequest_

					annotation class PathHolder(val path: String)

					@PathHolder(UserRequest_.Address.CITY)
					class HoldsPath
					""".trimIndent(),
				),
			)
		}
			.compile()
		
		assertThat(usage.exitCode).withFailMessage { usage.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("cyclic object graphs still generate Fields without hanging")
	fun cyclicFieldsTerminate() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class NodeA(val b: NodeB?)

			@Validatable
			data class NodeB(val a: NodeA?)
			""".trimIndent(),
		)
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val fieldsA = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("NodeA_") }
			.readText()
		assertThat(fieldsA.lowercase()).containsAnyOf("cycle", "omitted")
	}
	
	@Test
	@DisplayName("JsonProperty wire name resolves for flat sibling PropertyRef")
	fun flatJsonPropertySibling() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import com.fasterxml.jackson.annotation.JsonProperty
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class User(
				@field:JsonProperty("pwd")
				val password: String?,
				@field:MatchesSibling(property = "pwd")
				val confirm: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
}
