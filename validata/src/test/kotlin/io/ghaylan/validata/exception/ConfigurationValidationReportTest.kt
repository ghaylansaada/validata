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
 * Formatting rules for [ConfigurationValidationReport].
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationReportTest {
	
	@Test
	@DisplayName("description prefixes property paths and lists each violation")
	fun descriptionIncludesPrefixedPaths() {
		val failure = ConfigurationValidationException(
			beanName = "mail",
			targetClassName = "com.example.MailProperties",
			propertyPrefix = "app.mail.",
			errors = listOf(
				ConstraintError(
					path = "host",
					code = ConstraintErrorCode.TEXT_TOO_SHORT,
					message = "too short",
				),
				ConstraintError(
					path = null,
					code = ConstraintErrorCode.VALUE_MISSING,
					message = "class level",
				),
			),
		)
		val text = ConfigurationValidationReport.description(failure)
		
		assertThat(text).contains("bean 'mail'")
		assertThat(text).contains("com.example.MailProperties")
		assertThat(text).contains("app.mail.host")
		assertThat(text).contains("app.mail.[class-level]")
		assertThat(text).contains("TEXT_TOO_SHORT")
		assertThat(text).contains("(no path on this violation)")
	}
	
	@Test
	@DisplayName("description uses explicit placeholders when message is absent")
	fun descriptionPlaceholdersForMissingFields() {
		val failure = ConfigurationValidationException(
			beanName = "mail",
			targetClassName = "com.example.MailProperties",
			propertyPrefix = "app.mail.",
			errors = listOf(
				ConstraintError(
					path = "host",
					code = ConstraintErrorCode.VALUE_MISSING,
					message = null,
				),
			),
		)
		val text = ConfigurationValidationReport.description(failure)
		
		assertThat(text).contains("VALUE_MISSING")
		assertThat(text).contains("(no message on this violation)")
		assertThat(text).doesNotContain("UNKNOWN")
		assertThat(text).doesNotContain("No descriptive message provided.")
	}
	
	@Test
	@DisplayName("description with empty prefix still renders paths without a double-dot")
	fun descriptionEmptyPrefix() {
		val failure = ConfigurationValidationException(
			beanName = "mail",
			targetClassName = "com.example.MailProperties",
			propertyPrefix = "",
			errors = listOf(
				ConstraintError(path = "host", code = ConstraintErrorCode.VALUE_MISSING, message = "required"),
			),
		)
		
		assertThat(ConfigurationValidationReport.description(failure)).contains("host")
	}
	
	@Test
	@DisplayName("action names the bean and properties prefix when present")
	fun actionNamesBeanAndPrefix() {
		val failure = ConfigurationValidationException(
			beanName = "mail",
			targetClassName = "com.example.MailProperties",
			propertyPrefix = "app.mail.",
			errors = emptyList(),
		)
		
		assertThat(ConfigurationValidationReport.action(failure)).contains("mail")
			.contains("app.mail.")
	}
	
	@Test
	@DisplayName("exception message delegates to ConfigurationValidationReport.description")
	fun exceptionMessageMatchesReport() {
		val failure = ConfigurationValidationException(
			beanName = "mail",
			targetClassName = "com.example.MailProperties",
			propertyPrefix = "",
			errors = listOf(
				ConstraintError(path = "host", code = ConstraintErrorCode.VALUE_MISSING, message = "required"),
			),
		)
		
		assertThat(failure.message).isEqualTo(ConfigurationValidationReport.description(failure))
	}
}
