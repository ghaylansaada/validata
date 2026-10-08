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
package io.ghaylan.validata.exception

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Boot failure-analyzer mapping for configuration validation.
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationFailureAnalyzerTest {
	
	@Test
	@DisplayName("maps a nested ConfigurationValidationException via ConfigurationValidationReport")
	fun usesStructuredReport() {
		val cause = ConfigurationValidationException(
			beanName = "mail-properties",
			targetClassName = "com.example.MailProperties",
			propertyPrefix = "app.mail.",
			errors = listOf(
				ConstraintError(
					path = "host",
					code = ConstraintErrorCode.VALUE_MISSING,
					message = "required",
				),
			),
		)
		val root = RuntimeException("bootstrap failed", cause)
		val analysis = ConfigurationValidationFailureAnalyzer().analyze(root)
		
		assertThat(analysis).isNotNull
		assertThat(analysis!!.description).isEqualTo(ConfigurationValidationReport.description(cause))
		assertThat(analysis.description).contains("app.mail.host")
		assertThat(analysis.action).isEqualTo(ConfigurationValidationReport.action(cause))
		assertThat(analysis.action).contains("mail-properties")
		assertThat(analysis.cause).isSameAs(cause)
	}
	
	@Test
	@DisplayName("returns null when the failure chain has no ConfigurationValidationException")
	fun unrelatedFailure() {
		val analysis = ConfigurationValidationFailureAnalyzer().analyze(IllegalStateException("other"))
		assertThat(analysis).isNull()
	}
}
