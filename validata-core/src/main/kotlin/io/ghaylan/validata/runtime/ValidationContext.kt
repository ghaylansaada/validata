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
package io.ghaylan.validata.runtime

import io.ghaylan.validata.schema.shape.TypeShape
import java.time.Clock
import kotlin.reflect.KClass

/**
 * Read-only validation state visible to constraint validators during a single constraint evaluation.
 *
 * The engine owns a mutable cursor that implements this interface. Validators must treat the instance
 * as a snapshot for the duration of `ConstraintValidator.validate` only —
 * do not retain it across calls. Not request-scoped and not safe to share across threads; one cursor
 * walks one tree on one thread.*
 * 
 * @author Ghaylan Saada
 */
interface ValidationContext {
	
	/**
	 * Lazy path rope; materialize with [fieldPath] only when emitting an error.	 
	 */
	val path: PathSegment
	
	/**
	 * Wire / parameter name of the current field.	 
	 */
	val fieldName: String
	
	/**
	 * IR shape of the current field, if known.	 
	 */
	val shape: TypeShape?
	
	/**
	 * When `true`, stop after the first violation on this param.	 
	 */
	val oneErrorPerParam: Boolean
	
	/**
	 * When `true`, stop after the first violation anywhere in the request (whole-request abort from
	 * `@Validate.failFast`).	 
	 */
	val failFast: Boolean
	
	/**
	 * When `true`, every constraint on this run is already known to be active under [groups]
	 * (engine group fast-path); validators must skip intersection work. Never set without that proof
	 * — incorrect `true` would run group-scoped constraints that should have been skipped.	 
	 */
	val skipGroupChecks: Boolean
	
	/**
	 * Index within an iterable, or [NO_ELEMENT_INDEX].	 
	 */
	val elementIndex: Int
	
	/**
	 * Array metadata for cross-element constraints (e.g. `@Distinct`), if any.	 
	 */
	val array: ValidationContextValue<List<Any>>?
	
	/**
	 * Parent object metadata for sibling property reads, if any.	 
	 */
	val containerObject: ValidationContextValue<Any>?
	
	/**
	 * Nesting depth from the validation root (limit-checked by the engine).	 
	 */
	val depth: Int
	
	/**
	 * Shared per-run attribute cache (same instance for the whole tree).	 
	 */
	val attributeBag: AttributeBag
	
	/**
	 * Active validation groups for group filtering.	 
	 */
	val groups: Set<KClass<*>>
	
	/**
	 * Clock for relative temporal constraints (`@RelativeToNow`).
	 *
	 * Fixed for the validation run; defaults to the system clock. Tests may inject a fixed clock.
	 * Prefer `nowMatching` (constraint.ext) so "now" is memoized per run.	 
	 */
	val clock: Clock get() = Clock.systemDefaultZone()
	
	/**
	 * Materialized path string for error payloads.
	 *
	 * Call only on the failure path — success must not pay for path construction.	 
	 */
	val fieldPath: String get() = path.toPathString()
	
	/**
	 * Returns a cached attribute for [key], computing it once per validation run via [attributeBag].
	 *
	 * May mutate [attributeBag] on first access for [key].
	 *
	 * @param key Cache key stable for the run (string, or an immutable value key).
	 * @param compute Producer invoked at most once per key.
	 * @return Cached or newly computed attribute value.	 
	 */
	fun <T> getOrComputeAttribute(
		key: Any,
		compute: () -> T
	): T = attributeBag.getOrCompute(key, compute)
	
	/**
	 * Probe with a reusable [lookupKey], store under an immutable [storeKey] on miss.
	 *
	 * Delegates to [AttributeBag.getOrCompute] on [attributeBag]; may mutate the bag on miss.
	 *
	 * @param lookupKey Probe key (may be reused or mutated after this call returns).
	 * @param storeKey Factory for the immutable key retained in the map (invoked only on miss).
	 * @param compute Producer invoked at most once per logical key.
	 * @return Cached or newly computed attribute value.	 
	 */
	fun <T> getOrComputeAttribute(
		lookupKey: Any,
		storeKey: () -> Any,
		compute: () -> T
	): T = attributeBag.getOrCompute(lookupKey, storeKey, compute)
	
	/**
	 * Whether [fieldPath] already recorded a violation under [oneErrorPerParam].
	 *
	 * Engine cursor implements this with an O(1) set; test doubles default to `false`.
	 * Call only when [oneErrorPerParam] is true — success path must not allocate.
	 *
	 * @return `true` when this wire path already failed in the current run.	 
	 */
	fun hasFailedPath(): Boolean = false
	
	/**
	 * Records that [fieldPath] failed so later constraint lists on the same path are skipped.
	 *
	 * No-op in test doubles. Engine cursor stores the path in a run-scoped set.	 
	 */
	fun markPathFailed() {}
	
	companion object {
		
		/**
		 * Sentinel used when the current context is not an element of an iterable.
		 */
		const val NO_ELEMENT_INDEX: Int = -1
	}
}
