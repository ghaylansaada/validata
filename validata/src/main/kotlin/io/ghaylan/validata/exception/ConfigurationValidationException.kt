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

/**
 * Thrown during bootstrap when configuration validation detects constraint violations.
 *
 * Carries structured context so callers (logs, tests, Boot [ConfigurationValidationFailureAnalyzer])
 * can render or inspect the failure without parsing a pre-baked string. The [message] is produced
 * by [ConfigurationValidationReport.description] from that payload.
 *
 * ```kotlin
 * assertThat(context.startupFailure)
 *     .rootCause()
 *     .isInstanceOf(ConfigurationValidationException::class.java)
 *     .hasMessageContaining("app.mail.host")
 * ```
 *
 * @property beanName Spring bean name that failed validation.
 * @property targetClassName Binary name of the validated type (proxy target when applicable).
 * @property propertyPrefix Configuration-properties prefix including a trailing `.` when present
 *   (e.g. `app.mail.`), or empty when the bean has no prefix.
 * @property errors Constraint failures from the validation engine.
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationException(
	val beanName: String,
	val targetClassName: String,
	val propertyPrefix: String,
	val errors: List<ConstraintError<*>>,
): RuntimeException() {
	
	/**
	 * Multi-line report from [ConfigurationValidationReport.description], rebuilt on each read.
	 */
	override val message: String get() = ConfigurationValidationReport.description(this)
}
