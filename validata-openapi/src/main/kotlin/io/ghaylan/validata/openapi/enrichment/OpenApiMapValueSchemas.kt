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

import io.swagger.v3.oas.models.media.Schema

/**
 * Resolves the OpenAPI schema used for map **values** without clobbering
 * `additionalProperties: false`.
 *
 * When springdoc left `additionalProperties` as `true` or unset, replaces it with an object
 * schema so value constraints can be documented. When it is already a [Schema], that instance
 * is reused. When it is `false`, returns `null` (nothing to enrich).*
 * 
 * @author Ghaylan Saada
 */
internal object OpenApiMapValueSchemas {
	
	/**
	 * Schema for map values under [schema], or `null` when enrichment must be skipped.
	 *
	 * May mutate [schema].additionalProperties when upgrading `true` / unset to a schema object.
	 *
	 * @param schema OpenAPI schema for a map-shaped property
	 * @return value schema to enrich, or `null` when `additionalProperties` is `false`	 
	 */
	fun resolve(schema: Schema<*>): Schema<*>? = when (val additional = schema.additionalProperties) {
		is Schema<*> -> additional
		false -> null
		else -> Schema<Any>().also { schema.additionalProperties = it }
	}
}
