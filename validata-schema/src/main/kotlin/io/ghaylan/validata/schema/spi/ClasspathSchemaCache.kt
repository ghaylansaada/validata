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

import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas

/**
 * Thread-safe, lazily loaded immutable classpath snapshot with a private monitor.
 *
 * Shared by [GeneratedSchemas] and [GeneratedRequestSchemas] so cache / lock / reset semantics
 * cannot drift. Not part of the public API.
 *
 * @param K map key type
 * @param V map value type
 * @param load builds the immutable snapshot (typically via [ServiceLoaderMerge])*
 * 
 * @author Ghaylan Saada
 */
internal class ClasspathSchemaCache<K, V>(
	private val load: () -> Map<K, V>,
) {
	
	/**
	 * Private monitor — never expose this (or the owning public `object`) as a lock.
	 */
	private val lock = Any()
	
	@Volatile
	private var cached: Map<K, V>? = null
	
	/**
	 * Returns the cached snapshot, loading once under [lock] on first use.
	 */
	fun all(): Map<K, V> {
		cached?.let { return it }
		return synchronized(lock) {
			cached ?: load().also { cached = it }
		}
	}
	
	/**
	 * Looks up [key] in the snapshot (may trigger [all] on first use).
	 */
	fun get(key: K): V? = (cached ?: all())[key]
	
	/**
	 * Clears the snapshot so the next [all] / [get] reloads. Test isolation only.
	 */
	fun reset() {
		synchronized(lock) {
			cached = null
		}
	}
}
