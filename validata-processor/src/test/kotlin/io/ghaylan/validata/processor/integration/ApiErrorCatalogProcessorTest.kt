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
 * KSP catalog verification and IR emission for OpenAPI `@ApiError`.
 * 
 * @author Ghaylan Saada

 */
@OptIn(ExperimentalCompilerApi::class)
class ApiErrorCatalogProcessorTest {

	@Test
	@DisplayName("valid ConstraintErrorDefinition enum catalog emits SchemaErrorDoc into PropertySpec")
	fun validEnumCatalogEmitsErrorDocs() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
				EMAIL_TAKEN("Email already registered");
				override val code: String get() = name
			}

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(
					code = "EMAIL_TAKEN",
					catalog = UserErrors::class,
				)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode)
			.withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val generated = KspCompileSupport.generatedSourceText(result)
		assertThat(generated)
			.contains("SchemaErrorDoc(code = \"EMAIL_TAKEN\"")
			.contains("catalogFqcn = \"sample.UserErrors\"")
			.contains("errorDocs = listOf(")
		// Message may be baked via reflection when the class is loadable, or left blank for
		// OpenAPI to resolve — either way the catalog FQCN must be present.
	}

	@Test
	@DisplayName("annotation message override is baked into SchemaErrorDoc")
	fun annotationMessageOverrideIsBaked() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
				EMAIL_TAKEN("Email already registered");
				override val code: String get() = name
			}

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(
					code = "EMAIL_TAKEN",
					message = "Docs override",
					catalog = UserErrors::class,
				)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode)
			.withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val generated = KspCompileSupport.generatedSourceText(result)
		assertThat(generated)
			.contains("SchemaErrorDoc(code = \"EMAIL_TAKEN\"")
			.contains("message = \"Docs override\"")
	}

	@Test
	@DisplayName("invalid enum catalog code fails KSP")
	fun invalidEnumCatalogFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
				EMAIL_TAKEN("taken");
				override val code: String get() = name
			}

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(code = "NOT_A_MEMBER", catalog = UserErrors::class)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("NOT_A_MEMBER")
	}

	@Test
	@DisplayName("blank ApiError code fails KSP")
	fun blankCodeFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
				EMAIL_TAKEN("taken");
				override val code: String get() = name
			}

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(code = "", catalog = UserErrors::class)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("blank")
	}

	@Test
	@DisplayName("non-enum catalog class fails KSP")
	fun nonEnumCatalogFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			class NotAnEnum : ConstraintErrorDefinition {
				override val code: String get() = "X"
				override val message: String get() = "x"
			}

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(code = "X", catalog = NotAnEnum::class)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("enum")
	}

	@Test
	@DisplayName("enum without ConstraintErrorDefinition fails KSP")
	fun enumMissingInterfaceFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			enum class PlainErrors { EMAIL_TAKEN }

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(code = "EMAIL_TAKEN", catalog = PlainErrors::class)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("ConstraintErrorDefinition")
	}

	@Test
	@DisplayName("stacked ApiError markers all emit into PropertySpec")
	fun stackedApiErrorsEmit() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError
			import io.ghaylan.validata.schema.Validatable

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
				A("first"),
				B("second");
				override val code: String get() = name
			}

			@Validatable
			data class CreateUserRequest(
				@field:ApiError(code = "A", catalog = UserErrors::class)
				@field:ApiError(code = "B", catalog = UserErrors::class)
				val email: String?,
			)
			""".trimIndent(),
		)

		assertThat(result.exitCode)
			.withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val generated = KspCompileSupport.generatedSourceText(result)
		assertThat(generated)
			.contains("SchemaErrorDoc(code = \"A\"")
			.contains("SchemaErrorDoc(code = \"B\"")
	}
}
