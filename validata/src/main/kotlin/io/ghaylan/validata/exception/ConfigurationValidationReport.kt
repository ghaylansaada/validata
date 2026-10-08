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

import org.springframework.boot.diagnostics.FailureAnalysis

/**
 * Formats [ConfigurationValidationException] for humans and for Boot's failure analysis UI.
 *
 * Single formatting authority: the post-processor throws structured data; this object turns that
 * data into description and action text.*
 * 
 * @author Ghaylan Saada
 */
internal object ConfigurationValidationReport {
	
	/**
	 * Multi-line description of the validation failure (bean identity + each violation).
	 *
	 * @param failure structured configuration-validation exception
	 * @return report suitable for logs, exception messages, and [FailureAnalysis] description	 
	 */
	fun description(failure: ConfigurationValidationException): String {
		val prefix = failure.propertyPrefix
		return buildString {
			appendLine("Reason: Configuration validation failed for bean '${failure.beanName}'.")
			appendLine("Class : ${failure.targetClassName}")
			appendLine("Errors: ${failure.errors.size} violation(s) found.")
			appendLine()
			failure.errors.forEachIndexed { index, error ->
				val propertyPath = error.path?.let { "$prefix$it" } ?: "${prefix}[class-level]"
				appendLine("(${index + 1}) Property : $propertyPath")
				appendLine("    Path     : ${error.path ?: "(no path on this violation)"}")
				appendLine("    Code     : ${error.code}")
				appendLine("    Message  : ${error.message ?: "(no message on this violation)"}")
				appendLine()
			}
		}
	}
	
	/**
	 * Actionable guidance for Boot's [FailureAnalysis] "action" section.
	 *
	 * @param failure structured configuration-validation exception
	 * @return short remediation text naming the bean and, when known, the properties prefix	 
	 */
	fun action(failure: ConfigurationValidationException): String {
		val prefixHint = failure.propertyPrefix.takeIf { it.isNotEmpty() }
			?.let { " under '$it*'" }
			?: ""
		return "Correct the invalid configuration for bean '${failure.beanName}'$prefixHint, then restart the application."
	}
}
