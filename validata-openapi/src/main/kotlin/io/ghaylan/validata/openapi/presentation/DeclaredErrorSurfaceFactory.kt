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
package io.ghaylan.validata.openapi.presentation

import io.ghaylan.validata.openapi.enrichment.EndpointErrorCodeCollector
import io.ghaylan.validata.openapi.enrichment.SchemaErrorDocResolver
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.*

/**
 * Builds a [DeclaredErrorSurface] from an [EndpointSchema] without re-scanning source annotations.
 *
 * Merge:
 * - detail codes ← [EndpointErrorCodeCollector] ∪ property [PropertySpec.errorDocs]
 * - detail docs ← property schema docs (with [PublishedErrorDoc.path] / [PublishedErrorDoc.location]
 *   from wire names and the request section)
 *
 * Nested object / list / map shapes are walked. Paths use wire [PropertySpec.externalName]
 * segments joined by `.`; collection elements append `[]` (e.g. `items[].sku`). Cycles are
 * skipped via visited object types.*
 * 
 * @author Ghaylan Saada
 */
object DeclaredErrorSurfaceFactory {
	
	/**
	 * Builds the OpenAPI error surface for [endpoint].
	 *
	 * @param endpoint Validata request schema for one handler
	 * @return deterministic, sorted surface	 
	 */
	fun from(endpoint: EndpointSchema): DeclaredErrorSurface {
		val detailCodes = sortedSetOf<String>()
		detailCodes += EndpointErrorCodeCollector.collect(endpoint)
		val detailDocs = ArrayList<PublishedErrorDoc>()
		
		collectSectionDocs(endpoint.pathVariables, EndpointArgumentKind.PATH, detailCodes, detailDocs)
		collectSectionDocs(endpoint.headers, EndpointArgumentKind.HEADER, detailCodes, detailDocs)
		collectSectionDocs(endpoint.queryParams, EndpointArgumentKind.QUERY, detailCodes, detailDocs)
		collectSectionDocs(endpoint.requestBody, EndpointArgumentKind.BODY, detailCodes, detailDocs)
		
		return DeclaredErrorSurface(
			detailCodes = detailCodes.toList(),
			detailErrorDocs = detailDocs.distinctBy { listOf(it.location, it.path, it.code, it.message) }
				.sortedWith(compareBy(
					{ it.location?.name },
					{ it.path },
					{ it.code },
					{ it.message },
				)),
		)
	}
	
	private fun collectSectionDocs(
		section: ObjectSchema?,
		location: EndpointArgumentKind,
		detailCodes: MutableSet<String>,
		detailDocs: MutableList<PublishedErrorDoc>,
	) {
		if (section == null) return
		walkObject(
			schema = section,
			location = location,
			pathPrefix = "",
			detailCodes = detailCodes,
			detailDocs = detailDocs,
			ancestors = HashSet())
	}
	
	private fun walkObject(
		schema: ObjectSchema,
		location: EndpointArgumentKind,
		pathPrefix: String,
		detailCodes: MutableSet<String>,
		detailDocs: MutableList<PublishedErrorDoc>,
		ancestors: MutableSet<Class<*>>,
	) {
		if (!ancestors.add(schema.type)) return
		try {
			for (prop in schema.properties) {
				val path = joinPath(pathPrefix, prop.externalName)
				for (doc in prop.errorDocs) {
					val (code, message) = SchemaErrorDocResolver.resolve(doc)
					detailCodes += code
					detailDocs += PublishedErrorDoc(
						code = code,
						message = message,
						path = path,
						location = location)
				}
				walkShape(
					shape = prop.shape,
					location = location,
					pathPrefix = path,
					detailCodes = detailCodes,
					detailDocs = detailDocs,
					ancestors = ancestors)
			}
			for (sub in schema.subtypes.values) {
				walkObject(
					schema = sub,
					location = location,
					pathPrefix = pathPrefix,
					detailCodes = detailCodes,
					detailDocs = detailDocs,
					ancestors = ancestors)
			}
		}
		finally {
			ancestors.remove(schema.type)
		}
	}
	
	private fun walkShape(
		shape: TypeShape,
		location: EndpointArgumentKind,
		pathPrefix: String,
		detailCodes: MutableSet<String>,
		detailDocs: MutableList<PublishedErrorDoc>,
		ancestors: MutableSet<Class<*>>,
	) {
		when (shape) {
			is ScalarShape,
			is DynamicShape -> Unit
			
			is ObjectRefShape -> walkObject(
				schema = shape.ref.value,
				location = location,
				pathPrefix = pathPrefix,
				detailCodes = detailCodes,
				detailDocs = detailDocs,
				ancestors = ancestors)
			
			is IterableShape -> walkShape(
				shape = shape.element,
				location = location,
				pathPrefix = "$pathPrefix[]",
				detailCodes = detailCodes,
				detailDocs = detailDocs,
				ancestors = ancestors)
			
			is MapShape -> walkShape(
				shape = shape.value,
				location = location,
				pathPrefix = joinPath(pathPrefix, "*"),
				detailCodes = detailCodes,
				detailDocs = detailDocs,
				ancestors = ancestors)
		}
	}
	
	private fun joinPath(
		prefix: String,
		segment: String
	): String = if (prefix.isEmpty()) segment else "$prefix.$segment"
}
