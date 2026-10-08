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

/**
 * Lazily allocated cache shared by every [ValidationContext] frame in one validation tree
 * (one instance for the whole `ValidationCursor` run).
 *
 * Most validations never allocate; constraints such as `@Distinct` populate derived values on demand.
 * Keys are [Any] so callers may use immutable value keys or a reusable probe with the
 * [getOrCompute] `storeKey` overload.*
 * 
 * @author Ghaylan Saada
 */
class AttributeBag {
	
	/**
	 * Backing map; `null` until the first [getOrCompute] stores a value.
	 */
	private var map: MutableMap<Any, Any>? = null
	
	/**
	 * Returns a cached value for [key], computing it once via [compute] on first access.
	 *
	 * Mutates the bag: allocates the map on first use and retains [key] (must be immutable after insert).
	 *
	 * @param key Stable cache key for this validation run.
	 * @param compute Producer invoked at most once per [key].
	 * @return Cached or newly computed value for [key].	 
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T> getOrCompute(
		key: Any,
		compute: () -> T
	): T {
		val attributes = map ?: HashMap<Any, Any>(8).also { map = it }
		(attributes[key] as T?)?.let { return it }
		val computed = compute()
		attributes[key] = computed as Any
		return computed
	}
	
	/**
	 * Like [getOrCompute], but [lookupKey] is used only for map probing and need not be retained.
	 *
	 * On a miss, stores under [storeKey] (immutable key that equals [lookupKey]). Used by
	 * `@Distinct` to probe with a ThreadLocal key and allocate a stable key only on miss.
	 *
	 * Mutates the bag on miss: allocates the map if needed and retains [storeKey]'s result.
	 *
	 * @param lookupKey Probe key (may be reused or mutated after this call returns).
	 * @param storeKey Factory for the immutable key retained in the map (invoked only on miss).
	 * @param compute Producer invoked at most once per logical key.
	 * @return Cached or newly computed value for the logical key.	 
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T> getOrCompute(
		lookupKey: Any,
		storeKey: () -> Any,
		compute: () -> T
	): T {
		val attributes = map ?: HashMap<Any, Any>(8).also { map = it }
		(attributes[lookupKey] as T?)?.let { return it }
		val computed = compute()
		attributes[storeKey()] = computed as Any
		return computed
	}
	
	/**
	 * Whether no attributes have been stored.
	 *
	 * @return `true` when the map is absent or empty.	 
	 */
	fun isEmpty(): Boolean = map.isNullOrEmpty()
	
	/**
	 * Whether the backing map has been allocated (any store has occurred).
	 */
	val isAllocated: Boolean get() = map != null
}
