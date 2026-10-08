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
package io.ghaylan.validata.bootstrap

import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas
import org.springframework.context.ApplicationContext

/**
 * Resolves pre-built [EndpointSchema] graphs for `@Validate` endpoints.
 *
 * KSP builds schema content via [GeneratedRequestSchemas]. Spring discovery only maps a live bean
 * method to that pre-built key via [getUniqueIdentifier].
 *
 * Discovering which handlers exist is a runtime concern
 * ([ValidateHandlerDiscovery.findRequestValidationMethods]); endpoint meaning is compile-time.*
 * 
 * @author Ghaylan Saada
 */
internal object EndpointSchemaIndex {
	
	/**
	 * Looks up generated schemas for every `@Validate` handler found in [appContext].
	 *
	 * Missing generated entries fail loudly at startup so a controller that the interceptor will
	 * try to validate cannot silently run without rules.
	 *
	 * @param appContext live Spring context (used only to discover handler methods)
	 * @return map of endpoint id → schema (same keys the servlet handler uses)
	 * @throws IllegalStateException when a discovered handler has no generated schema, or when two
	 *   handlers produce the same endpoint id	 
	 */
	fun resolveStaticSchemas(appContext: ApplicationContext): Map<String, EndpointSchema> {
		val generated = GeneratedRequestSchemas.all()
		val methods = ValidateHandlerDiscovery.findRequestValidationMethods(appContext)
		val result = LinkedHashMap<String, EndpointSchema>(methods.size)
		for ((method, _) in methods) {
			val id = method.getUniqueIdentifier()
			val schema = generated[id]
				?: error("No generated EndpointSchema for endpoint '$id' " + "(${method.declaringClass.name}#${method.name}). " + "Apply the validata-processor KSP dependency to the module that declares " + "this @Validate handler.")
			val previous = result.put(id, schema)
			if (previous != null) {
				error("Duplicate endpoint identifier '$id' while registering request schemas. Two handler methods produced the same key — fix overloads or parameter types.")
			}
		}
		return result
	}
}
