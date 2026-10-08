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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.model.ConstraintError

/**
 * Path+code deduplication and natural-path ordering for constraint error lists.
 *
 * Extracted from [ValidatorEngine] so the façade stays thin.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintErrorDeduper {

	/**
	 * Orders constraint errors by natural path, then by error code name for deterministic ties.
	 *
	 * Example: `users[2].name` before `users[10].name`; same path sorts by code name.
	 */
	private val ERROR_ORDER: Comparator<ConstraintError<*>> =
		compareBy(NaturalPathOrder) { error: ConstraintError<*> -> error.path.orEmpty() }
			.thenBy { (it.code as? Enum<*>)?.name.orEmpty() }

	/**
	 * Deduplicates by path+code and sorts with [NaturalPathOrder].
	 *
	 * No mutation of [errors]; returns [emptyList] when empty, [errors] unchanged when size == 1.
	 *
	 * @param errors Raw accumulated errors (may contain duplicates).
	 * @return Stable, unique list; identity for size ≤ 1 to avoid HashSet + sort on the success path.
	 */
	fun deduplicate(errors: List<ConstraintError<*>>): List<ConstraintError<*>> {
		// Success must return the shared emptyList (not a grown ArrayList / DeferredErrorList).
		if (errors.isEmpty()) return emptyList()
		
		// Single-error responses must not pay for HashSet + sort (Phase 4.1).
		if (errors.size == 1) return errors
		
		val capacity = errors.size
		val seen = HashSet<ConstraintErrorKey>(capacity)
		val unique = ArrayList<ConstraintError<*>>(capacity)

		for (error in errors) {
			val key = ConstraintErrorKey(error.path, error.code)
			if (seen.add(key)) {
				unique.add(error)
			}
		}

		unique.sortWith(ERROR_ORDER)
		return unique
	}
	
	
	/**
	 * Deduplication key for constraint errors within a single validation run
	 * (same path + same error code → keep one).
	 *
	 * @property field Error path, or `null` for path-less errors.
	 * @property code Error code object used for equality (always present on [ConstraintError]).
	 *
	 * @author Ghaylan Saada
	 */
	private data class ConstraintErrorKey(
		val field: String?,
		val code: Any)
}
