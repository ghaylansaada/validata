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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.model.EndpointArgumentSlotModel
import io.ghaylan.validata.processor.model.EndpointModel
import io.ghaylan.validata.processor.model.EndpointParameterModel
import io.ghaylan.validata.processor.naming.EndpointIdentifier
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * Builds [EndpointModel] graphs for every effective `@Validate` handler in the compilation.
 *
 * Orchestrates [EndpointDiscovery], [EndpointParameterBuilder], and [ValidateFlagsReader].
 *
 * @property logger emits transport / body / mapping diagnostics that fail or warn the consumer build
 * @property constraints shared constraint resolver (same instance as object-schema generation)
 * @property shapes shared shape builder (same instance as object-schema generation)
 * @property errorDocs shared `@ApiError` parser (same instance as [SchemaModelBuilder])
 * @property strictCrossModuleCascade when `true`, unmarked cross-module `@RequestBody` types are errors*
 * 
 * @author Ghaylan Saada
 */
internal class EndpointModelBuilder(
	private val logger: KSPLogger,
	private val constraints: ConstraintModelBuilder,
	private val shapes: ShapeModelBuilder,
	private val errorDocs: ApiErrorAnnotationParser,
	private val strictCrossModuleCascade: Boolean = false,
) {
	
	/**
	 * Delegates flat and body parameter construction for one handler.
	 */
	private val parameters = EndpointParameterBuilder(
		logger = logger,
		constraints = constraints,
		shapes = shapes,
		errorDocs = errorDocs,
		strictCrossModuleCascade = strictCrossModuleCascade,
	)
	
	/**
	 * Discovers and builds every endpoint model in this round.
	 *
	 * No side effects beyond KSP symbol reads and nested model construction.
	 *
	 * @param resolver current KSP resolver
	 * @param skipIdentifiers already-written endpoint ids — skip [buildEndpointModel] for these (still
	 *   rediscovered cheaply via identifier, without parameter walks)
	 * @return models sorted by [EndpointModel.identifier] for deterministic emission	 
	 */
	fun buildAll(
		resolver: Resolver,
		skipIdentifiers: Set<String> = emptySet(),
	): List<EndpointModel> {
		val candidates = EndpointDiscovery.discover(resolver, skipIdentifiers)
		return candidates.values.mapNotNull { (fn, ann) -> buildEndpointModel(fn, ann) }.sortedBy(EndpointModel::identifier)
	}
	
	/**
	 * Builds one [EndpointModel] for [function] using the effective [validateRequest] annotation.
	 *
	 * May emit KSP errors via [logger] for transport / body mapping violations.
	 *
	 * @param function Spring handler function
	 * @param validateRequest effective `@Validate` annotation for [function]
	 * @return endpoint model, or `null` when the function has no class owner	 
	 */
	private fun buildEndpointModel(
		function: KSFunctionDeclaration,
		validateRequest: KSAnnotation,
	): EndpointModel? {
		val owner = function.parentDeclaration as? KSClassDeclaration
			?: return null
		val ownerQ = owner.qualifiedName?.asString()
			?: return null
		val packageName = owner.packageName.asString()
		val identifier = EndpointIdentifier.of(function)
		val flags = ValidateFlagsReader.read(validateRequest)
		val parametersOut = mutableListOf<EndpointParameterModel>()
		val argumentLayout = mutableListOf<EndpointArgumentSlotModel>()
		var bodyCount = 0
		val flatSiblingTypes = LinkedHashMap<String, KSType>()
		for (param in function.parameters) {
			val kinds = parameters.classifyTransport(param)
			if (kinds.size == 1 && kinds.single() != EndpointArgumentKind.BODY) {
				val name = param.name?.asString()
					?: continue
				val type = param.type.resolve()
				flatSiblingTypes[name] = type
				val kind = kinds.single()
				val resolved = parameters.resolveTransportName(param, kind, name)
				if (resolved != name) flatSiblingTypes[resolved] = type
			}
		}
		
		for (param in function.parameters) {
			val kinds = parameters.classifyTransport(param)
			when {
				kinds.isEmpty() -> {
					argumentLayout += EndpointArgumentSlotModel(EndpointArgumentKind.OTHER)
					continue
				}
				
				kinds.size > 1 -> {
					logger.error(
						"Parameter '${param.name?.asString()}' on '$ownerQ.${function.simpleName.asString()}' " + "cannot be both ${
							kinds.joinToString(" and ")
						}. " + "Use a single transport annotation.",
						param,
					)
					argumentLayout += EndpointArgumentSlotModel(EndpointArgumentKind.OTHER)
					continue
				}
			}
			val kind = kinds.single()
			if (kind == EndpointArgumentKind.BODY) {
				bodyCount++
				if (bodyCount > 1) {
					logger.error(
						"Multiple @RequestBody parameters on '$ownerQ.${function.simpleName.asString()}'. " + "Only one is allowed.",
						param,
					)
					argumentLayout += EndpointArgumentSlotModel(EndpointArgumentKind.OTHER)
					continue
				}
				val bodyParam = parameters.buildBodyParameter(param, ownerQ)
				parametersOut += bodyParam
				argumentLayout += EndpointArgumentSlotModel(EndpointArgumentKind.BODY)
			}
			else {
				val flat = parameters.buildFlatParameter(param, kind, ownerQ, flatSiblingTypes)
				parametersOut += flat
				argumentLayout += EndpointArgumentSlotModel(
					kind = kind,
					name = flat.resolvedName,
				)
			}
		}
		
		return EndpointModel(
			identifier = identifier,
			packageName = packageName,
			functionQualifiedName = "$ownerQ.${function.simpleName.asString()}",
			sourceFilePath = function.containingFile?.filePath,
			oneErrorPerParam = flags.oneErrorPerParam,
			failFast = flags.failFast,
			groupsFqcn = flags.groupsFqcn,
			parameters = parametersOut,
			argumentLayout = argumentLayout,
		)
	}
}
