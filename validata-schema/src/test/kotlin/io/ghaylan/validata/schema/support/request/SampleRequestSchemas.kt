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
package io.ghaylan.validata.schema.support.request

import io.ghaylan.validata.schema.request.EndpointSchema

/**
 * Minimal [EndpointSchema] factories for endpoint / request-SPI tests.
 *
 * Uses production constructor defaults so tests do not invent different fail-fast flags.
 * All transport sections omitted by default so SPI/cache tests stay independent of body/query IR.*
 * 
 * @author Ghaylan Saada
 */
object SampleRequestSchemas {
	
	/**
	 * Builds an [EndpointSchema] with no transport sections and production defaults.
	 *
	 * @param id endpoint identifier key	 
	 */
	fun of(id: String = "test.Endpoint#handler()"): EndpointSchema =
		EndpointSchema(id = id, groups = emptySet())
}
