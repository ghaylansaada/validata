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
package io.ghaylan.validata.processor.contract

import io.ghaylan.validata.processor.ProcessorOptions
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming
import io.ghaylan.validata.processor.verify.PropertyReferenceVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Freezes KSP option keys and allowed naming values from the SemVer public surface.
 *
 * Renaming a key or dropping `IDENTITY` / `SNAKE_CASE` is a SemVer break.
 *
 * @author Ghaylan Saada
 */
class ProcessorOptionsContractTest {

	@Test
	@DisplayName("KSP option keys stay validata.strictCrossModuleCascade / maxShapeNestingDepth / jackson.naming")
	fun optionKeysAreFrozen() {
		assertThat(ProcessorOptions.STRICT_CROSS_MODULE_CASCADE)
			.isEqualTo("validata.strictCrossModuleCascade")
		assertThat(ProcessorOptions.MAX_SHAPE_NESTING_DEPTH)
			.isEqualTo("validata.maxShapeNestingDepth")
		assertThat(ProcessorOptions.JACKSON_NAMING)
			.isEqualTo("validata.jackson.naming")
	}

	@Test
	@DisplayName("jackson.naming accepts only IDENTITY and SNAKE_CASE entry names")
	fun jacksonNamingEntriesAreFrozen() {
		assertThat(JacksonPropertyNaming.entries.map { it.name })
			.containsExactly("IDENTITY", "SNAKE_CASE")
	}

	@Test
	@DisplayName("defaults stay cascade=false, depth=32, naming=IDENTITY")
	fun optionDefaultsAreFrozen() {
		assertThat(ProcessorOptions.strictCrossModuleCascade(emptyMap())).isFalse()
		assertThat(ProcessorOptions.maxShapeNestingDepth(emptyMap()))
			.isEqualTo(PropertyReferenceVerifier.DEFAULT_MAX_SHAPE_NESTING_DEPTH)
			.isEqualTo(32)
		assertThat(ProcessorOptions.jacksonNaming(emptyMap()))
			.isEqualTo(JacksonPropertyNaming.IDENTITY)
	}
}
