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

import io.ghaylan.validata.openapi.config.ValidataOpenApiAutoConfiguration
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import java.util.*

/**
 * Tracks OpenAPI [Schema] / [Operation] instances that have already been enriched so springdoc's
 * repeated converter/customizer calls do not re-apply mappers.
 *
 * Uses a generation counter plus identity maps. [clear] bumps the generation and drops identity
 * entries so sequential `/v3/api-docs` rebuilds re-enrich, and so schema instances are not retained
 * across builds. Concurrent document generation is not supported — callers should serialize OpenAPI
 * builds or accept possible double-enrichment / skipped marks.
 *
 * Cleared after each OpenAPI document build via autoconfig [ValidataOpenApiAutoConfiguration].*
 * 
 * @author Ghaylan Saada
 */
internal object OpenApiEnrichmentCache {
	
	
	private val lock = Any()
	
	/**
	 * Current enrichment generation; bumped on every [clear].
	 */
	private var generation: Long = 0L
	
	/**
	 * Identity → generation last marked for schemas.
	 */
	private val schemas = IdentityHashMap<Any, Long>()
	
	/**
	 * Identity → generation last marked for operations.
	 */
	private val operations = IdentityHashMap<Any, Long>()
	
	/**
	 * Marks [schema] as enriched for the current generation.
	 *
	 * @param schema identity-tracked schema instance
	 * @return `true` if this is the first mark in the current generation (caller should enrich)	 
	 */
	fun markSchema(schema: Any): Boolean = mark(schemas, schema)
	
	/**
	 * Marks [operation] as enriched for the current generation.
	 *
	 * @param operation identity-tracked operation instance
	 * @return `true` if this is the first mark in the current generation (caller should enrich)	 
	 */
	fun markOperation(operation: Any): Boolean = mark(operations, operation)
	
	/**
	 * Bumps the generation and clears identity maps so the next OpenAPI generation starts fresh.
	 *
	 * Called after each document build; also used by unit tests.	 
	 */
	fun clear() {
		synchronized(lock) {
			generation++
			schemas.clear()
			operations.clear()
		}
	}
	
	/**
	 * Clears tracking so unit tests can assert enrichment behavior in isolation.
	 *
	 * Test-only alias of [clear].	 
	 */
	fun resetForTests() = clear()
	
	private fun mark(
		map: IdentityHashMap<Any, Long>,
		key: Any
	): Boolean = synchronized(lock) {
		val gen = generation
		val previous = map.put(key, gen)
		previous != gen
	}
}
