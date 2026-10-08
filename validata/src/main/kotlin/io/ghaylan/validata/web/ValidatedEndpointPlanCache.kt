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

import io.ghaylan.validata.bootstrap.ValidateHandlerDiscovery
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.request.EndpointSchema
import org.springframework.web.method.HandlerMethod
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide cache of [HandlerValidationPlan] keyed by bridged handler [Method].
 *
 * ## Why this exists
 *
 * Spring MVC builds a fresh [ValidatingServletInvocableHandlerMethod] for every request. Without a
 * shared cache, each request would re-run [ValidateHandlerDiscovery.findValidate], recompute the
 * endpoint id, and re-resolve the [EndpointSchema] — pure overhead on top of the engine. This
 * cache makes that work **once per handler method**.
 *
 * ## Lifecycle
 *
 * Entries are never evicted: handler methods are fixed for the lifetime of the application context.
 * A missing schema for an annotated handler fails loudly on first resolve (misconfiguration), and
 * the failed compute is not cached by [ConcurrentHashMap.computeIfAbsent] when the lambda throws.
 *
 * ## Thread safety
 *
 * Safe for concurrent first-hit resolution from multiple Tomcat workers.
 *
 * @property validationRegistry Source of generated [EndpointSchema]s.*
 * 
 * @author Ghaylan Saada
 */
class ValidatedEndpointPlanCache(
	private val validationRegistry: ValidationRegistry,
) {

	/**
	 * Bridged handler method → resolved plan; never evicted for the context lifetime.
	 */
	private val plansByMethod = ConcurrentHashMap<Method, HandlerValidationPlan>()

	
	/**
	 * Returns the cached plan for [handlerMethod], computing and storing it on first call.
	 *
	 * @param handlerMethod Spring handler (annotation scan uses method + bean type)
	 * @param bridgedMethod user-defined method ([HandlerMethod] exposes this only to subclasses via
	 *   protected `getBridgedMethod()`; the invocable passes it explicitly)
	 * @return [HandlerValidationPlan.Skip] or [HandlerValidationPlan.Active]
	 * @throws IllegalStateException when an annotated handler with parameters has no generated
	 *   [EndpointSchema] in [validationRegistry]
	 */
	internal fun planFor(handlerMethod: HandlerMethod, bridgedMethod: Method): HandlerValidationPlan {
		return plansByMethod.computeIfAbsent(bridgedMethod) {
			buildPlan(handlerMethod, bridgedMethod)
		}
	}

	/**
	 * Test / diagnostics hook: number of cached methods (including [HandlerValidationPlan.Skip]).
	 *
	 * @return current cache size
	 */
	internal fun cachedMethodCount(): Int = plansByMethod.size

	/**
	 * Clears every cached plan so tests can re-resolve after registry changes.
	 */
	internal fun clearForTests() {
		plansByMethod.clear()
	}

	/**
	 * Builds a plan for an annotated handler, or [HandlerValidationPlan.Skip] when not applicable.
	 *
	 * @param handlerMethod Spring handler used for `@Validate` discovery
	 * @param bridged user-defined method whose id keys the registry schema
	 * @return skip, or an active plan with section flags derived from the schema layout
	 * @throws IllegalStateException when the handler is annotated, has parameters, and no schema exists
	 */
	private fun buildPlan(handlerMethod: HandlerMethod, bridged: Method): HandlerValidationPlan {
		if (ValidateHandlerDiscovery.findValidate(handlerMethod) == null) {
			return HandlerValidationPlan.Skip
		}
		if (bridged.parameterCount == 0) {
			return HandlerValidationPlan.Skip
		}

		val endpointId = bridged.getUniqueIdentifier()
		val schema = validationRegistry.getSchemaByRequest(endpointId)
			?: error("No generated EndpointSchema for endpoint '$endpointId' " +
					"(${bridged.declaringClass.name}#${bridged.name}). " +
					"Apply the validata-processor KSP dependency to the module that declares " +
					"this @Validate handler.")

		val layout = schema.argumentLayout
		// Single pass over layout (was 3× any{} = up to 3n scans) — first-hit plan build only.
		var needsQuery = false
		var needsHeader = false
		var needsPath = false
		
		for ((kind) in layout) {
			when (kind) {
				EndpointArgumentKind.QUERY -> needsQuery = true
				EndpointArgumentKind.HEADER -> needsHeader = true
				EndpointArgumentKind.PATH -> needsPath = true
				else -> Unit
			}
			if (needsQuery && needsHeader && needsPath) break
		}
		
		val schemaWhenBodyAbsent =
			if (schema.requestBody != null) schema.copy(requestBody = null) else schema

		return HandlerValidationPlan.Active(
			endpointId = endpointId,
			schema = schema,
			schemaWhenBodyAbsent = schemaWhenBodyAbsent,
			needsQuery = needsQuery,
			needsHeader = needsHeader,
			needsPath = needsPath)
	}
}
