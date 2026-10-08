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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.processor.naming.GeneratedNames

/**
 * Shared Kotlin source fragments for [ShapeModel] constructor-call expressions.
 *
 * Used by [SchemaCodeWriter] and [EndpointSchemaCodeWriter] so scalar / iterable / map / object-ref
 * emission cannot drift between object-schema and endpoint factories.
 *
 * Object-ref policy:
 * - Schema path: pass `schemasByQualifiedName` — in-unit peers use `PeerSchema.build()`, missing
 *   peers use `GeneratedSchemaLookup`.
 * - Endpoint path: pass `schemasByQualifiedName = null` — always `GeneratedSchemaLookup`.*
 * 
 * @author Ghaylan Saada
 */
internal object ShapeSourceRenderer {
	
	/**
	 * Renders a [ShapeModel] as a Kotlin constructor-call expression.
	 *
	 * Side effects: none.
	 *
	 * @param shape Shape to render.
	 * @param schemasByQualifiedName Peer lookup for schema factories; `null` forces
	 *   `GeneratedSchemaLookup` for every [ObjectRefShapeModel] (endpoint emission).
	 * @return Source fragment such as `ScalarShape(…)` or `ObjectRefShape(lazy { … }, …)`.	 
	 */
	fun renderShape(
		shape: ShapeModel,
		schemasByQualifiedName: Map<String, SchemaModel>?,
	): String =
		when (shape) {
			is ScalarShapeModel -> "ScalarShape(ScalarKind.${shape.kind.name}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints)})"
			is DynamicShapeModel -> "DynamicShape(${ConstraintSourceRenderer.renderConstraints(shape.constraints)})"
			is IterableShapeModel -> "IterableShape(${renderShape(shape.element, schemasByQualifiedName)}, ${
				ConstraintSourceRenderer.renderConstraints(shape.constraints)
			})"
			
			is MapShapeModel -> "MapShape(${renderShape(shape.key, schemasByQualifiedName)}, ${
				renderShape(shape.value, schemasByQualifiedName)
			}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints)})"
			
			is ObjectRefShapeModel -> {
				val call = objectRefSchemaCall(shape, schemasByQualifiedName)
				"ObjectRefShape(lazy { $call }, ${ConstraintSourceRenderer.renderConstraints(shape.constraints)})"
			}
		}
	
	/**
	 * Resolves the `lazy { … }` body for an [ObjectRefShapeModel].
	 *
	 * Side effects: none.
	 *
	 * @param shape Object-ref shape whose target type drives the call.
	 * @param schemasByQualifiedName Peer map, or `null` for always-lookup.
	 * @return `PeerSchema.build()` or `GeneratedSchemaLookup.requireGeneratedSchema(…)` fragment.	 
	 */
	private fun objectRefSchemaCall(
		shape: ObjectRefShapeModel,
		schemasByQualifiedName: Map<String, SchemaModel>?,
	): String {
		if (schemasByQualifiedName != null) {
			val peer = schemasByQualifiedName[shape.typeQualifiedName]
			if (peer != null) {
				return "${GeneratedNames.schemaObjectName(peer.packageName, peer.qualifiedName)}.build()"
			}
		}
		return "GeneratedSchemaLookup.requireGeneratedSchema(${shape.typeQualifiedName}::class.java)"
	}
}
