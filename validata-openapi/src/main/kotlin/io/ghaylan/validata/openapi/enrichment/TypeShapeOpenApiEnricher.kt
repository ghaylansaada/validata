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

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.*
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KClass

/**
 * Shared nested type-use enrichment for DTO schemas and operation parameters.
 *
 * Applies constraints on [TypeShape] nodes and recurses into list items / map values.*
 * 
 * @author Ghaylan Saada
 */
internal object TypeShapeOpenApiEnricher {
	
	/**
	 * Recursively applies type-use constraints on [shape] (list items, map values).
	 *
	 * Mutates [schema] and nested item / additionalProperties schemas.
	 *
	 * @param shape type-use IR node
	 * @param schema OpenAPI schema matching [shape]
	 * @param mappers OpenAPI override SPI
	 * @param activeGroups when non-null, only active groups are applied; `null` applies all (DTO union)	 
	 */
	fun applyShape(
		shape: TypeShape,
		schema: Schema<*>,
		mappers: List<OpenApiConstraintMapper>,
		activeGroups: Set<KClass<*>>? = null,
	) {
		applyAllConstraints(shape.constraints, schema, shape, mappers, activeGroups)
		when (shape) {
			is ScalarShape,
			is ObjectRefShape,
			is DynamicShape -> Unit
			is IterableShape -> {
				val items = schema.items ?: Schema<Any>().also { schema.items = it }
				applyShape(shape.element, items, mappers, activeGroups)
			}
			is MapShape -> {
				val valueSchema = OpenApiMapValueSchemas.resolve(schema) ?: return
				applyShape(shape.value, valueSchema, mappers, activeGroups)
			}
		}
	}
	
	/**
	 * Applies each compiled constraint onto [schema].
	 *
	 * @param constraints compiled constraints on a property or shape
	 * @param schema OpenAPI schema to enrich
	 * @param shape structural hint for shape-sensitive documenters; may be `null`
	 * @param mappers OpenAPI override SPI
	 * @param activeGroups when non-null, filters by [ConstraintGroupFilter]; `null` applies all
	 * @return `true` when any constraint marked the property as required	 
	 */
	fun applyAllConstraints(
		constraints: List<CompiledConstraint>,
		schema: Schema<*>,
		shape: TypeShape?,
		mappers: List<OpenApiConstraintMapper>,
		activeGroups: Set<KClass<*>>? = null,
	): Boolean {
		var required = false
		for (compiled in constraints) {
			val metadata = compiled.metadata as? ConstraintMetadata
				?: continue
			if (activeGroups != null && !ConstraintGroupFilter.isActive(metadata, activeGroups)) continue
			if (ConstraintSchemaApplicator.apply(metadata, schema, shape, mappers)) {
				required = true
			}
		}
		return required
	}
}
