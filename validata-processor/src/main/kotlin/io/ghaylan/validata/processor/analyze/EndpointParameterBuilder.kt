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
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.TypeClassification
import io.ghaylan.validata.processor.compat.has
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.EndpointParameterModel
import io.ghaylan.validata.processor.model.ShapeModel
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * Builds flat and body [EndpointParameterModel]s for one handler parameter.
 *
 * @property logger transport and `@RequestBody` cascade diagnostics
 * @property constraints shared constraint resolver for parameter annotations
 * @property shapes shared shape builder for flat parameter types
 * @property errorDocs parses `@ApiError` on flat parameters
 * @property strictCrossModuleCascade when `true`, unmarked cross-module body types are errors*
 * 
 * @author Ghaylan Saada
 */
internal class EndpointParameterBuilder(
	private val logger: KSPLogger,
	private val constraints: ConstraintModelBuilder,
	private val shapes: ShapeModelBuilder,
	private val errorDocs: ApiErrorAnnotationParser,
	private val strictCrossModuleCascade: Boolean,
) {
	
	/**
	 * Builds a flat query/header/path parameter: constraints, shape, and a map-key [EndpointParameterModel.readerExpr].
	 *
	 * Flat params never cascade into DTOs ([CascadePolicy.NO_CASCADE]). The reader pulls from the runtime
	 * `Map<String, Any?>` using the Spring-resolved transport name (not the Kotlin param name).
	 *
	 * No side effects beyond nested resolver / shape calls.
	 *
	 * @param param handler parameter declaration
	 * @param kind resolved transport kind (query, header, or path)
	 * @param ownerQ declaring handler class FQCN
	 * @param siblingParamTypes flat sibling parameter types for cross-param constraint refs
	 * @return flat parameter model with shape, constraints, and map reader	 
	 */
	fun buildFlatParameter(
		param: KSValueParameter,
		kind: EndpointArgumentKind,
		ownerQ: String,
		siblingParamTypes: Map<String, KSType>,
	): EndpointParameterModel {
		val declaredName = param.name?.asString()
			?: "param"
		val resolvedName = resolveTransportName(param, kind, declaredName)
		val type = param.type.resolve()
		val mapKeyLiteral = resolvedName.replace("\\", "\\\\").replace("\"", "\\\"")
		val readerExpr = "{ (it as Map<String, Any?>)[\"$mapKeyLiteral\"] }"
		val paramConstraints = constraints.resolveConstraintsFromAnnotations(
			ownerQualifiedName = ownerQ,
			subjectName = declaredName,
			subjectNode = param,
			annotations = param.annotations.toList(),
			valueType = type,
			siblingParamTypes = siblingParamTypes,
		)
		val shape = shapes.buildShape(
			type = type,
			typeUseAnnotations = emptyList(),
			ctx = ShapeBuildContext(
				ownerQualifiedName = ownerQ,
				subjectName = declaredName,
				subjectNode = param,
				visiting = mutableSetOf(),
				cascadePolicy = CascadePolicy.NO_CASCADE,
			),
		)
		val shapeWithTypeUse = mergeTypeUseIntoShape(type, shape, ownerQ, declaredName, param)
		
		return EndpointParameterModel(
			kind = kind,
			declaredName = declaredName,
			resolvedName = resolvedName,
			readerExpr = readerExpr,
			shape = shapeWithTypeUse,
			constraints = paramConstraints,
			errorDocs = errorDocs.parseMemberErrorDocs(param),
		)
	}
	
	/**
	 * Re-builds the shape when the parameter type itself carries nested type-use annotations
	 * (e.g. `List<@Size(min=3) String>`).
	 *
	 * No side effects beyond nested [ShapeModelBuilder.buildShape] when type-use annotations are present.
	 *
	 * @param type resolved parameter type
	 * @param fallback shape built without type-use annotations
	 * @param ownerQ declaring handler class FQCN
	 * @param subjectName Kotlin parameter name
	 * @param param handler parameter declaration
	 * @return [fallback] or a rebuilt shape when type-use annotations exist	 
	 */
	fun mergeTypeUseIntoShape(
		type: KSType,
		fallback: ShapeModel,
		ownerQ: String,
		subjectName: String,
		param: KSValueParameter,
	): ShapeModel {
		val typeRef = param.type
		val nestedAnns = typeRef.annotations.toList()
		if (nestedAnns.isEmpty() && type.arguments.all { it.annotations.none() && it.type?.annotations?.none() != false }) {
			return fallback
		}
		return shapes.buildShape(
			type = type,
			typeUseAnnotations = nestedAnns,
			ctx = ShapeBuildContext(
				ownerQualifiedName = ownerQ,
				subjectName = subjectName,
				subjectNode = param,
				visiting = mutableSetOf(),
				cascadePolicy = CascadePolicy.NO_CASCADE,
			),
		)
	}
	
	/**
	 * Builds a `@RequestBody` slot: records body / element type FQCNs for schema lookup at codegen.
	 *
	 * May emit KSP errors or warnings via [logger] for unsupported or unmarked body types.
	 *
	 * @param param handler `@RequestBody` parameter
	 * @param ownerQ declaring handler class FQCN
	 * @return body parameter model with schema target FQCNs	 
	 */
	fun buildBodyParameter(
		param: KSValueParameter,
		ownerQ: String,
	): EndpointParameterModel {
		val declaredName = param.name?.asString()
			?: "body"
		val type = param.type.resolve()
		val qName = type.declaration.qualifiedName?.asString()
		val isCollection = TypeClassification.isIterable(type) || TypeClassification.isArray(type)
		val isMap = TypeClassification.isMap(type)
		
		if (isMap) {
			logger.error(
				"@RequestBody parameter '$declaredName' on '$ownerQ' is a Map — map request bodies are not supported for schema validation. Wrap the payload in a @Validatable DTO.",
				param,
			)
		}
		val elementType = if (isCollection) {
			type.arguments.firstOrNull()?.type?.resolve()
		}
		else {
			null
		}
		val elementQ = elementType?.declaration?.qualifiedName?.asString()
		val schemaTarget = when {
			isCollection && elementQ != null -> elementQ
			else -> qName
		}
		
		if (schemaTarget != null) {
			val decl = (if (isCollection) elementType else type)?.declaration as? com.google.devtools.ksp.symbol.KSClassDeclaration
			val sameCompilation = decl?.containingFile != null
			val marked = decl != null && decl.has(ProcessorFqns.VALIDATABLE)
			when {
				sameCompilation && !marked && !TypeClassification.isPlatformLeaf(schemaTarget) -> logger.error(
					"@RequestBody type '$schemaTarget' on '$ownerQ.$declaredName' is not @Validatable. Annotate it with @Validatable (same-module types must be marked).",
					param,
				)
				
				!sameCompilation && decl != null && !marked && !TypeClassification.isPlatformLeaf(schemaTarget) -> {
					val message = "@RequestBody type '$schemaTarget' on '$ownerQ.$declaredName' could not be proven @Validatable (cross-module). Ensure the declaring module applies validata-processor."
					if (strictCrossModuleCascade) {
						logger.error(message, param)
					}
					else {
						logger.warn(message, param)
					}
				}
			}
		}
		
		return EndpointParameterModel(
			kind = EndpointArgumentKind.BODY,
			declaredName = declaredName,
			resolvedName = declaredName,
			bodyTypeQualifiedName = qName,
			bodyIsCollection = isCollection && elementQ != null,
			bodyElementTypeQualifiedName = elementQ,
		)
	}
	
	/**
	 * Collects Spring transport annotations on [param].
	 *
	 * Empty → not a validated transport slot (`OTHER` in the argument layout).
	 * More than one → author error (e.g. `@RequestParam` + `@PathVariable` on the same parameter).
	 *
	 * No side effects.
	 *
	 * @param param handler parameter declaration
	 * @return zero or more transport kinds detected on [param]	 
	 */
	fun classifyTransport(param: KSValueParameter): List<EndpointArgumentKind> {
		val kinds = mutableListOf<EndpointArgumentKind>()
		for (ann in param.annotations) {
			when (AnnotationFqcn.of(ann)) {
				ProcessorFqns.REQUEST_BODY -> kinds += EndpointArgumentKind.BODY
				ProcessorFqns.REQUEST_PARAM -> kinds += EndpointArgumentKind.QUERY
				ProcessorFqns.REQUEST_HEADER -> kinds += EndpointArgumentKind.HEADER
				ProcessorFqns.PATH_VARIABLE -> kinds += EndpointArgumentKind.PATH
			}
		}
		return kinds
	}
	
	/**
	 * Resolves the Spring-effective name with the same precedence as
	 * `Parameter.requestParamName()` / `requestHeaderName()` / `pathVariableName()`:
	 * `name` attribute, then `value`, then the Kotlin parameter name.
	 *
	 * No side effects.
	 *
	 * @param param handler parameter declaration
	 * @param kind transport kind whose binding annotation is consulted
	 * @param fallback Kotlin parameter name when binding attrs are blank
	 * @return Spring-effective transport name	 
	 */
	fun resolveTransportName(
		param: KSValueParameter,
		kind: EndpointArgumentKind,
		fallback: String,
	): String {
		val fqcn = when (kind) {
			EndpointArgumentKind.QUERY -> ProcessorFqns.REQUEST_PARAM
			EndpointArgumentKind.HEADER -> ProcessorFqns.REQUEST_HEADER
			EndpointArgumentKind.PATH -> ProcessorFqns.PATH_VARIABLE
			EndpointArgumentKind.BODY, EndpointArgumentKind.OTHER -> return fallback
		}
		val ann = EndpointDiscovery.findAnnotation(param, fqcn)
			?: return fallback
		val nameArg = ann.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.SpringBinding.NAME }?.value as? String
		if (!nameArg.isNullOrBlank()) return nameArg
		val valueArg = ann.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.SpringBinding.VALUE }?.value as? String
		if (!valueArg.isNullOrBlank()) return valueArg
		return fallback
	}
}
