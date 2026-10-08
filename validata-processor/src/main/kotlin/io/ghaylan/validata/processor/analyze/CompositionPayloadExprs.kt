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
package io.ghaylan.validata.processor.analyze

import io.ghaylan.validata.processor.compat.KotlinStringLiteral
import io.ghaylan.validata.processor.fqns.ProcessorFqns

/**
 * Pure Kotlin-source expressions for OR-composition outer `message` / `groups`.
 *
 * Kept free of KSP symbols so [ConstraintCompositionExpander] payload rendering stays unit-testable.*
 * 
 * @author Ghaylan Saada
 */
internal object CompositionPayloadExprs {
	
	/** Kotlin string literal for a composition `message` (empty when [raw] is null).
	 *
	 * No side effects.
	 *
	 * @param raw usage-site message text, or `null` when omitted
	 * @return Kotlin string literal expression	 
	 */
	fun message(raw: String?): String {
		if (raw == null) return "\"\""
		return "\"${KotlinStringLiteral.escape(raw)}\""
	}
	
	/**
	 * Kotlin `setOf(…::class)` for composition `groups`.
	 *
	 * @param groupFqcns Fully qualified group class names, or null/empty for [ProcessorFqns.ON_DEFAULT].
	 * @return Kotlin `setOf(…::class)` expression	 
	 */
	fun groups(groupFqcns: List<String>?): String {
		if (groupFqcns.isNullOrEmpty()) {
			return "setOf(${ProcessorFqns.ON_DEFAULT}::class)"
		}
		val classes = groupFqcns.map { "$it::class" }
		return "setOf(${classes.joinToString(", ")})"
	}
}
