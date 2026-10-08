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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.runtime.ValidationContext
import kotlin.reflect.KClass

/**
 * Shared group-intersection check for validators and composition runners.
 *
 * Empty constraint groups always match. Empty context groups never match a non-empty constraint
 * set. Otherwise iterates the smaller set looking for membership in the larger (typical size 1–3).*
 * 
 * @author Ghaylan Saada
 */
object ConstraintGroupMatching {
	
	/**
	 * Returns whether a constraint with [constraintGroups] should run under [context].
	 *
	 * No I/O or mutation. Honors [ValidationContext.skipGroupChecks].
	 *
	 * @param constraintGroups Groups declared on the constraint metadata.
	 * @param context Active validation cursor.
	 * @return `true` when validation should proceed.	 
	 */
	fun shouldRun(
		constraintGroups: Set<KClass<*>>,
		context: ValidationContext
	): Boolean {
		if (context.skipGroupChecks) return true
		if (constraintGroups.isEmpty()) return true
		if (context.groups.isEmpty()) return false
		val small: Set<KClass<*>>
		val large: Set<KClass<*>>
		if (constraintGroups.size <= context.groups.size) {
			small = constraintGroups
			large = context.groups
		}
		else {
			small = context.groups
			large = constraintGroups
		}
		
		for (validationGroup in small) {
			if (validationGroup in large) return true
		}
		return false
	}
}
