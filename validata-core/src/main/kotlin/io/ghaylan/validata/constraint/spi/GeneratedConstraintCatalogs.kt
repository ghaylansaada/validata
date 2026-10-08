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
package io.ghaylan.validata.constraint.spi

import io.ghaylan.validata.constraint.spi.GeneratedConstraintCatalogs.cached
import io.ghaylan.validata.constraint.spi.GeneratedConstraintCatalogs.load
import io.ghaylan.validata.internal.TypeInfo
import java.util.*

/**
 * Merges every [ConstraintCatalog] on the classpath via [ServiceLoader].
 *
 * This is the only place the framework looks for constraint bindings — [ServiceLoader] reads the
 * list of implementation names each module's KSP run already wrote. There is no classpath scanning
 * and no `Class.forName` of annotation packages.
 *
 * Cross-module duplicate `(metadataType, valueType)` pairs are rejected loudly at load time so
 * unspecified ServiceLoader order cannot silently pick a winner.
 *
 * ### Example
 *
 * ```kotlin
 * // Host startup: flatten catalogs into the engine's validator map
 * val entries = GeneratedConstraintCatalogs.all()
 * val byMetadata = entries.groupBy { it.metadataType.kotlin }
 *   .mapValues { (_, list) ->
 *     list.associate { it.valueType to it.defaultInstanceFactory() }
 *   }
 * registry.registerValidators(byMetadata)
 * ```
 *
 * Call [all] once at host startup before serving traffic (Spring hosts do this in
 * `ValidationRegistryInitializer.afterPropertiesSet`). Use [resetForTests] only in tests that swap
 * classloaders or SPI resources.*
 * 
 * @author Ghaylan Saada
 */
object GeneratedConstraintCatalogs {
	
	/**
	 * Cached flattened catalog snapshot; `null` until [all] loads successfully.
	 */
	@Volatile
	private var cached: List<ConstraintCatalogEntry>? = null
	
	/**
	 * Private monitor for load / [resetForTests]. Avoids contending on the public object monitor.
	 */
	private val lock = Any()
	
	/**
	 * Every catalog entry discovered on the classpath.
	 *
	 * Loads once via [ServiceLoader] and caches the snapshot; subsequent calls reuse it.
	 * No I/O after the first successful load.
	 *
	 * @return Flattened, collision-checked list of [ConstraintCatalogEntry] from every SPI catalog.
	 * @throws IllegalStateException when two catalogs claim the same metadata + value type pair.	 
	 */
	fun all(): List<ConstraintCatalogEntry> {
		cached?.let { return it }
		return synchronized(lock) {
			cached ?: load().also { cached = it }
		}
	}
	
	/**
	 * Clears the cache so tests can reload after classpath / classloader changes.
	 *
	 * Mutates [cached] under lock. Not intended for production use.	 
	 */
	fun resetForTests() {
		synchronized(lock) {
			cached = null
		}
	}
	
	/**
	 * Discovers every [ConstraintCatalog] and flattens their entries.
	 *
	 * Side effect: loads SPI implementations via [ServiceLoader]. Fails on cross-module collisions
	 * for the same metadata + value type.
	 *
	 * @return Merged catalog entries in discovery order.
	 * @throws IllegalStateException when two catalogs claim the same metadata + value type pair.	 
	 */
	private fun load(): List<ConstraintCatalogEntry> {
		val merged = ArrayList<ConstraintCatalogEntry>()
		val seen = HashMap<Pair<Class<*>, String>, String>()
		val loader = ConstraintCatalog::class.java.classLoader
		ServiceLoader.load(ConstraintCatalog::class.java, loader)
			.forEach { catalog ->
				for (entry in catalog.entries()) {
					val key = entry.metadataType to typeKey(entry.valueType)
					val previousOwner = seen.put(key, catalog.javaClass.name)
					if (previousOwner != null) {
						error("Duplicate constraint catalog binding for metadata=${entry.metadataType.name} " +
								"valueType=${entry.valueType.concreteType.qualifiedName} " +
								"(validators claimed by both $previousOwner and ${catalog.javaClass.name}).")
					}
					merged += entry
				}
			}
		return merged
	}
	
	/**
	 * Stable string key for [TypeInfo] identity in the duplicate check.
	 *
	 * No side effects. Uses concrete class name plus recursive type-argument keys.
	 *
	 * @param info Type to key.
	 * @return Deterministic identity string for collision maps in [load].	 
	 */
	private fun typeKey(info: TypeInfo): String {
		val args = info.typeArguments.joinToString(",") { typeKey(it) }
		return "${info.concreteType.java.name}<$args>"
	}
}
