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
 * `@RequiredWhen` compile-time gate typing and condition-based skip of `value` / `values`.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class RequiredWhenProcessorTest {
	
	@Test
	@DisplayName("EQ value typed against Int gate rejects non-numeric literal")
	fun requiredWhenGateTypeTypedLiteral_failsOnBadNumber() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.EQ,
					value = "abc",
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("abc")
	}
	
	@Test
	@DisplayName("EQ value typed against Int gate accepts numeric literal")
	fun requiredWhenGateTypeTypedLiteral_okOnNumber() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.EQ,
					value = "18",
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("MISSING skips unused value even when literal would fail gate typing")
	fun requiredWhenMissing_skipsValueValidation() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.MISSING,
					value = "abc",
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("EQ value typed against enum gate rejects unknown constant")
	fun requiredWhenGateTypeTypedLiteral_failsOnBadEnum() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			enum class ContactType { PHONE, EMAIL }

			@Validatable
			data class SampleDto(
				val contactType: ContactType?,
				@field:RequiredWhen(
					ref = "contactType",
					condition = RequiredWhen.Condition.EQ,
					value = "SMS",
				)
				val phone: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("SMS")
	}
	
	@Test
	@DisplayName("PRESENT skips unused value even when literal would fail gate typing")
	fun requiredWhenPresent_skipsValueValidation() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.PRESENT,
					value = "abc",
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("IN values typed against Int gate rejects non-numeric element")
	fun requiredWhenIn_failsOnBadNumberElement() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.IN,
					values = ["18", "abc"],
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("abc")
	}
	
	@Test
	@DisplayName("IN values typed against Int gate accepts numeric elements")
	fun requiredWhenIn_okOnNumberElements() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.IN,
					values = ["18", "21"],
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("NIN skips unused single value even when literal would fail gate typing")
	fun requiredWhenNotIn_skipsValueValidation() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class SampleDto(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.NIN,
					value = "abc",
					values = ["18"],
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
}
