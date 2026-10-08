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
package io.ghaylan.validata.web

import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.request.EndpointArgumentSlot

/**
 * Zips Spring's resolved argument array against a compile-time [EndpointArgumentSlot] layout into
 * the maps / body instance [ValidatorEngine] expects.
 *
 * Extracted from the invocable so packing rules are unit-testable without standing up Tomcat, and
 * so "allocate maps only when needed" policy lives in one place.
 *
 * ## Allocation policy
 *
 * - QUERY / HEADER / PATH maps are created **lazily** on the first slot of that kind, and only when
 *   the corresponding [HandlerValidationPlan.Active] flag is `true`.
 * - Body-only endpoints therefore perform zero [HashMap] allocations in this assembler.
 * - Multiple BODY slots are not expected; the first non-null body wins.
 *
 * ## Thread safety
 *
 * Stateless; safe to call concurrently with distinct argument arrays.*
 * 
 * @author Ghaylan Saada
 */
internal object RequestArgumentAssembler {
	
	/**
	 * Builds [AssembledRequestArguments] for one invocation.
	 *
	 * @param layout compile-time slots aligned with [args] indices
	 * @param args Spring-resolved method arguments in declaration order
	 * @param plan active validation plan whose section flags gate map allocation
	 * @return body + optional section maps (`null` maps mean "section unused")
	 * @throws IllegalArgumentException when [layout] and [args] lengths differ	 
	 */
	fun assemble(
		layout: List<EndpointArgumentSlot>,
		args: Array<Any?>,
		plan: HandlerValidationPlan.Active,
	): AssembledRequestArguments {
		require(layout.size == args.size) {
			"Argument layout size ${layout.size} does not match resolved args ${args.size} " + "for '${plan.endpointId}'. Rebuild so KSP regenerates the endpoint schema " + "(overloads/varargs must stay in sync)."
		}
		var requestBody: Any? = null
		var queryParams: MutableMap<String, Any?>? = null
		var headers: MutableMap<String, Any?>? = null
		var pathVariables: MutableMap<String, Any?>? = null
		
		for (index in layout.indices) {
			val slot = layout[index]
			val value = args[index]
			when (slot.kind) {
				EndpointArgumentKind.BODY -> if (requestBody == null) requestBody = value
				
				EndpointArgumentKind.QUERY -> if (plan.needsQuery) {
					queryParams = put(queryParams, slot.name, value)
				}
				
				EndpointArgumentKind.HEADER -> if (plan.needsHeader) {
					headers = put(headers, slot.name, value)
				}
				
				EndpointArgumentKind.PATH -> if (plan.needsPath) {
					pathVariables = put(pathVariables, slot.name, value)
				}
				
				EndpointArgumentKind.OTHER -> Unit
			}
		}
		
		return AssembledRequestArguments(
			body = requestBody,
			headers = headers,
			queryParams = queryParams,
			pathVariables = pathVariables)
	}
	
	/**
	 * Puts [value] under [name], allocating a [LinkedHashMap] on first use so insertion order
	 * matches layout order (stable for debugging; cheap at section sizes typical of HTTP APIs).
	 *
	 * @param map existing section map, or null before the first slot of this kind
	 * @param name transport parameter / header / path variable name
	 * @param value resolved argument value (may be null)
	 * @return the map that now contains [name]	 
	 */
	private fun put(
		map: MutableMap<String, Any?>?,
		name: String,
		value: Any?,
	): MutableMap<String, Any?> {
		val target = map ?: LinkedHashMap()
		target[name] = value
		return target
	}
}
