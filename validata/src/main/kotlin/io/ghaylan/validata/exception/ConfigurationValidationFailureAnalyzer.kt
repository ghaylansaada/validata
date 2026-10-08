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

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis

/**
 * Boot [AbstractFailureAnalyzer] for [ConfigurationValidationException].
 *
 * Maps the structured exception into Boot's description/action pair via
 * [ConfigurationValidationReport]. Does not re-validate or invent violation details.*
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationFailureAnalyzer: AbstractFailureAnalyzer<ConfigurationValidationException>() {
	
	/**
	 * Builds a [FailureAnalysis] from the structured configuration-validation failure.
	 *
	 * @param rootFailure root cause thrown during bootstrap
	 * @param cause [ConfigurationValidationException] carrying bean identity and errors
	 * @return analysis with report description and remediation action	 
	 */
	override fun analyze(
		rootFailure: Throwable,
		cause: ConfigurationValidationException,
	): FailureAnalysis {
		val description = ConfigurationValidationReport.description(cause)
		val action = ConfigurationValidationReport.action(cause)
		return FailureAnalysis(description, action, cause)
	}
}
