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
package io.ghaylan.validata.processor.verify

import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.schema.PropertyPath

/**
 * Endpoint flat sibling refs and nested endpoint shape walks.
 *
 * @property error emits a KSP diagnostic for the current verification site
 * @property maxShapeNestingDepth maximum iterable/map nesting when walking type-use constraints
 * @property elementRefChecks element-path checks shared with schema verification*
 * 
 * @author Ghaylan Saada
 */
internal class PropertyEndpointRefChecks(
	private val error: (String) -> Unit,
	private val maxShapeNestingDepth: Int,
	private val elementRefChecks: PropertyElementRefChecks,
) {
	
	/**
	 * Verifies sibling and element refs among flat endpoint parameters.
	 *
	 * Side effects: may emit KSP errors via [error].
	 *
	 * @param endpoint Endpoint under verification.
	 * @param declaredName Kotlin parameter name.
	 * @param resolvedName Spring-effective parameter name.
	 * @param shape Parameter shape for element-ref checks.
	 * @param constraints Property-level constraints on the parameter.
	 * @param lookup Flat-parameter name → shape lookup from [EndpointModel.flatParameterLookup].
	 * @param schemasByQualifiedName All schemas built in this round for element-type lookup.	 
	 */
	fun verifyEndpointConstraints(
		endpoint: EndpointModel,
		declaredName: String,
		resolvedName: String,
		shape: ShapeModel,
		constraints: List<ConstraintModel>,
		lookup: NamedShapeLookup,
		schemasByQualifiedName: Map<String, SchemaModel>,
	) {
		for (constraint in constraints) {
			elementRefChecks.reportDistinctContainerPlacement(
				ownerLabel = endpoint.functionQualifiedName,
				subjectName = declaredName,
				subjectShape = shape,
				annotationSimpleName = constraint.annotationSimpleName,
			)
			for (ref in constraint.siblingRefs) {
				val segments = PropertyPath.split(ref)
				if (segments.size > 1) {
					error("Constraint on '${endpoint.functionQualifiedName}.$declaredName' " +
							"references nested path '$ref', but flat query/header/path parameters have no nested object schema to step into.")
					continue
				}
				if (lookup.resolve(ref) == null) {
					error("Constraint on '${endpoint.functionQualifiedName}.$declaredName' references unknown sibling '$ref'. " +
							"Use the name of another query/header/path parameter on the same handler.")
					continue
				}
				if (ref == declaredName || ref == resolvedName) {
					error("Constraint on '${endpoint.functionQualifiedName}.$declaredName' cannot reference itself via '$ref'. " +
							"Cross-field constraints must name " + "a different sibling parameter.")
				}
			}
			elementRefChecks.reportUnknownElementRefs(
				ownerLabel = endpoint.functionQualifiedName,
				subjectName = declaredName,
				subjectShape = shape,
				elementRefs = constraint.elementRefs,
				annotationSimpleName = constraint.annotationSimpleName,
				schemasByQualifiedName = schemasByQualifiedName,
			)
		}
	}
	
	/**
	 * Walks nested type-use constraints on endpoint parameter shapes.
	 *
	 * Side effects: may emit KSP errors via [error]; recurses into iterable/map children.
	 *
	 * @param endpoint Endpoint under verification.
	 * @param declaredName Kotlin parameter name for diagnostics.
	 * @param shape Current shape node.
	 * @param schemasByQualifiedName All schemas built in this round for element-type lookup.
	 * @param depth Current nesting depth for depth-cap enforcement.	 
	 */
	fun verifyNestedEndpointShapeConstraints(
		endpoint: EndpointModel,
		declaredName: String,
		shape: ShapeModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		depth: Int,
	) {
		if (depth > maxShapeNestingDepth) {
			error("Shape nesting on '${endpoint.functionQualifiedName}.$declaredName' exceeds " + "max depth $maxShapeNestingDepth.")
			return
		}
		when (shape) {
			is IterableShapeModel -> {
				for (constraint in shape.constraints) {
					elementRefChecks.reportDistinctContainerPlacement(
						ownerLabel = endpoint.functionQualifiedName,
						subjectName = declaredName,
						subjectShape = shape,
						annotationSimpleName = constraint.annotationSimpleName,
					)
					elementRefChecks.reportUnknownElementRefs(
						ownerLabel = endpoint.functionQualifiedName,
						subjectName = declaredName,
						subjectShape = shape,
						elementRefs = constraint.elementRefs,
						annotationSimpleName = constraint.annotationSimpleName,
						schemasByQualifiedName = schemasByQualifiedName,
					)
				}
				verifyNestedEndpointShapeConstraints(
					endpoint = endpoint,
					declaredName = declaredName,
					shape = shape.element,
					schemasByQualifiedName = schemasByQualifiedName,
					depth = depth + 1,
				)
			}
			
			is MapShapeModel -> {
				for (constraint in shape.constraints) {
					elementRefChecks.reportDistinctContainerPlacement(
						ownerLabel = endpoint.functionQualifiedName,
						subjectName = declaredName,
						subjectShape = shape,
						annotationSimpleName = constraint.annotationSimpleName,
					)
					elementRefChecks.reportUnknownElementRefs(
						ownerLabel = endpoint.functionQualifiedName,
						subjectName = declaredName,
						subjectShape = shape,
						elementRefs = constraint.elementRefs,
						annotationSimpleName = constraint.annotationSimpleName,
						schemasByQualifiedName = schemasByQualifiedName,
					)
				}
				verifyNestedEndpointShapeConstraints(
					endpoint = endpoint,
					declaredName = declaredName,
					shape = shape.key,
					schemasByQualifiedName = schemasByQualifiedName,
					depth = depth + 1,
				)
				verifyNestedEndpointShapeConstraints(
					endpoint = endpoint,
					declaredName = declaredName,
					shape = shape.value,
					schemasByQualifiedName = schemasByQualifiedName,
					depth = depth + 1,
				)
			}
			
			is ObjectRefShapeModel, is ScalarShapeModel, is DynamicShapeModel -> {
				for (constraint in shape.constraints) {
					elementRefChecks.reportUnknownElementRefs(
						ownerLabel = endpoint.functionQualifiedName,
						subjectName = declaredName,
						subjectShape = shape,
						elementRefs = constraint.elementRefs,
						annotationSimpleName = constraint.annotationSimpleName,
						schemasByQualifiedName = schemasByQualifiedName,
					)
				}
			}
		}
	}
}
