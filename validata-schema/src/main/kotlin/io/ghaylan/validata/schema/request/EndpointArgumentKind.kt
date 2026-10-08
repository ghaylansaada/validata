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
package io.ghaylan.validata.schema.request

/**
 * How one handler method parameter participates in request validation.
 *
 * Pre-computed into [EndpointSchema.argumentLayout] so the servlet interceptor does not re-scan
 * parameter annotations per request. Constant names mirror Spring annotations; this enum does not
 * import Spring types.
 * 
 * @author Ghaylan Saada
 */
enum class EndpointArgumentKind {
	
	/**
	 * `@RequestBody` parameter; at most one per method.
	 */
	BODY,
	
	/**
	 * `@RequestParam` parameter; value keyed by [EndpointArgumentSlot.name] in the query map.
	 */
	QUERY,
	
	/**
	 * `@RequestHeader` parameter; value keyed by [EndpointArgumentSlot.name] in the header map.
	 */
	HEADER,
	
	/**
	 * `@PathVariable` parameter; value keyed by [EndpointArgumentSlot.name] in the path map.
	 */
	PATH,
	
	/**
	 * Non-transport parameter (e.g. `HttpServletRequest`, custom resolvers).
	 *
	 * Keeps layout indices aligned with the runtime `args` array; ignored by validation.
	 */
	OTHER,
}
