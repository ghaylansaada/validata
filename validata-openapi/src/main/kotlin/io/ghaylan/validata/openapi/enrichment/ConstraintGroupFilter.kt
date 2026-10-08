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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.schema.constraint.ConstraintConfig
import io.ghaylan.validata.constraint.ConstraintGroupMatching
import kotlin.reflect.KClass

/**
 * OpenAPI group gating for constraint facets, vendor extensions, and error-code collection.
 *
 * Empty constraint groups or empty `activeGroups` mean the constraint is treated as active
 * (documentation union / “show everything” when no endpoint group context).
 *
 * This is intentionally distinct from runtime [ConstraintGroupMatching]
 * (empty context groups never match a non-empty constraint set during validation).*
 * 
 * @author Ghaylan Saada
 */
object ConstraintGroupFilter {
	
	/**
	 * Whether [metadata] participates under [activeGroups] for OpenAPI enrichment.
	 *
	 * @param metadata constraint config carrying optional validation groups
	 * @param activeGroups groups active for the endpoint; empty means all constraints active
	 * @return `true` if the constraint should contribute facets / extensions / codes	 
	 */
	fun isActive(
		metadata: ConstraintConfig,
		activeGroups: Set<KClass<*>>
	): Boolean {
		if (metadata.groups.isEmpty()) return true
		if (activeGroups.isEmpty()) return true
		return metadata.groups.any { it in activeGroups }
	}
}
