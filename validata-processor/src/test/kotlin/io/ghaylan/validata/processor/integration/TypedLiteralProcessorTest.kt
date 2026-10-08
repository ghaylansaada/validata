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
import io.ghaylan.validata.processor.verify.ConstraintLiteralVerifier
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * End-to-end [ConstraintLiteralVerifier.typedLiteralError] coverage via `@In` (TYPED_LITERAL).
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class TypedLiteralProcessorTest {
	
	@Test
	@DisplayName("@In numeric subject rejects non-number; accepts decimal")
	fun in_numberMatrix() {
		assertThat(
			KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.constraint.annotation.In
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class Ok(@field:In(values = ["18", "0.5"]) val age: Int?)
				""".trimIndent(),
			).exitCode,
		).isEqualTo(KotlinCompilation.ExitCode.OK)
		val bad = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.In
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Bad(@field:In(values = ["abc"]) val age: Int?)
			""".trimIndent(),
		)
		assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(bad.messages).contains("abc")
	}
	
	@Test
	@DisplayName("blank TYPED_LITERAL rejected for non-string gate via @RequiredWhen EQ")
	fun typedLiteral_blankNonStringRejected() {
		val bad = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Bad(
				val age: Int?,
				@field:RequiredWhen(
					ref = "age",
					condition = RequiredWhen.Condition.EQ,
					value = ""
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(bad.messages).containsIgnoringCase("non-blank")
	}
	
	@Test
	@DisplayName("blank TYPED_LITERAL allowed for String gate via @RequiredWhen EQ")
	fun typedLiteral_blankStringAllowed() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Ok(
				val label: String?,
				@field:RequiredWhen(
					ref = "label",
					condition = RequiredWhen.Condition.EQ,
					value = ""
				)
				val nickname: String?,
			)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("@In enum subject rejects unknown constant")
	fun in_enumMatrix() {
		val bad = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.In
			import io.ghaylan.validata.schema.Validatable

			enum class Color { RED, BLUE }

			@Validatable
			data class Bad(@field:In(values = ["GREEN"]) val color: Color?)
			""".trimIndent(),
		)
		assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(bad.messages).contains("GREEN")
		val ok = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.In
			import io.ghaylan.validata.schema.Validatable

			enum class Color { RED, BLUE }

			@Validatable
			data class Ok(@field:In(values = ["RED"]) val color: Color?)
			""".trimIndent(),
		)
		assertThat(ok.exitCode).withFailMessage { ok.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
	
	@Test
	@DisplayName("@In LocalDate rejects non-ISO; accepts LocalDate.parse form")
	fun in_temporalMatrix() {
		assertThat(
			KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.constraint.annotation.In
				import io.ghaylan.validata.schema.Validatable
				import java.time.LocalDate

				@Validatable
				data class Ok(@field:In(values = ["2020-01-01"]) val day: LocalDate?)
				""".trimIndent(),
			).exitCode,
		).isEqualTo(KotlinCompilation.ExitCode.OK)
		val bad = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.In
			import io.ghaylan.validata.schema.Validatable
			import java.time.LocalDate

			@Validatable
			data class Bad(@field:In(values = ["not-a-date"]) val day: LocalDate?)
			""".trimIndent(),
		)
		assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(bad.messages).contains("not-a-date")
	}
}
