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
import io.ghaylan.validata.processor.ProcessorOptions
import io.ghaylan.validata.processor.SchemaProcessor
import io.ghaylan.validata.processor.support.KspCompileSupport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Cascade / cross-module object-ref emission contracts for [SchemaProcessor].
 * 
 * @author Ghaylan Saada

 */
@OptIn(ExperimentalCompilerApi::class)
class CascadePolicyProcessorTest {

	@Nested
	inner class SameCompilation {

		@Test
		@DisplayName("unmarked cascade target in the same compilation unit is a compile error")
		fun unmarkedCascadeErrors() {
			val result = KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.schema.Validatable

				data class Nested(val city: String?)

				@Validatable
				data class Outer(val nested: Nested?)
				""".trimIndent(),
			)

			assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
			assertThat(result.messages).contains("not @Validatable")
			assertThat(result.messages).contains("@NoCascade")
		}
	}

	@Nested
	inner class CrossModule {

		@Test
		@DisplayName("unmarked cascade into a dependency type warns and emits GeneratedSchemaLookup")
		fun unmarkedDependencyWarnsAndLooksUp() {
			// ConstraintMetadata lives on the test classpath (validata-core jar) → containingFile == null.
			val result = KspCompileSupport.compile(
				"""
				package sample
				import io.ghaylan.validata.constraint.ConstraintMetadata
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class Outer(val meta: ConstraintMetadata?)
				""".trimIndent(),
			)

			assertThat(result.exitCode)
				.describedAs(result.messages)
				.isEqualTo(KotlinCompilation.ExitCode.OK)
			assertThat(result.messages).contains("cross-module")
			val generated = KspCompileSupport.generatedSourceText(result)
			assertThat(generated).contains(
				"GeneratedSchemaLookup.requireGeneratedSchema(ConstraintMetadata::class.java)",
			)
			assertThat(generated).doesNotContain(
				"ObjectSchema(type = io.ghaylan.validata.constraint.ConstraintMetadata::class.java, properties = emptyList())",
			)
		}

		@Test
		@DisplayName("strictCrossModuleCascade upgrades dependency unmarked cascade to an error")
		fun strictOptionErrors() {
			val result = KspCompileSupport.compile(
				source = """
				package sample
				import io.ghaylan.validata.constraint.ConstraintMetadata
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class Outer(val meta: ConstraintMetadata?)
				""".trimIndent(),
				kspOptions = mapOf(ProcessorOptions.STRICT_CROSS_MODULE_CASCADE to "true"),
			)

			assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
			assertThat(result.messages).contains("not @Validatable")
		}
	}
}
