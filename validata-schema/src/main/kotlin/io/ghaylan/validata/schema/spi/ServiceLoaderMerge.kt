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
package io.ghaylan.validata.schema.spi

import java.util.*

/**
 * Shared merge helpers for ServiceLoader-backed schema SPI aggregators.
 *
 * First contributor wins recording; a second claim for the same key fails. Returned maps are unmodifiable.*
 * 
 * @author Ghaylan Saada
 */
internal object ServiceLoaderMerge {

	/**
	 * Discovers every [moduleType] on the classpath and merges [schemas] contributions.
	 *
	 * Performs ServiceLoader classpath discovery; returned map is unmodifiable.
	 *
	 * @param K contribution map key type
	 * @param V contribution map value type
	 * @param M SPI module type loaded via [ServiceLoader]
	 * @param moduleType SPI interface loaded via [ServiceLoader]
	 * @param schemas extracts the contribution map from one module instance
	 * @param duplicateMessage builds the failure text when a key is claimed twice
	 * @return unmodifiable merged map
	 * @throws IllegalStateException when two modules contribute the same key
	 */
	fun <K, V, M> load(
		moduleType: Class<M>,
		schemas: (M) -> Map<K, V>,
		duplicateMessage: (key: K, firstOwner: String, secondOwner: String) -> String,
	): Map<K, V> {
		val loader = moduleType.classLoader
		// ServiceLoader yields platform types (M!); skip nulls so the merge stays non-null.
		val contributions = ServiceLoader.load(moduleType, loader).mapNotNull { module ->
			module?.let { it.javaClass.name to schemas(it) }
		}
		return merge(contributions, duplicateMessage)
	}

	/**
	 * Merges pre-resolved (owner → map) contributions — used by tests without ServiceLoader.
	 *
	 * No I/O; builds a new unmodifiable map from [contributions].
	 *
	 * @param K contribution map key type
	 * @param V contribution map value type
	 * @param contributions pairs of owner class name and that owner's schemas
	 * @param duplicateMessage builds the failure text when a key is claimed twice
	 * @return unmodifiable merged map
	 * @throws IllegalStateException when two owners contribute the same key
	 */
	fun <K, V> merge(
		contributions: Iterable<Pair<String, Map<K, V>>>,
		duplicateMessage: (key: K, firstOwner: String, secondOwner: String) -> String,
	): Map<K, V> {
		var estimatedSize = 0
		for ((_, schemas) in contributions) {
			estimatedSize += schemas.size
		}
		val merged = LinkedHashMap<K, V>(mapCapacity(estimatedSize))
		val owners = HashMap<K, String>(mapCapacity(estimatedSize))
		for ((owner, schemas) in contributions) {
			for ((key, value) in schemas) {
				val previousOwner = owners.put(key, owner)
				if (previousOwner != null) {
					error(duplicateMessage(key, previousOwner, owner))
				}
				merged[key] = value
			}
		}
		// Unmodifiable: SPI caches are process-wide; accidental put/remove would corrupt all callers.
		return Collections.unmodifiableMap(merged)
	}

	/**
	 * Hash map initial capacity for [expectedSize] entries at the default load factor (0.75).
	 *
	 * Visible for unit tests that lock the sizing formula.
	 */
	internal fun mapCapacity(expectedSize: Int): Int {
		if (expectedSize < 3) return expectedSize + 1
		if (expectedSize < (1 shl 30)) return ((expectedSize / 0.75f) + 1).toInt()
		return Int.MAX_VALUE
	}
}
