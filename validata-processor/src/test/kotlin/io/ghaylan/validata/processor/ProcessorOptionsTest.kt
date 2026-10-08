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
package io.ghaylan.validata.processor

import io.ghaylan.validata.processor.support.RecordingKspLogger
import io.ghaylan.validata.processor.verify.PropertyReferenceVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [ProcessorOptions] parsing.
 * 
 * @author Ghaylan Saada
 */
class ProcessorOptionsTest {
	
	@Test
	@DisplayName("strictCrossModuleCascade defaults false and accepts true")
	fun strictCascade() {
		assertThat(ProcessorOptions.strictCrossModuleCascade(emptyMap())).isFalse()
		assertThat(ProcessorOptions.strictCrossModuleCascade(mapOf("validata.strictCrossModuleCascade" to "true"))).isTrue()
		assertThat(ProcessorOptions.strictCrossModuleCascade(mapOf("validata.strictCrossModuleCascade" to "TRUE"))).isTrue()
		assertThat(ProcessorOptions.strictCrossModuleCascade(mapOf("validata.strictCrossModuleCascade" to "false"))).isFalse()
	}
	
	@Test
	@DisplayName("maxShapeNestingDepth defaults and rejects invalid values")
	fun maxShapeNestingDepth() {
		assertThat(ProcessorOptions.maxShapeNestingDepth(emptyMap())).isEqualTo(PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH)
		assertThat(ProcessorOptions.maxShapeNestingDepth(mapOf("validata.maxShapeNestingDepth" to "1"))).isEqualTo(1)
		assertThat(ProcessorOptions.maxShapeNestingDepth(mapOf("validata.maxShapeNestingDepth" to "0"))).isEqualTo(0)
		assertThat(ProcessorOptions.maxShapeNestingDepth(mapOf("validata.maxShapeNestingDepth" to "nope"))).isEqualTo(PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH)
		assertThat(ProcessorOptions.maxShapeNestingDepth(mapOf("validata.maxShapeNestingDepth" to "-3"))).isEqualTo(PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH)
	}
	
	@Test
	@DisplayName("maxShapeNestingDepth warns via logger when value is invalid")
	fun maxShapeNestingDepthWarns() {
		val logger = RecordingKspLogger()
		assertThat(
			ProcessorOptions.maxShapeNestingDepth(
				mapOf("validata.maxShapeNestingDepth" to "nope"),
				logger,
			),
		).isEqualTo(PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH)
		assertThat(logger.warnings).anyMatch { it.contains("validata.maxShapeNestingDepth") }
	}

	@Test
	@DisplayName("jacksonNaming defaults IDENTITY and accepts SNAKE_CASE")
	fun jacksonNaming() {
		assertThat(ProcessorOptions.jacksonNaming(emptyMap())).isEqualTo(
			io.ghaylan.validata.processor.naming.JacksonPropertyNaming.IDENTITY,
		)
		assertThat(
			ProcessorOptions.jacksonNaming(mapOf("validata.jackson.naming" to "SNAKE_CASE")),
		).isEqualTo(io.ghaylan.validata.processor.naming.JacksonPropertyNaming.SNAKE_CASE)
		assertThat(
			ProcessorOptions.jacksonNaming(mapOf("validata.jackson.naming" to "snake_case")),
		).isEqualTo(io.ghaylan.validata.processor.naming.JacksonPropertyNaming.SNAKE_CASE)
		assertThat(
			ProcessorOptions.jacksonNaming(mapOf("validata.jackson.naming" to "IDENTITY")),
		).isEqualTo(io.ghaylan.validata.processor.naming.JacksonPropertyNaming.IDENTITY)
	}

	@Test
	@DisplayName("jacksonNaming warns via logger when value is invalid")
	fun jacksonNamingWarns() {
		val logger = RecordingKspLogger()
		assertThat(
			ProcessorOptions.jacksonNaming(
				mapOf("validata.jackson.naming" to "kebab"),
				logger,
			),
		).isEqualTo(io.ghaylan.validata.processor.naming.JacksonPropertyNaming.IDENTITY)
		assertThat(logger.warnings).anyMatch { it.contains("validata.jackson.naming") }
	}
}
