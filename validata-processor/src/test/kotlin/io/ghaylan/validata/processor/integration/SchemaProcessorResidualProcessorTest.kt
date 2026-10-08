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
import io.ghaylan.validata.processor.SchemaProcessorRoundProbe
import io.ghaylan.validata.processor.support.KspCompileSupport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Residual SchemaProcessor / schema-builder edges for publish confidence.
 * 
 * @author Ghaylan Saada

 */
@OptIn(ExperimentalCompilerApi::class)
class SchemaProcessorResidualProcessorTest {

	@BeforeEach
	fun resetProbe() {
		DeferredRoundProbe.reset()
		SchemaProcessorRoundProbe.reset()
	}

	@Test
	@DisplayName("empty @Validatable concrete type is skipped with a warning")
	fun emptyValidatableType_skippedWithWarning() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validatable

			@Validatable
			class EmptyDto
			""".trimIndent(),
			providers = KspCompileSupport.Providers.NONE,
			extraProviders = listOf(ProbingSchemaProcessorProvider()),
		)
		assertThat(result.exitCode)
			.withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("no readable properties")
		assertThat(KspCompileSupport.generatedSourceText(result))
			.doesNotContain("EmptyDtoSchema")
	}

	@Test
	@DisplayName("process returns early after completion without rediscovering endpoints")
	fun completed_earlyReturn_noExtraEndpointBuild() {
		val result = KspCompileSupport.compile(
			source = """
				package sample
				import io.ghaylan.validata.schema.NoCascade
				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class NeedsPeer(
					@NoCascade val peer: DeferredPeer?,
				)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.NONE,
			extraProviders = listOf(
				ProbingSchemaProcessorProvider(),
				DeferredPeerStubProcessorProvider(),
			),
		)
		assertThat(result.exitCode)
			.withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(SchemaProcessorRoundProbe.endpointBuildAllCalls).isEqualTo(1)
		// KSP typically invokes process after finalize; completed guard must no-op.
		assertThat(SchemaProcessorRoundProbe.completedEarlyReturns)
			.withFailMessage {
				"expected at least one post-complete process no-op; " +
					"deferred=${DeferredRoundProbe.deferredSizes} " +
					"early=${SchemaProcessorRoundProbe.completedEarlyReturns}"
			}
			.isGreaterThan(0)
	}

	@Test
	@DisplayName("validata.maxShapeNestingDepth option fails deeply nested iterable shapes")
	fun maxShapeNestingDepth_option_failsDeepNesting() {
		val result = KspCompileSupport.compile(
			source = """
				package sample

				import io.ghaylan.validata.schema.Validatable

				@Validatable
				data class DeepNest(
					val nested: List<List<List<List<String>>>>?,
				)
			""".trimIndent(),
			kspOptions = mapOf("validata.maxShapeNestingDepth" to "2"),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("exceeds max depth 2")
	}
}
