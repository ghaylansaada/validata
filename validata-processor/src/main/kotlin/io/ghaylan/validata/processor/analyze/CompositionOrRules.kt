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

import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.model.ConstraintModel

/**
 * Pure validation rules for `@ConstraintComposition(OR)` leaf lists.
 *
 * Used by [ConstraintModelBuilder] after leaves are built so OR rejection messages stay
 * unit-testable without KSP symbols.*
 * 
 * @author Ghaylan Saada
 */
internal object CompositionOrRules {
	
	/**
	 * Validates OR-composition leaf count and presence membership.
	 *
	 * No side effects.
	 *
	 * @param composedName simple name of the OR-composed annotation
	 * @param children built leaf constraint models
	 * @return diagnostic message when [children] is not a legal OR leaf set, or `null` when OK	 
	 */
	fun validateLeaves(
		composedName: String,
		children: List<ConstraintModel>,
	): String? {
		if (children.size < 2) {
			return "@$composedName uses @ConstraintComposition(OR) but declares ${children.size} " + "leaf @Constraint member(s); OR requires at least 2."
		}
		val presence = children.filter { isPresenceAnnotation(it.annotationSimpleName) }
		if (presence.isNotEmpty()) {
			return "@$composedName uses @ConstraintComposition(OR) but includes presence constraint(s) " + "${presence.joinToString { it.annotationSimpleName }}. Presence must stay outside OR."
		}
		return null
	}
	
	/** Exact match on built-in presence simple names (same set as [ConstraintModelBuilder]).
	 *
	 * No side effects.
	 *
	 * @param simpleName constraint annotation simple name
	 * @return `true` for built-in presence constraints	 
	 */
	fun isPresenceAnnotation(simpleName: String): Boolean =
		simpleName in AnnotationAttrs.PresenceSimpleNames.ALL
}
