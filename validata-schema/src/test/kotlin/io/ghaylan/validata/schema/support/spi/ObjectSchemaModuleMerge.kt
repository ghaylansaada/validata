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
package io.ghaylan.validata.schema.support.spi

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.schema.spi.ObjectSchemaModule
import io.ghaylan.validata.schema.spi.SchemaSpiDiagnostics
import io.ghaylan.validata.schema.spi.ServiceLoaderMerge

/**
 * Merges [ObjectSchemaModule] contributions using the same conflict rules as [GeneratedSchemas],
 * without requiring `META-INF/services` registration on the test classpath.
 *
 * Keep in lockstep with production merge diagnostics via [ServiceLoaderMerge].*
 * 
 * @author Ghaylan Saada
 */
object ObjectSchemaModuleMerge {
	
	/**
	 * @param modules modules to merge in order
	 * @return unmodifiable merged map
	 * @throws IllegalStateException when two modules claim the same [Class]	 
	 */
	fun merge(vararg modules: ObjectSchemaModule): Map<Class<*>, ObjectSchema> =
		ServiceLoaderMerge.merge(
			contributions = modules.map { it.javaClass.name to it.schemas() },
			duplicateMessage = SchemaSpiDiagnostics::objectSchemaDuplicate,
		)
}
