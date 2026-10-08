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
package io.ghaylan.validata.openapi.mapper

import java.util.*

/**
 * Discovers [OpenApiConstraintMapper] implementations via [ServiceLoader].
 *
 * Built-ins (if any) and app mappers are loaded once from ServiceLoader
 * (`META-INF/services/` + [OpenApiConstraintMapper]'s binary name).*
 * 
 * @author Ghaylan Saada
 */
object OpenApiConstraintMappers {
	
	/**
	 * Cached ServiceLoader snapshot; `null` means not loaded yet.
	 */
	@Volatile
	private var cached: List<OpenApiConstraintMapper>? = null
	
	/**
	 * Returns every mapper on the classpath (built-ins + app SPI), loading and caching on first call.
	 *
	 * @return immutable snapshot of discovered mappers	 
	 */
	fun all(): List<OpenApiConstraintMapper> {
		cached?.let { return it }
		return synchronized(this) {
			cached ?: load().also { cached = it }
		}
	}
	
	/**
	 * Clears the cache so tests can reload after classpath or ServiceLoader changes.
	 */
	fun resetForTests() {
		synchronized(this) {
			cached = null
		}
	}
	
	/**
	 * Loads mappers from ServiceLoader in provider-file order.
	 *
	 * @return newly loaded snapshot (not yet cached)	 
	 */
	private fun load(): List<OpenApiConstraintMapper> {
		val loader = OpenApiConstraintMapper::class.java.classLoader
		return ServiceLoader.load(OpenApiConstraintMapper::class.java, loader)
			.toList()
	}
}
