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

@OptIn(ExperimentalCompilerApi::class)
class ContainsProcessorTest {

	@Test
	@DisplayName("@Contains on List peels ELEMENT TYPED_LITERAL against element type")
	fun contains_elementTypedLiteral() {
		val ok = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Contains
			import io.ghaylan.validata.schema.Validatable

			enum class Tier { A, B }

			@Validatable
			data class Ok(
				@field:Contains(values = ["A", "B"])
				val tiers: List<Tier>?,
			)
			""".trimIndent(),
		)
		assertThat(ok.exitCode).withFailMessage { ok.messages }.isEqualTo(KotlinCompilation.ExitCode.OK)

		val bad = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Contains
			import io.ghaylan.validata.schema.Validatable

			enum class Tier { A, B }

			@Validatable
			data class Bad(
				@field:Contains(values = ["Z"])
				val tiers: List<Tier>?,
			)
			""".trimIndent(),
		)
		assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(bad.messages).contains("Z")
	}

	@Test
	@DisplayName("@Contains on String fails validator subject typing")
	fun contains_rejectsStringSubject() {
		val bad = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Contains
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			data class Bad(
				@field:Contains(values = ["a"])
				val label: String?,
			)
			""".trimIndent(),
		)
		assertThat(bad.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(bad.messages).containsIgnoringCase("Contains")
	}
}
