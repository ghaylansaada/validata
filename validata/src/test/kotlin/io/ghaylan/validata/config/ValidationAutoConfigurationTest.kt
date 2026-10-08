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

import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.web.ValidatedEndpointPlanCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations

/**
 * Full auto-config set for non-web and web runners.
 *
 * Threat: web MVC auto-config without [ConditionalOnWebApplication] fails non-web Boot apps that
 * only need `@ConfigurationProperties` validation.
 * 
 * @author Ghaylan Saada
 */
class ValidationAutoConfigurationTest {
	
	private val autoConfigurations = AutoConfigurations.of(
		ValidationConfig::class.java,
		WebMvcValidationAutoConfiguration::class.java,
		ValidataWebMvcConfiguration::class.java,
	)
	
	@Nested
	@DisplayName("Non-web ApplicationContextRunner")
	inner class NonWeb {
		
		private val runner = ApplicationContextRunner().withConfiguration(autoConfigurations)
		
		@Test
		@DisplayName("starts with registry, engine, and post-processor, and without MVC plan cache")
		fun nonWebStartsWithoutMvcBeans() {
			runner.run { context ->
				assertThat(context).hasNotFailed()
				assertThat(context).hasSingleBean(ValidationRegistry::class.java)
				assertThat(context).hasSingleBean(ValidatorEngine::class.java)
				assertThat(context).hasSingleBean(ConfigurationValidationPostProcessor::class.java)
				assertThat(context).doesNotHaveBean(ValidatedEndpointPlanCache::class.java)
				assertThat(context).doesNotHaveBean(WebMvcRegistrations::class.java)
				assertThat(context).doesNotHaveBean(WebMvcValidationAutoConfiguration::class.java)
				assertThat(context).doesNotHaveBean(ValidataWebMvcConfiguration::class.java)
			}
		}
		
		@Test
		@DisplayName("the module ships IDE metadata for validata.limits.* with the engine defaults")
		fun configurationMetadataIsOnTheClasspath() {
			val resource = checkNotNull(
				javaClass.classLoader.getResource("META-INF/spring-configuration-metadata.json"),
			) { "META-INF/spring-configuration-metadata.json is missing from the test classpath" }
			val text = resource.readText()
			assertThat(text).contains("validata.limits.max-depth")
			assertThat(text).contains("validata.limits.max-elements-per-container")
			assertThat(text).contains("validata.limits.max-errors")
			assertThat(text).contains("\"defaultValue\": 32")
			assertThat(text).contains("\"defaultValue\": 10000")
			assertThat(text).contains("\"defaultValue\": 200")
		}
	}
	
	@Nested
	@DisplayName("WebApplicationContextRunner")
	inner class Web {
		
		private val runner = WebApplicationContextRunner().withConfiguration(autoConfigurations)
		
		@Test
		@DisplayName("starts with plan cache, WebMvcRegistrations, and ValidataWebMvcConfiguration")
		fun webStartsWithMvcRegistrations() {
			runner.run { context ->
				assertThat(context).hasNotFailed()
				assertThat(context).hasSingleBean(ValidatedEndpointPlanCache::class.java)
				assertThat(context).hasSingleBean(WebMvcRegistrations::class.java)
				assertThat(context.getBean<WebMvcRegistrations>()).isInstanceOf(WebMvcValidationAutoConfiguration::class.java)
				assertThat(context).hasSingleBean(ValidataWebMvcConfiguration::class.java)
			}
		}
	}
}
