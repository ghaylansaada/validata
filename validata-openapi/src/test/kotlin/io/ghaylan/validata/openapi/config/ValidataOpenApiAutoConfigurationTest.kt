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
package io.ghaylan.validata.openapi.config

import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import io.ghaylan.validata.openapi.springdoc.ValidataModelConverter
import io.ghaylan.validata.openapi.springdoc.ValidataOperationCustomizer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

/**
 * Guards auto-config wiring: customizer + converter present; no default error-doc publishers.
 * 
 * @author Ghaylan Saada
 */
class ValidataOpenApiAutoConfigurationTest {
	
	private val runner = WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ValidataOpenApiAutoConfiguration::class.java))
	
	@Test
	@DisplayName("auto-config registers OperationCustomizer and ValidataModelConverter")
	fun registersPlaceholders() {
		runner.run { context ->
			assertThat(context).hasBean("validataOperationCustomizer")
			assertThat(context).hasBean("validataModelConverter")
			assertThat(context).hasBean("validataOpenApiCacheResetCustomizer")
			assertThat(context.getBean("validataOperationCustomizer")).isInstanceOf(ValidataOperationCustomizer::class.java)
			assertThat(context.getBean("validataModelConverter")).isInstanceOf(ValidataModelConverter::class.java)
			assertThat(context.getBeansOfType(ErrorDocPublisher::class.java)).isEmpty()
		}
	}
}
