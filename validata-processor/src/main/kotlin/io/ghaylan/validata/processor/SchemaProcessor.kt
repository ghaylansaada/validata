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
package io.ghaylan.validata.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import io.ghaylan.validata.processor.analyze.*
import io.ghaylan.validata.processor.compat.ProcessorRoundCache
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.EndpointModel
import io.ghaylan.validata.processor.model.SchemaModel
import io.ghaylan.validata.processor.verify.PropertyReferenceVerifier

/**
 * KSP [SymbolProcessor] that generates validation IR factories for one compilation unit.
 *
 * ## Module
 *
 * Entry orchestrator in **`validata-processor`**. Builds models (`analyze.*`), verifies
 * cross-field paths ([PropertyReferenceVerifier]), then emits Kotlin + SPI resources
 * (`codegen.*`). Literal-argument checks run earlier via [ConstraintModelBuilder].
 *
 * ## Emits
 * 1. One schema factory + Fields object per `@Validatable` type
 * 2. One endpoint factory per `@Validate` handler
 * 3. Thin SPI aggregators (`ObjectSchemasModule`, `RequestSchemasModule`)
 *
 * ## Multi-round
 * Roots that fail [KSClassDeclaration.validate] are deferred to a later round (returned from
 * [process]). Per-type schema files are written as soon as a root is ready. Endpoint factories
 * and SPI aggregators wait until nothing remains deferred so property-ref checks see the full
 * schema graph. Schema roots are tracked by FQCN only and re-resolved from the current [Resolver]
 * before verify/write (KSP symbols must not be retained across rounds).
 *
 * Consumers never construct this class directly — KSP loads [SchemaProcessorProvider].
 *
 * @param environment KSP environment (logger + code generator + options)*
 * 
 * @author Ghaylan Saada
 */
class SchemaProcessor(
	environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

	/**
	 * KSP logger for analysis and verification diagnostics.
	 */
	private val logger = environment.logger

	/**
	 * KSP option map for this compilation.
	 */
	private val options = environment.options

	/**
	 * True after aggregators have been written (or there was nothing to emit).
	 */
	private var completed = false

	/**
	 * FQCNs whose per-type schema and Fields files were already written.
	 */
	private val writtenSchemaFqcns = linkedSetOf<String>()

	/**
	 * Endpoint ids whose factory files were already written.
	 */
	private val writtenEndpointIds = linkedSetOf<String>()

	/**
	 * FQCNs of discovered `@Validatable` roots — never store [KSClassDeclaration] across rounds.
	 */
	private val schemaRootFqcns = linkedSetOf<String>()

	/**
	 * Accumulated schema models keyed by validated type FQCN.
	 */
	private val schemaModelsByFqcn = LinkedHashMap<String, SchemaModel>()

	/**
	 * Accumulated endpoint models keyed by endpoint identifier.
	 */
	private val endpointModelsById = LinkedHashMap<String, EndpointModel>()

	/**
	 * Round-scoped FQCN → class resolution (cleared at the start of each [process]).
	 */
	private val classByFqcn = HashMap<String, KSClassDeclaration?>()

	/**
	 * Writes generated schema, Fields, endpoint, and SPI files.
	 */
	private val emit = SchemaEmitSupport(
		codeGenerator = environment.codeGenerator,
		resolveClass = ::resolveClass)

	/**
	 * Per-round constraint model builder shared by schema and endpoint analysis.
	 */
	private val jacksonNaming = ProcessorOptions.jacksonNaming(options, logger)

	private val constraintBuilder = ConstraintModelBuilder(
		logger = logger,
		jacksonNaming = jacksonNaming,
	)

	/**
	 * Shape classifier for properties and endpoint parameters.
	 */
	private val shapeBuilder = ShapeModelBuilder(
		logger = logger,
		constraints = constraintBuilder,
		strictCrossModuleCascade = ProcessorOptions.strictCrossModuleCascade(options))

	/**
	 * Optional OpenAPI `@ApiError` doc parser for schema properties.
	 */
	private val errorDocs = ApiErrorAnnotationParser(logger)

	/**
	 * Builds [SchemaModel] graphs from `@Validatable` types.
	 */
	private val schemaBuilder = SchemaModelBuilder(
		logger = logger,
		constraints = constraintBuilder,
		shapes = shapeBuilder,
		errorDocs = errorDocs,
		jacksonNaming = jacksonNaming,
	)

	/**
	 * Builds [EndpointModel] graphs from `@Validate` handlers.
	 */
	private val endpointBuilder = EndpointModelBuilder(
		logger = logger,
		constraints = constraintBuilder,
		shapes = shapeBuilder,
		errorDocs = errorDocs,
		strictCrossModuleCascade = ProcessorOptions.strictCrossModuleCascade(options),
	)

	/**
	 * Verifies cross-field property references before emission.
	 */
	private val referenceVerifier = PropertyReferenceVerifier(
		logger = logger,
		maxShapeNestingDepth = ProcessorOptions.maxShapeNestingDepth(options, logger),
	)

	/**
	 * Builds and emits schema and endpoint generated sources for the current compilation round.
	 *
	 * Side effects: writes generated files via [SchemaEmitSupport]; mutates round caches and
	 * accumulated model maps.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @return `@Validatable` roots not yet resolvable (deferred); empty when finished.
	 */
	override fun process(resolver: Resolver): List<KSAnnotated> {
		if (completed) {
			SchemaProcessorRoundProbe.completedEarlyReturns++
			return emptyList()
		}
		classByFqcn.clear()
		ProcessorRoundCache.begin()
		try {
			constraintBuilder.beginRound(resolver)

			val annotatedRoots = discoverValidatableRoots(resolver)
			val (deferred, deferredFqcns) = collectDeferredRoots(annotatedRoots)
			materializeReadySchemas(resolver, annotatedRoots, deferredFqcns)
			val newlyReadySchemas = LinkedHashMap<String, SchemaModel>()
			for ((fqcn, model) in schemaModelsByFqcn) {
				if (fqcn !in writtenSchemaFqcns) newlyReadySchemas[fqcn] = model
			}
			verifyAndWriteSchemas(resolver, newlyReadySchemas)

			if (deferred.isNotEmpty()) {
				// Skip endpoint rediscovery while schemas remain deferred — shapes may still change.
				SchemaProcessorRoundProbe.endpointBuildAllSkippedWhileDeferred++
				return deferred
			}

			refreshEndpoints(resolver)
			finalizeEndpointsAndAggregators(resolver)
			completed = true
			return emptyList()
		} finally {
			constraintBuilder.endRound()
			ProcessorRoundCache.end()
		}
	}

	/**
	 * Clears round-scoped builder state when KSP finishes successfully.
	 *
	 * Side effects: ends constraint round session; clears [ProcessorRoundCache].
	 */
	override fun finish() {
		constraintBuilder.endRound()
		ProcessorRoundCache.forceClear()
	}

	/**
	 * Clears round-scoped builder state after a KSP error.
	 *
	 * Side effects: ends constraint round session; clears [ProcessorRoundCache].
	 */
	override fun onError() {
		constraintBuilder.endRound()
		ProcessorRoundCache.forceClear()
	}

	/**
	 * One `@Validatable` discovery walk for the round (shared by deferred + materialize).
	 *
	 * Side effects: none.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @return All class declarations annotated with `@Validatable`.
	 */
	private fun discoverValidatableRoots(resolver: Resolver): List<KSClassDeclaration> =
		resolver.getSymbolsWithAnnotation(ProcessorFqns.VALIDATABLE)
			.filterIsInstance<KSClassDeclaration>()
			.toList()

	/**
	 * Collects `@Validatable` roots that fail [KSClassDeclaration.validate] this round.
	 *
	 * Side effects: none.
	 *
	 * @param annotatedRoots Candidate roots from [discoverValidatableRoots].
	 * @return Deferred symbols for KSP plus their FQCNs (identity-safe across resolver walks).
	 */
	private fun collectDeferredRoots(
		annotatedRoots: List<KSClassDeclaration>,
	): Pair<ArrayList<KSAnnotated>, Set<String>> {
		val deferred = ArrayList<KSAnnotated>()
		val deferredFqcns = linkedSetOf<String>()

		for (root in annotatedRoots) {
			val fqcn = root.qualifiedName?.asString()
			if (fqcn != null && fqcn in writtenSchemaFqcns) continue
			if (!root.validate(enableNewFeatures = true)) {
				deferred += root
				if (fqcn != null) deferredFqcns += fqcn
			}
		}
		return deferred to deferredFqcns
	}

	/**
	 * Builds [SchemaModel]s for ready roots and records their FQCNs (not KS declarations).
	 *
	 * Side effects: mutates [schemaModelsByFqcn] and [schemaRootFqcns]; may log duplicate errors.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @param annotatedRoots All discovered `@Validatable` roots.
	 * @param deferredFqcns FQCNs deferred because [KSClassDeclaration.validate] failed.
	 */
	private fun materializeReadySchemas(
		resolver: Resolver,
		annotatedRoots: List<KSClassDeclaration>,
		deferredFqcns: Set<String>,
	) {
		for (root in annotatedRoots) {
			val fqcn = root.qualifiedName?.asString()
			if (fqcn != null && fqcn in writtenSchemaFqcns) continue
			if (fqcn != null && fqcn in deferredFqcns) continue
			if (!root.validate(enableNewFeatures = true)) continue

			val model = schemaBuilder.buildSchema(
				clazz = root,
				resolver = resolver,
				visiting = mutableSetOf()
			) ?: continue
			
			val prev = schemaModelsByFqcn.put(model.qualifiedName, model)
			
			if (prev != null && model.qualifiedName !in writtenSchemaFqcns) {
				logger.error("Duplicate @Validatable schema for '${model.qualifiedName}'. " +
						"Each type must produce at most one schema in this compilation unit.",
					root)
			}
			schemaRootFqcns += model.qualifiedName
		}
	}

	/**
	 * Verifies property refs and writes per-type schema + Fields files for newly ready models.
	 *
	 * Side effects: writes generated files via [SchemaEmitSupport]; updates [writtenSchemaFqcns].
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @param newlyReadySchemas Schema models not yet written this compilation.
	 */
	private fun verifyAndWriteSchemas(
		resolver: Resolver,
		newlyReadySchemas: Map<String, SchemaModel>,
	) {
		for ((fqcn, model) in newlyReadySchemas) {
			referenceVerifier.verify(
				schema = model,
				schemasByQualifiedName = schemaModelsByFqcn,
				site = resolveClass(resolver, fqcn))
		}

		if (newlyReadySchemas.isNotEmpty()) {
			emit.writeObjectSchemaFiles(
				resolver = resolver,
				byQualified = LinkedHashMap(newlyReadySchemas),
				allSchemas = schemaModelsByFqcn)
			writtenSchemaFqcns += newlyReadySchemas.keys
		}
	}

	/**
	 * Rebuilds unwritten endpoint models from the current-round [resolver].
	 *
	 * Side effects: mutates [endpointModelsById]; increments [SchemaProcessorRoundProbe] counters.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 */
	private fun refreshEndpoints(resolver: Resolver) {
		SchemaProcessorRoundProbe.endpointBuildAllCalls++
		for (endpoint in endpointBuilder.buildAll(resolver, skipIdentifiers = writtenEndpointIds)) {
			endpointModelsById[endpoint.identifier] = endpoint
		}
	}

	/**
	 * Verifies endpoints, writes endpoint factories, then SPI aggregators.
	 *
	 * Side effects: writes generated files via [SchemaEmitSupport]; updates [writtenEndpointIds].
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 */
	private fun finalizeEndpointsAndAggregators(resolver: Resolver) {
		val endpointsToWrite = LinkedHashMap<String, EndpointModel>()
		
		for ((id, endpoint) in endpointModelsById) {
			if (id !in writtenEndpointIds) endpointsToWrite[id] = endpoint
		}
		
		for ((_, endpoint) in endpointsToWrite) {
			referenceVerifier.verifyEndpoint(
				endpoint = endpoint,
				schemasByQualifiedName = schemaModelsByFqcn,
				site = emit.resolveEndpointContainingFile(resolver, endpoint))
		}
		
		if (endpointsToWrite.isNotEmpty()) {
			emit.writeEndpointSchemaFiles(
				resolver = resolver,
				endpoints = endpointsToWrite.values.toList(),
				allEndpoints = endpointModelsById.values)
			writtenEndpointIds += endpointsToWrite.keys
		}

		if (schemaModelsByFqcn.isNotEmpty()) {
			val roots = schemaRootFqcns.mapNotNull { resolveClass(resolver, it) }
			emit.writeObjectSchemaAggregator(schemaModelsByFqcn, roots)
		}
		
		if (endpointModelsById.isNotEmpty()) {
			emit.writeEndpointSchemaAggregator(resolver, endpointModelsById.values.toList())
		}
	}

	/**
	 * Resolves [fqcn] to a [KSClassDeclaration], cached for the current [process] call.
	 *
	 * Side effects: mutates [classByFqcn].
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @param fqcn Fully qualified class name to resolve.
	 * @return Class declaration, or `null` when [fqcn] is unknown this round.
	 */
	private fun resolveClass(resolver: Resolver, fqcn: String): KSClassDeclaration? {
		if (classByFqcn.containsKey(fqcn)) return classByFqcn[fqcn]
		val resolved = resolver.getClassDeclarationByName(resolver.getKSNameFromString(fqcn))
		classByFqcn[fqcn] = resolved
		return resolved
	}
}
