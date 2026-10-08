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
import io.ghaylan.validata.processor.SchemaProcessor
import io.ghaylan.validata.processor.SchemaProcessorRoundProbe
import io.ghaylan.validata.processor.support.KspCompileSupport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Documents multi-round deferral for unready `@Validatable` roots ([SchemaProcessor]).
 *
 * A companion stub processor generates a missing peer type after round 1 so
 * `KSClassDeclaration.validate()` can flip from false → true without a permanent skip.
 * 
 * @author Ghaylan Saada

 */
@OptIn(ExperimentalCompilerApi::class)
class SchemaProcessorMultiRoundTest {

	@BeforeEach
	fun resetProbe() {
		DeferredRoundProbe.reset()
		SchemaProcessorRoundProbe.reset()
	}

	@Test
	@DisplayName("unready @Validatable root is deferred then emitted once its peer type exists")
	fun defersUntilPeerTypeReady() {
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
		assertThat(DeferredRoundProbe.deferredSizes)
			.withFailMessage { "rounds=${DeferredRoundProbe.deferredSizes} messages=${result.messages}" }
			.isNotEmpty()
		assertThat(DeferredRoundProbe.deferredSizes.first())
			.withFailMessage { "expected non-empty deferral in round 1; got ${DeferredRoundProbe.deferredSizes}" }
			.isGreaterThan(0)
		assertThat(DeferredRoundProbe.deferredSizes.last()).isEqualTo(0)
		assertThat(KspCompileSupport.generatedSourceText(result))
			.contains("NeedsPeer")
			.contains("ObjectSchemasModule")
		// T8.5: endpoint rediscovery skipped while schemas deferred, then run once when ready.
		assertThat(SchemaProcessorRoundProbe.endpointBuildAllSkippedWhileDeferred)
			.isGreaterThan(0)
		assertThat(SchemaProcessorRoundProbe.endpointBuildAllCalls)
			.isEqualTo(1)
	}

	@Test
	@DisplayName("permanently broken cascade still fails the compilation")
	fun permanentlyBrokenStillErrors() {
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
	}
}
