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
package io.ghaylan.validata.config

import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.engine.ValidatorEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Autoconfig binding: `validata.limits.*` → [ValidatorEngine.limits].
 * 
 * @author Ghaylan Saada
 */
class ValidataLimitsPropertiesBindingTest {
	
	@Test
	@DisplayName("validata.limits.* bind onto the ValidatorEngine bean")
	fun limitsReachEngine() {
		ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ValidationConfig::class.java))
			.withPropertyValues(
				"validata.limits.max-depth=7",
				"validata.limits.max-elements-per-container=11",
				"validata.limits.max-errors=13",
			)
			.run { context ->
				val limits = context.getBean<ValidatorEngine>().limits
				assertThat(limits).isEqualTo(
					ValidationLimits(maxDepth = 7, maxElementsPerContainer = 11, maxErrors = 13),
				)
			}
	}
	
	@Test
	@DisplayName("unbound properties leave engine defaults in place")
	fun unboundKeepsDefaults() {
		ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ValidationConfig::class.java))
			.run { context ->
				assertThat(context.getBean<ValidatorEngine>().limits).isEqualTo(ValidationLimits())
			}
	}
}
