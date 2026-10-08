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
package io.ghaylan.validata.openapi.springdoc

import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.openapi.enrichment.ConstraintGroupFilter
import io.ghaylan.validata.openapi.enrichment.OpenApiEnrichmentCache
import io.ghaylan.validata.openapi.enrichment.PropertyOpenApiExtensionsWriter
import io.ghaylan.validata.openapi.enrichment.TypeShapeOpenApiEnricher
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMappers
import io.ghaylan.validata.openapi.presentation.DeclaredErrorSurface
import io.ghaylan.validata.openapi.presentation.DeclaredErrorSurfaceFactory
import io.ghaylan.validata.openapi.presentation.ErrorDocPublishContext
import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.request.EndpointSchema
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.web.method.HandlerMethod
import kotlin.reflect.KClass

/**
 * Documents Validata query/header/path parameters and publishes per-endpoint error docs.
 *
 * Error documentation (docs only — runtime envelopes are app-owned):
 * 1. Build [DeclaredErrorSurface] from IR (validation collector ∪ baked schema docs) — **no** annotation re-scan.
 * 2. Invoke every registered [ErrorDocPublisher] with an [ErrorDocPublishContext]
 *    (default: none — no forced response schemas).
 *
 * Native facets, `x-validata-constraints`, and `x-validata-errors` on parameters respect
 * [EndpointSchema.groups] via [ConstraintGroupFilter].
 *
 * @property registry Validata registry; when null, customization is a no-op
 * @property mappers OpenAPI override SPI; defaults to [OpenApiConstraintMappers.all]
 * @property errorDocPublishers ordered app publishers (schemas, examples, extensions, …)*
 * 
 * @author Ghaylan Saada
 */
class ValidataOperationCustomizer(
	private val registry: ValidationRegistry?,
	private val mappers: List<OpenApiConstraintMapper> = OpenApiConstraintMappers.all(),
	private val errorDocPublishers: List<ErrorDocPublisher> = emptyList(),
): OperationCustomizer {
	
	/**
	 * Enriches path/header/query parameters from Validata IR and runs [errorDocPublishers].
	 *
	 * Mutates [operation] (parameters and enrichment cache) and invokes each publisher.
	 *
	 * @param operation springdoc operation being customized
	 * @param handlerMethod Spring MVC handler
	 * @return the same [operation] instance	 
	 */
	override fun customize(
		operation: Operation,
		handlerMethod: HandlerMethod
	): Operation {
		val reg = registry ?: return operation
		val endpointId = handlerMethod.method.getUniqueIdentifier()
		
		val endpoint = reg.getSchemaByRequest(endpointId) ?: return operation
		
		if (!OpenApiEnrichmentCache.markOperation(operation)) {
			return operation
		}
		
		enrichSection(
			operation = operation,
			section = endpoint.pathVariables,
			`in` = ParameterIn.PATH,
			activeGroups = endpoint.groups)
		
		enrichSection(
			operation = operation,
			section = endpoint.headers,
			`in` = ParameterIn.HEADER,
			activeGroups = endpoint.groups)
		
		enrichSection(
			operation = operation,
			section = endpoint.queryParams,
			`in` = ParameterIn.QUERY,
			activeGroups = endpoint.groups)
		
		if (errorDocPublishers.isNotEmpty()) {
			val context = ErrorDocPublishContext(
				surface = DeclaredErrorSurfaceFactory.from(endpoint),
				endpointId = endpointId,
				operation = operation,
				handlerMethod = handlerMethod,
			)
			for (publisher in errorDocPublishers) {
				publisher.publish(context)
			}
		}
		return operation
	}
	
	/**
	 * Applies IR constraints to every property in [section] that matches an OpenAPI parameter
	 * with the given `in` location.
	 *
	 * Mutates matching parameters on [operation].
	 *
	 * @param operation operation whose parameters are enriched
	 * @param section Validata IR for path, header, or query properties; ignored when `null`
	 * @param in OpenAPI parameter location
	 * @param activeGroups validation groups active for the endpoint	 
	 */
	private fun enrichSection(
		operation: Operation,
		section: ObjectSchema?,
		`in`: ParameterIn,
		activeGroups: Set<KClass<*>>,
	) {
		section ?: return
		
		for (prop in section.properties) {
			enrichParameter(
				operation = operation,
				`in` = `in`,
				prop = prop,
				activeGroups = activeGroups,
				ownerSchema = section)
		}
	}
	
	/**
	 * Overlays facets, `x-validata-constraints`, and `x-validata-errors` onto the matching OpenAPI parameter.
	 *
	 * Mutates the matched parameter schema (and `required`) on [operation].
	 *
	 * @param operation operation that owns the parameter
	 * @param in OpenAPI parameter location
	 * @param prop Validata property IR
	 * @param activeGroups validation groups active for the endpoint
	 * @param ownerSchema enclosing section schema for sibling `@PropertyRef` rewrite	 
	 */
	private fun enrichParameter(
		operation: Operation,
		`in`: ParameterIn,
		prop: PropertySpec,
		activeGroups: Set<KClass<*>>,
		ownerSchema: ObjectSchema,
	) {
		val inAsString = `in`.toString()
		val name = prop.externalName
		
		val parameter = operation.parameters?.firstOrNull {
			it.name == name && it.`in` == inAsString
		} ?: return
		
		val schema = parameter.schema
			?: Schema<Any>().also { parameter.schema = it }
		
		val markedRequired = TypeShapeOpenApiEnricher.applyAllConstraints(
			constraints = prop.constraints,
			schema = schema,
			shape = prop.shape,
			mappers = mappers,
			activeGroups = activeGroups)
		
		TypeShapeOpenApiEnricher.applyShape(
			shape = prop.shape,
			schema = schema,
			mappers = mappers,
			activeGroups = activeGroups)
		
		PropertyOpenApiExtensionsWriter.write(
			schema = schema,
			prop = prop,
			activeGroups = activeGroups,
			ownerSchema = ownerSchema)
		
		if (markedRequired) {
			parameter.required = true
		}
	}
}
