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

import io.ghaylan.validata.schema.request.spi.RequestSchemaModule

/**
 * Shared merge-conflict diagnostics for classpath schema aggregators and test helpers.
 *
 * Keep message text stable — processor / IDE tests assert on these strings.*
 * 
 * @author Ghaylan Saada
 */
internal object SchemaSpiDiagnostics {
	
	/**
	 * Duplicate [ObjectSchemaModule] contribution for the same runtime class.
	 */
	fun objectSchemaDuplicate(
		cls: Class<*>,
		first: String,
		second: String
	): String = "Duplicate generated validation schema for ${cls.name}. Contributed by both $first and $second."
	
	/**
	 * Duplicate [RequestSchemaModule] contribution for the  same endpoint id.
	 */
	fun requestSchemaDuplicate(
		endpointId: String,
		first: String,
		second: String
	): String = "Duplicate generated request schema for endpoint '$endpointId'. Contributed by both $first and $second."
}
