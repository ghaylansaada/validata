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

import io.ghaylan.validata.config.fixture.MailProperties
import io.ghaylan.validata.config.fixture.MailPropertiesConfig
import io.ghaylan.validata.exception.ConfigurationValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Boot ApplicationContextRunner coverage for `@Validate` `@ConfigurationProperties` startup.
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationIntegrationTest {
	
	private val runner = ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ValidationConfig::class.java))
		.withUserConfiguration(MailPropertiesConfig::class.java)
	
	@Test
	@DisplayName("the framework starts in a non-web context")
	fun startsWithoutWebMvc() {
		runner.withPropertyValues("app.mail.host=smtp.example.com", "app.mail.port=587")
			.run { context -> assertThat(context).hasNotFailed() }
	}
	
	@Test
	@DisplayName("a valid @Validate ConfigurationProperties bean is accepted")
	fun validConfiguration() {
		runner.withPropertyValues("app.mail.host=smtp.example.com", "app.mail.port=587")
			.run { context ->
				assertThat(context.getBean<MailProperties>().host).isEqualTo("smtp.example.com")
				assertThat(context.getBean<MailProperties>().port).isEqualTo(587)
			}
	}
	
	@Test
	@DisplayName("an invalid @Validate bean fails startup naming prefixed properties")
	fun invalidConfigurationFailsStartup() {
		runner.withPropertyValues("app.mail.host=x", "app.mail.port=0")
			.run { context ->
				assertThat(context).hasFailed()
				assertThat(context.startupFailure).rootCause()
					.isInstanceOf(ConfigurationValidationException::class.java)
					.hasMessageContaining("app.mail.host")
					.hasMessageContaining("app.mail.port")
			}
	}
}
