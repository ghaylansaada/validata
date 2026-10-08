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

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.openapi.enrichment.EndpointErrorCodeCollector.cache
import io.ghaylan.validata.openapi.enrichment.EndpointErrorCodeCollector.compute
import io.ghaylan.validata.openapi.enrichment.EndpointErrorCodeCollector.warnEmptyOnce
import io.ghaylan.validata.openapi.enrichment.EndpointErrorCodeCollector.warnedValidators
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Collects machine-readable error codes reachable from an [EndpointSchema] for OpenAPI docs.
 *
 * Structural codes always included for any validated endpoint:
 * - [ConstraintErrorCode.VALUE_TYPE_MISMATCH] — Jackson / conversion failures
 * - [ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED] — depth limit violations
 *
 * Plus [ConstraintErrorCode.VALUE_MISSING] when any active `@Required` is present.
 *
 * Constraint codes come from validators via [ConstraintErrorCodeWalk], filtered by
 * [EndpointSchema.groups]. Validators with an empty set are logged once.
 *
 * Results are cached by endpoint id plus the identity of the active group set so repeated
 * OperationCustomizer calls do not re-walk IR.*
 * 
 * @author Ghaylan Saada
 */
object EndpointErrorCodeCollector {
	
	/**
	 * SLF4J logger for empty-`possibleErrorCodes` warnings.
	 */
	private val log = LoggerFactory.getLogger(EndpointErrorCodeCollector::class.java)
	
	/**
	 * Validator classes already warned for empty `possibleErrorCodes`.
	 */
	private val warnedValidators = ConcurrentHashMap.newKeySet<Class<*>>()
	
	/**
	 * Process-lifetime cache keyed by endpoint id + active groups.
	 * Cleared after each OpenAPI document build via autoconfig.	 
	 */
	private val cache = ConcurrentHashMap<CacheKey, Set<String>>()
	
	/**
	 * Returns all machine codes reachable from [endpoint], using a process-wide cache.
	 *
	 * Writes a miss into [cache]. May log once per validator class with empty `possibleErrorCodes`.
	 *
	 * @param endpoint Validata request schema for one handler
	 * @return immutable set of code names (enum names preferred)	 
	 */
	fun collect(endpoint: EndpointSchema): Set<String> {
		val key = CacheKey(endpoint.id, endpoint.groups)
		cache[key]?.let { return it }
		val computed = compute(endpoint)
		return cache.putIfAbsent(key, computed) ?: computed
	}
	
	/**
	 * Clears [cache] (and optionally warning state) so OpenAPI regenerations and tests stay bounded.
	 *
	 * @param clearWarnings when `true`, also clears [warnedValidators] (tests); production clears
	 *   keep one-time warning state	 
	 */
	fun clear(clearWarnings: Boolean = false) {
		cache.clear()
		if (clearWarnings) warnedValidators.clear()
	}
	
	/**
	 * Clears [cache] and [warnedValidators] so tests can assert warning / compute behavior in isolation.
	 *
	 * Test-only alias of [clear] with warnings reset.	 
	 */
	fun resetForTests() = clear(clearWarnings = true)
	
	/**
	 * Walks [endpoint] IR and builds the immutable code set for one cache miss.
	 *
	 * May log via [warnEmptyOnce] for validators with empty `possibleErrorCodes`.
	 *
	 * @param endpoint Validata request schema for one handler
	 * @return set of structural, required, and constraint code names
	 */
	private fun compute(endpoint: EndpointSchema): Set<String> {
		val codes = linkedSetOf<String>()
		codes += ConstraintErrorCode.VALUE_TYPE_MISMATCH.code
		codes += ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED.code
		
		val state = CollectState(endpoint.groups)
		val visited = HashSet<Class<*>>()
		
		walkObject(endpoint.pathVariables, state, visited)
		walkObject(endpoint.headers, state, visited)
		walkObject(endpoint.queryParams, state, visited)
		walkObject(endpoint.requestBody, state, visited)
		
		codes += state.codes
		
		if (state.hasRequired) {
			codes += ConstraintErrorCode.VALUE_MISSING.code
		}
		
		return codes
	}
	
	/**
	 * Collects codes from [schema] properties and subtypes, skipping already-[visited] types.
	 *
	 * Mutates [state] and [visited].
	 *
	 * @param schema object IR section, or `null` to skip
	 * @param state accumulator for codes and required presence
	 * @param visited object types already walked (cycle guard)	 
	 */
	private fun walkObject(
		schema: ObjectSchema?,
		state: CollectState,
		visited: MutableSet<Class<*>>
	) {
		if (schema == null) return
		if (!visited.add(schema.type)) return
		
		for (property in schema.properties) {
			val subjectType = TypeShapeJavaTypes.resolve(property.shape)
			ConstraintErrorCodeWalk.forEachActive(
				type = subjectType,
				constraints = property.constraints,
				activeGroups = state.activeGroups,
				onRequired = { state.hasRequired = true },
				onDefinition = { def -> state.codes += def.code },
				onEmptyValidator = ::warnEmptyOnce)
			walkShape(property.shape, state, visited)
		}
		
		for (sub in schema.subtypes.values) {
			walkObject(sub, state, visited)
		}
	}
	
	/**
	 * Collects codes from [shape] constraints and nested list/map/object shapes.
	 *
	 * Mutates [state] and [visited].
	 *
	 * @param shape type-use IR node
	 * @param state accumulator for codes and required presence
	 * @param visited object types already walked (cycle guard)	 
	 */
	private fun walkShape(
		shape: TypeShape,
		state: CollectState,
		visited: MutableSet<Class<*>>
	) {
		ConstraintErrorCodeWalk.forEachActive(
			type = TypeShapeJavaTypes.resolve(shape),
			constraints = shape.constraints,
			activeGroups = state.activeGroups,
			onRequired = { state.hasRequired = true },
			onDefinition = { def -> state.codes += def.code },
			onEmptyValidator = ::warnEmptyOnce)
		
		when (shape) {
			is ScalarShape -> Unit
			is DynamicShape -> Unit
			is ObjectRefShape -> walkObject(shape.ref.value, state, visited)
			is IterableShape -> {
				walkShape(shape.element, state, visited)
			}
			is MapShape -> {
				walkShape(shape.key, state, visited)
				walkShape(shape.value, state, visited)
			}
		}
	}
	
	/**
	 * Logs once per validator class when `possibleErrorCodes` is empty.
	 *
	 * Mutates [warnedValidators]; may write a WARN log line.
	 *
	 * @param validator validator whose code set was empty	 
	 */
	private fun warnEmptyOnce(validator: ConstraintValidator<*, *>) {
		val type = validator.javaClass
		if (!warnedValidators.add(type)) return
		log.warn("Validator {} has empty possibleErrorCodes; OpenAPI will omit its field codes. Override possibleErrorCodes on the validator.", type.name)
	}
	
	/**
	 * Cache key for one endpoint id under a fixed active group set.
	 *
	 * @property endpointId Validata endpoint identifier
	 * @property groups validation groups active for that endpoint
	 */
	private data class CacheKey(
		val endpointId: String,
		val groups: Set<KClass<*>>)
	
	/**
	 * Mutable walk state for one [compute] pass.
	 *
	 * @property activeGroups groups used by [ConstraintGroupFilter]
	 * @property codes constraint code names collected so far
	 * @property hasRequired whether any active `@Required` was seen
	 */
	private class CollectState(
		val activeGroups: Set<KClass<*>>,
		val codes: MutableSet<String> = linkedSetOf(),
		var hasRequired: Boolean = false)
}
