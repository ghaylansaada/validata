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
package io.ghaylan.validata.schema.request.spi

import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.spi.ClasspathSchemaCache
import io.ghaylan.validata.schema.spi.SchemaSpiDiagnostics
import io.ghaylan.validata.schema.spi.ServiceLoaderMerge

/**
 * Classpath aggregator for [RequestSchemaModule] contributions (ServiceLoader, lazy classloader-lifetime cache).
 *
 * Duplicate endpoint ids fail with both contributors named.
 * Production code never resets the cache; [resetForTests] exists only for test isolation
 * (also called from dependent modules' tests — keep public).*
 * 
 * @author Ghaylan Saada
 */
object GeneratedRequestSchemas {
	
	private val cache = ClasspathSchemaCache {
		ServiceLoaderMerge.load(
			moduleType = RequestSchemaModule::class.java,
			schemas = RequestSchemaModule::schemas,
			duplicateMessage = SchemaSpiDiagnostics::requestSchemaDuplicate)
	}
	
	/**
	 * Schema for [endpointId], or `null` when none was contributed.
	 *
	 * May populate the cache via [all] on first use.
	 *
	 * @param endpointId identifier matching `Method.getUniqueIdentifier()`
	 * @return matching [EndpointSchema], or `null`	 
	 */
	fun get(endpointId: String): EndpointSchema? = cache.get(endpointId)
	
	/**
	 * All merged generated endpoint schemas (ServiceLoader once; later calls reuse the unmodifiable snapshot).
	 *
	 * @return immutable endpoint-id → schema map
	 * @throws IllegalStateException when two modules contribute the same endpoint id	 
	 */
	fun all(): Map<String, EndpointSchema> = cache.all()
	
	/**
	 * Clears the cache so tests can reload after classpath / classloader changes.
	 *
	 * Not for production use.	 
	 */
	fun resetForTests() {
		cache.reset()
	}
}
