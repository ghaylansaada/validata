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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.schema.ref.PropertyRefScalarCompatibility
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * Compile-time checks for cross-field and element property references.
 *
 * ## What is checked
 * 1. **Existence** — every segment of a dotted path resolves on the owner (or element) schema
 * 2. **Readability** — intermediate segments are object refs (not scalars / `@NoCascade` opaques)
 * 3. **Type compatibility** — via [PropertyRefScalarCompatibility]
 *   for comparison constraints
 *
 * Nested shape walks are depth-capped by [maxShapeNestingDepth] so pathological nesting fails with
 * a KSP error instead of a JVM [StackOverflowError].
 *
 * @property logger emits KSP errors/warnings that fail or warn the consumer build
 * @property maxShapeNestingDepth maximum iterable/map nesting when walking type-use constraints*
 * 
 * @author Ghaylan Saada
 */
internal class PropertyReferenceVerifier(
	private val logger: KSPLogger,
	private val maxShapeNestingDepth: Int = DEFAULT_MAX_SHAPE_NESTING_DEPTH,
) {
	
	/**
	 * Verifies every property-level constraint on [schema] against the schema graph.
	 *
	 * Side effects: may emit KSP errors via [logger].
	 *
	 * @param schema object schema under verification
	 * @param schemasByQualifiedName all schemas built in this round — needed to step into nested
	 *   [ObjectRefShapeModel]s on dotted paths
	 * @param site optional KSP symbol for IDE jump-to-source on errors (typically the `@Validatable` class)	 
	 */
	fun verify(
		schema: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		site: KSNode? = null,
	) {
		val ctx = PropertyVerifyContext.create(
			logger = logger,
			site = site,
			maxShapeNestingDepth = maxShapeNestingDepth,
			schemasByQualifiedName = schemasByQualifiedName,
		)
		for (property in schema.properties) {
			verifyConstraints(
				ctx = ctx,
				ownerSchema = schema,
				subjectProperty = property,
				constraints = property.constraints,
				shapeForElementRefs = property.shape,
				subjectCheck = SubjectCheck.REQUIRED,
			)
			verifyNestedShapeConstraints(
				ctx = ctx,
				ownerSchema = schema,
				subjectProperty = property,
				shape = property.shape,
				depth = 0,
			)
		}
	}
	
	/**
	 * Verifies cross-field refs among an endpoint's flat query/header/path parameters.
	 *
	 * Flat sections have no nested object schemas, so only single-segment sibling names are valid.
	 * Body parameters are skipped here — their constraints are checked via [verify] on object schemas.
	 *
	 * Side effects: may emit KSP errors via [logger].
	 *
	 * @param endpoint endpoint under verification
	 * @param schemasByQualifiedName object schemas for element-ref checks on collection flat params
	 * @param site optional KSP symbol for diagnostics (typically the handler function)	 
	 */
	fun verifyEndpoint(
		endpoint: EndpointModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		site: KSNode? = null,
	) {
		val error: (String) -> Unit = { logger.error(it, site) }
		val schemaPathChecks = PropertySchemaPathChecks(error)
		val elementRefChecks = PropertyElementRefChecks(
			error = error,
			maxShapeNestingDepth = maxShapeNestingDepth,
			pathChecks = schemaPathChecks,
		)
		val endpointRefChecks = PropertyEndpointRefChecks(
			error = error,
			maxShapeNestingDepth = maxShapeNestingDepth,
			elementRefChecks = elementRefChecks,
		)
		val lookup = endpoint.flatParameterLookup()
		for (parameter in endpoint.parameters) {
			if (parameter.kind == EndpointArgumentKind.BODY) continue
			endpointRefChecks.verifyEndpointConstraints(
				endpoint = endpoint,
				declaredName = parameter.declaredName,
				resolvedName = parameter.resolvedName,
				shape = parameter.shape,
				constraints = parameter.constraints,
				lookup = lookup,
				schemasByQualifiedName = schemasByQualifiedName,
			)
			endpointRefChecks.verifyNestedEndpointShapeConstraints(
				endpoint = endpoint,
				declaredName = parameter.declaredName,
				shape = parameter.shape,
				schemasByQualifiedName = schemasByQualifiedName,
				depth = 0,
			)
		}
	}
	
	/**
	 * Runs sibling-path, subject-type, and element-path checks for constraints that apply to
	 * [shapeForElementRefs] (property shape or a nested type-use node).
	 *
	 * Side effects: may emit KSP errors via [ctx].
	 *
	 * @param ctx Shared verification collaborators.
	 * @param ownerSchema Schema that owns [subjectProperty].
	 * @param subjectProperty Property or logical subject for diagnostics.
	 * @param constraints Constraint models to verify.
	 * @param shapeForElementRefs Shape used for element-ref resolution.
	 * @param subjectCheck Whether subject scalar compatibility runs for this list.	 
	 */
	private fun verifyConstraints(
		ctx: PropertyVerifyContext,
		ownerSchema: SchemaModel,
		subjectProperty: PropertyModel,
		constraints: List<ConstraintModel>,
		shapeForElementRefs: ShapeModel,
		subjectCheck: SubjectCheck,
	) {
		for (constraint in constraints) {
			ctx.elementRefChecks.reportDistinctContainerPlacement(
				ownerLabel = ownerSchema.qualifiedName,
				subjectName = subjectProperty.declaredName,
				subjectShape = shapeForElementRefs,
				annotationSimpleName = constraint.annotationSimpleName,
			)
			if (subjectCheck == SubjectCheck.REQUIRED) {
				ctx.schemaPathChecks.verifySubjectCompatibility(
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					subjectShape = shapeForElementRefs,
					compatibilityKind = constraint.compatibilityKind,
					annotationSimpleName = constraint.annotationSimpleName,
				)
			}
			for (ref in constraint.siblingRefs) {
				ctx.schemaPathChecks.verifySiblingPath(
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					path = ref,
					compatibilityKind = constraint.compatibilityKind,
					annotationSimpleName = constraint.annotationSimpleName,
					schemasByQualifiedName = ctx.schemasByQualifiedName,
				)
			}
			ctx.elementRefChecks.reportUnknownElementRefs(
				ownerLabel = ownerSchema.qualifiedName,
				subjectName = subjectProperty.declaredName,
				subjectShape = shapeForElementRefs,
				elementRefs = constraint.elementRefs,
				annotationSimpleName = constraint.annotationSimpleName,
				schemasByQualifiedName = ctx.schemasByQualifiedName,
			)
		}
	}
	
	/**
	 * Verifies type-use constraints hanging on nested shape nodes
	 * (e.g. `List<@Distinct(by = ["a"]) String>` — `@Distinct` lives on the element [ScalarShapeModel]).
	 *
	 * Side effects: may emit KSP errors via [ctx]; recurses into iterable/map children.
	 *
	 * @param ctx Shared verification collaborators.
	 * @param ownerSchema Schema that owns [subjectProperty].
	 * @param subjectProperty Property whose shape tree is walked.
	 * @param shape Current shape node.
	 * @param depth Current nesting depth for depth-cap enforcement.	 
	 */
	private fun verifyNestedShapeConstraints(
		ctx: PropertyVerifyContext,
		ownerSchema: SchemaModel,
		subjectProperty: PropertyModel,
		shape: ShapeModel,
		depth: Int,
	) {
		if (depth > maxShapeNestingDepth) {
			ctx.error(
				"Shape nesting on '${ownerSchema.qualifiedName}.${subjectProperty.declaredName}' exceeds " + "max depth $maxShapeNestingDepth.",
			)
			return
		}
		when (shape) {
			is IterableShapeModel -> {
				verifyConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					constraints = shape.constraints,
					shapeForElementRefs = shape,
					subjectCheck = SubjectCheck.SKIP,
				)
				verifyNestedShapeConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					shape = shape.element,
					depth = depth + 1,
				)
			}
			
			is MapShapeModel -> {
				verifyConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					constraints = shape.constraints,
					shapeForElementRefs = shape,
					subjectCheck = SubjectCheck.SKIP,
				)
				verifyNestedShapeConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					shape = shape.key,
					depth = depth + 1,
				)
				verifyNestedShapeConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					shape = shape.value,
					depth = depth + 1,
				)
			}
			
			is ObjectRefShapeModel -> {
				verifyConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					constraints = shape.constraints,
					shapeForElementRefs = shape,
					subjectCheck = SubjectCheck.SKIP,
				)
			}
			
			is ScalarShapeModel, is DynamicShapeModel -> {
				verifyConstraints(
					ctx = ctx,
					ownerSchema = ownerSchema,
					subjectProperty = subjectProperty,
					constraints = shape.constraints,
					shapeForElementRefs = shape,
					subjectCheck = SubjectCheck.REQUIRED,
				)
			}
		}
	}
	
	companion object {
		
		/**
		 * Default cap for iterable/map nesting when verifying type-use constraints.
		 */
		const val DEFAULT_MAX_SHAPE_NESTING_DEPTH: Int = 32
	}
}
