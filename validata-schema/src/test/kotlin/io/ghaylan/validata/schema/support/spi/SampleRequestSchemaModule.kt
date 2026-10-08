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

import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas
import io.ghaylan.validata.schema.request.spi.RequestSchemaModule
import io.ghaylan.validata.schema.support.request.SampleRequestSchemas

/**
 * Test-classpath [RequestSchemaModule] registered via `META-INF/services` so
 * [GeneratedRequestSchemas] exercises the real
 * [java.util.ServiceLoader] path.
 *
 * Contributes [ENDPOINT_ID] only. See `src/test/resources/META-INF/services/…RequestSchemaModule`.
 * 
 * @author Ghaylan Saada
 */
class SampleRequestSchemaModule: RequestSchemaModule {
	
	override fun schemas(): Map<String, EndpointSchema> =
		mapOf(ENDPOINT_ID to SampleRequestSchemas.of(ENDPOINT_ID))
	
	companion object {
		
		/**
		 * Stable endpoint id contributed by this module.
		 */
		const val ENDPOINT_ID: String = "io.ghaylan.validata.schema.support.spi.Sample#probe()"
	}
}
