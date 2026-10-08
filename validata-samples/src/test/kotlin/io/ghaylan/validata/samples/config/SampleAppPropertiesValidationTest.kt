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
package io.ghaylan.validata.samples.config

import io.ghaylan.validata.config.ValidationConfig
import io.ghaylan.validata.exception.ConfigurationValidationException
import io.ghaylan.validata.samples.NativeSurfaceIT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

/**
 * Non-web [ApplicationContextRunner] coverage for [SampleAppProperties] startup validation.
 *
 * Kept out of [NativeSurfaceIT] so a nested `@Configuration` cannot hijack the Boot test context.*
 * 
 * @author Ghaylan Saada
 */
class SampleAppPropertiesValidationTest {
	
	/** Non-web runner with Validata auto-config and nested [SampleAppPropertiesConfig].	 */
	private val runner = ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ValidationConfig::class.java))
		.withUserConfiguration(SampleAppPropertiesConfig::class.java)
	
	@Test
	@DisplayName("tenantId shorter than Size(min=2) fails with ConfigurationValidationException")
			/** tenantId shorter than Size(min=2) fails with ConfigurationValidationException			 */
	fun rejectsShortTenantId() {
		runner.withPropertyValues("sample.app.tenant-id=x")
			.run { context ->
				assertThat(context).hasFailed()
				assertThat(context.startupFailure).rootCause()
					.isInstanceOf(ConfigurationValidationException::class.java)
					.hasMessageContaining("sample.app.tenantId")
			}
	}
	
	@Test
	@DisplayName("valid tenantId starts the non-web configuration path")
			/** valid tenantId starts the non-web configuration path			 */
	fun acceptsValidTenantId() {
		runner.withPropertyValues("sample.app.tenant-id=ok")
			.run { context ->
				assertThat(context).hasNotFailed()
				assertThat(context.getBean(SampleAppProperties::class.java).tenantId).isEqualTo("ok")
			}
	}
	
	/**
	 * Enables [SampleAppProperties] without starting the sample web application.
	 */
	@Configuration
	@EnableConfigurationProperties(SampleAppProperties::class)
	class SampleAppPropertiesConfig
}
