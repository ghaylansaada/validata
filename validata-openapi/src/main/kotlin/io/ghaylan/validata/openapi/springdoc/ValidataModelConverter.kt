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

import io.ghaylan.validata.openapi.enrichment.ObjectSchemaOpenApiEnricher
import io.ghaylan.validata.openapi.enrichment.OpenApiEnrichmentCache
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMappers
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.media.Schema
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * springdoc [ModelConverter] that overlays Validata IR ([GeneratedSchemas]) onto DTO schemas
 * after the remainder of the converter chain builds the base OpenAPI model.
 *
 * Types without a generated [ObjectSchema] are left unchanged.
 * Prefer enriching the components schema via [ModelConverterContext.getDefinedModels] so
 * `/v3/api-docs` publishes constraints on `#/components/schemas/…` rather than only on
 * requestBody `$ref` siblings (OpenAPI 3.1).
 * Already-enriched schema instances are skipped ([OpenApiEnrichmentCache]).
 *
 * @property mappers OpenAPI override SPI; defaults to [OpenApiConstraintMappers.all]*
 * 
 * @author Ghaylan Saada
 */
class ValidataModelConverter(
	private val mappers: List<OpenApiConstraintMapper> = OpenApiConstraintMappers.all(),
) : ModelConverter {

	/**
	 * Resolves [type] through the remaining converter chain, then overlays Validata IR once.
	 *
	 * Mutates the resolved and/or components schemas and [OpenApiEnrichmentCache] when IR applies.
	 *
	 * @param type annotated Java type springdoc is converting
	 * @param context converter context (components models)
	 * @param chain remaining converters
	 * @return the resolved schema, or `null` when the chain produced none
	 */
	override fun resolve(
		type: AnnotatedType,
		context: ModelConverterContext,
		chain: MutableIterator<ModelConverter>,
	): Schema<*>? {
		
		val resolved = if (chain.hasNext()) {
			chain.next().resolve(type, context, chain)
		} else {
			null
		} ?: return null

		val clazz = rawClass(type.type) ?: return resolved
		val objectSchema = GeneratedSchemas.get(clazz) ?: return resolved

		val modelName = schemaName(type, resolved, clazz)
		val defined = modelName?.let { context.definedModels[it] }

		when {
			defined != null -> enrichOnce(defined, objectSchema)
			resolved.`$ref`.isNullOrBlank() -> enrichOnce(resolved, objectSchema)
			!resolved.properties.isNullOrEmpty() -> enrichOnce(resolved, objectSchema)
		}

		// Keep `$ref` wrappers clean once the components model carries the facets.
		if (!resolved.`$ref`.isNullOrBlank() && defined != null) {
			resolved.properties = null
			resolved.required = null
		}

		return resolved
	}

	/**
	 * Enriches [schema] from [objectSchema] unless this instance was already marked.
	 *
	 * Mutates [schema] and [OpenApiEnrichmentCache] when enrichment runs.
	 *
	 * @param schema OpenAPI schema to enrich
	 * @param objectSchema Validata IR for the DTO
	 */
	private fun enrichOnce(schema: Schema<*>, objectSchema: ObjectSchema) {
		if (!OpenApiEnrichmentCache.markSchema(schema)) return
		ObjectSchemaOpenApiEnricher.enrich(schema, objectSchema, mappers)
	}

	/**
	 * Best-effort components schema name from the annotated type, resolved schema, or class.
	 *
	 * @param type annotated type springdoc is converting
	 * @param resolved schema returned by the converter chain
	 * @param clazz raw DTO class
	 * @return components schema name, or `null` when none can be derived
	 */
	private fun schemaName(
		type: AnnotatedType,
		resolved: Schema<*>,
		clazz: Class<*>
	): String? {
		type.name?.takeIf { it.isNotBlank() }?.let { return it }
		resolved.name?.takeIf { it.isNotBlank() }?.let { return it }
		resolved.`$ref`?.substringAfterLast('/')?.takeIf { it.isNotBlank() }?.let { return it }
		return clazz.simpleName.takeIf { it.isNotBlank() }
	}

	/**
	 * Unwraps parameterized / wildcard types to a raw [Class].
	 *
	 * @param type Java type to unwrap; may be `null`
	 * @return raw class, or `null` when unknown
	 */
	private fun rawClass(type: Type?): Class<*>? = when (type) {
		is Class<*> -> type
		is ParameterizedType -> rawClass(type.rawType)
		is WildcardType -> type.upperBounds.firstOrNull()?.let(::rawClass)
		else -> null
	}
}
