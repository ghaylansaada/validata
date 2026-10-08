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

import io.ghaylan.validata.schema.ObjectSchema
import java.util.*

/**
 * Classpath aggregator for [ObjectSchemaModule] contributions discovered through [ServiceLoader].
 *
 * Schemas are discovered once and retained as an immutable snapshot for the lifetime of the cache.
 * Production code never resets the cache; [resetForTests] exists only for test isolation
 * (also called from dependent modules' tests — keep public).
 *
 * Duplicate schemas for the same [Class] fail with both contributors named.
 * This is not a Spring bean lookup.*
 * 
 * @author Ghaylan Saada
 */
object GeneratedSchemas {
	
	private val cache = ClasspathSchemaCache {
		ServiceLoaderMerge.load(
			moduleType = ObjectSchemaModule::class.java,
			schemas = ObjectSchemaModule::schemas,
			duplicateMessage = SchemaSpiDiagnostics::objectSchemaDuplicate)
	}
	
	/**
	 * Returns the generated schema for [type], or `null` when none was contributed.
	 *
	 * The first call performs classpath discovery through [ServiceLoader].
	 *
	 * @param type runtime class to look up
	 * @return matching [ObjectSchema], or `null`	 
	 */
	fun get(type: Class<*>): ObjectSchema? = cache.get(type)
	
	/**
	 * Returns the immutable snapshot of all generated schemas.
	 *
	 * The classpath is scanned only once until [resetForTests] is called.
	 *
	 * @return immutable class-to-schema map
	 * @throws IllegalStateException when multiple modules contribute the same [Class]	 
	 */
	fun all(): Map<Class<*>, ObjectSchema> = cache.all()
	
	/**
	 * Clears the cached schema snapshot.
	 *
	 * Test-only operation for reloading schemas after classpath or classloader changes.
	 * Not for production use.	 
	 */
	fun resetForTests() {
		cache.reset()
	}
}
