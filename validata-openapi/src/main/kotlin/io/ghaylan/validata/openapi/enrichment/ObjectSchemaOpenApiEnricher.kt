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

import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMappers
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.swagger.v3.oas.models.media.Schema

/**
 * Overlays Validata [ObjectSchema] constraints onto a springdoc/swagger [Schema].
 *
 * Property keys follow [PropertySpec.externalName] (Jackson `@JsonProperty` / naming strategy).
 *
 * Without endpoint conversion context, every constraint is applied regardless of validation
 * `groups`. Error-code collection ([EndpointErrorCodeCollector]) narrows codes using the active
 * endpoint group set; schema facets on shared DTOs remain the union of declared constraints.*
 * 
 * @author Ghaylan Saada
 */
object ObjectSchemaOpenApiEnricher {
	
	/**
	 * Applies IR constraints under [objectSchema] onto [openApiSchema] properties.
	 *
	 * Mutates [openApiSchema] properties, nested schemas, required list, and vendor extensions.
	 *
	 * @param openApiSchema swagger schema to mutate (typically a components model)
	 * @param objectSchema Validata IR for the DTO
	 * @param mappers OpenAPI override SPI; defaults to [OpenApiConstraintMappers.all]	 
	 */
	fun enrich(
		openApiSchema: Schema<*>,
		objectSchema: ObjectSchema,
		mappers: List<OpenApiConstraintMapper> = OpenApiConstraintMappers.all(),
	) {
		@Suppress("UNCHECKED_CAST")
		val properties = (openApiSchema.properties as MutableMap<String, Schema<*>>?)
			?: linkedMapOf<String, Schema<*>>().also { openApiSchema.properties = it }
		val required = LinkedHashSet<String>()
		openApiSchema.required?.let { required.addAll(it) }
		
		for (prop in objectSchema.properties) {
			val propertySchema = resolvePropertySchema(properties, prop)
			val markedRequired = TypeShapeOpenApiEnricher.applyAllConstraints(
				constraints = prop.constraints,
				schema = propertySchema,
				shape = prop.shape,
				mappers = mappers,
				activeGroups = null)
			TypeShapeOpenApiEnricher.applyShape(prop.shape, propertySchema, mappers, activeGroups = null)
			PropertyOpenApiExtensionsWriter.write(
				schema = propertySchema,
				prop = prop,
				ownerSchema = objectSchema)
			if (markedRequired) {
				required.add(prop.externalName)
			}
		}
		
		if (required.isNotEmpty()) {
			openApiSchema.required = required.toList()
		}
	}
	
	/**
	 * Resolves the swagger property schema for [prop], renaming declared-name keys to
	 * [PropertySpec.externalName] when needed.
	 *
	 * May mutate [properties] (rename or insert).
	 *
	 * @param properties mutable swagger property map on the parent schema
	 * @param prop Validata property IR
	 * @return existing or newly created schema for [prop]	 
	 */
	private fun resolvePropertySchema(
		properties: MutableMap<String, Schema<*>>,
		prop: PropertySpec,
	): Schema<*> {
		properties[prop.externalName]?.let { return it }
		if (prop.declaredName != prop.externalName) {
			val underDeclared = properties.remove(prop.declaredName)
			if (underDeclared != null) {
				properties[prop.externalName] = underDeclared
				return underDeclared
			}
		}
		return Schema<Any>().also { properties[prop.externalName] = it }
	}
}
