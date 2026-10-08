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

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import io.ghaylan.validata.processor.analyze.ConstraintCatalogModelBuilder
import io.ghaylan.validata.processor.codegen.ConstraintCatalogCodeWriter
import io.ghaylan.validata.processor.compat.ProcessorRoundCache
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.ConstraintCatalogAnnotationModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.processor.naming.GeneratedPackageNamer

/**
 * KSP [SymbolProcessor] that emits a `ConstraintCatalog` SPI implementation for every
 * `@Constraint`-annotated annotation in the current compilation.
 *
 * ## Where `ConstraintCatalog` lives
 *
 * The interface lives in **`validata-core`**. This processor does not depend on that module
 * (classpath cycle with `ksp(this)`); generated source references it via string FQCNs in
 * `CodegenFqns`.
 *
 * ## Output layout
 * - Per annotation: `<annotationPkg>.ghaylan.validata/<Annotation>ConstraintEntries.kt`
 * - Aggregator: `<commonPrefix>.ghaylan.validata/ConstraintCatalogModule.kt` + SPI resource
 *
 * Option 2: metadata FQCNs for role-marker annotations are resolved by naming convention when the
 * generated class is not visible yet. Models accumulate by annotation FQCN across KSP rounds
 * (FQCN + diagnostic path only — no [KSFile] retention). [filesByFqcn] is cleared and refilled
 * each [process] by re-resolving known annotation FQCNs via the current [Resolver]; per-annotation
 * entry files and [finish] aggregator dependencies use that round-local map.*
 * 
 * @author Ghaylan Saada
 */
class ConstraintCatalogProcessor(
	environment: SymbolProcessorEnvironment,
): SymbolProcessor {
	
	/**
	 * KSP logger for catalog model build diagnostics.	 
	 */
	private val logger = environment.logger
	
	/**
	 * KSP file writer for generated catalog sources and SPI resources.	 
	 */
	private val codeGenerator = environment.codeGenerator
	
	/**
	 * Builds [ConstraintCatalogAnnotationModel] instances from `@Constraint` annotations.	 
	 */
	private val modelBuilder = ConstraintCatalogModelBuilder(logger)
	
	/**
	 * Accumulated catalog models keyed by annotation FQCN (refreshed each round they appear).	 
	 */
	private val modelsByAnnotationFqcn = LinkedHashMap<String, ConstraintCatalogAnnotationModel>()
	
	/**
	 * Round-local annotation FQCN → [KSFile], cleared and rebuilt every [process] for all known
	 * FQCNs via the current resolver (never retained from a prior round's symbols).	 
	 */
	private val filesByFqcn = LinkedHashMap<String, KSFile>()
	
	/**
	 * Annotation FQCNs whose per-annotation entry files were already written.	 
	 */
	private val writtenAnnotationFqcns = linkedSetOf<String>()
	
	/**
	 * True after the catalog SPI aggregator and service resource were written.	 
	 */
	private var aggregatorWritten = false
	
	/**
	 * Discovers `@Constraint` annotations for this round, merges into the accumulated model map,
	 * and writes per-annotation entry files for newly seen FQCNs.
	 *
	 * Side effects: writes generated Kotlin files via [codeGenerator]; mutates accumulated maps.
	 * Aggregator + SPI are deferred to [finish] so later rounds can still contribute annotations.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @return Always an empty list; this processor defers nothing to later rounds.	 
	 */
	override fun process(resolver: Resolver): List<KSAnnotated> {
		ProcessorRoundCache.forceClear()
		ProcessorRoundCache.begin()
		try {
			val models = modelBuilder.buildAll(resolver, skipFqcns = writtenAnnotationFqcns)
			for (model in models) {
				modelsByAnnotationFqcn[model.annotationFqcn] = model
			}
			refreshFilesByFqcn(resolver)
			
			for (model in models) {
				if (model.annotationFqcn in writtenAnnotationFqcns) continue
				val modulePackage = GeneratedPackageNamer.modulePackage(
					modelsByAnnotationFqcn.values.map {
						it.annotationFqcn.substringBeforeLast('.', "")
					},
				)
				val annPkg = model.annotationFqcn.substringBeforeLast('.', "")
				val typePkg = GeneratedPackageNamer.typePackage(annPkg, modulePackage)
				val fileName = ConstraintCatalogCodeWriter.annotationFileName(model.annotationSimpleName)
				val source = ConstraintCatalogCodeWriter.writeAnnotationEntries(typePkg, model)
				val file = filesByFqcn[model.annotationFqcn]
				val deps = if (file != null) {
					Dependencies(aggregating = false, file)
				}
				else {
					Dependencies(aggregating = false)
				}
				codeGenerator.createNewFile(
					dependencies = deps,
					packageName = typePkg,
					fileName = fileName,
				).bufferedWriter().use { it.write(source) }
				writtenAnnotationFqcns += model.annotationFqcn
			}
			
			return emptyList()
		}
		finally {
			ProcessorRoundCache.end()
		}
	}
	
	/**
	 * Writes the aggregator + SPI once all rounds have contributed annotation entry files.
	 *
	 * Side effects: may write aggregator and service files via [codeGenerator]; clears
	 * [ProcessorRoundCache].
	 *
	 * Uses [filesByFqcn] from the last [process] (refreshed each round for all accumulated FQCNs).
	 * When no files are available, emits aggregating dependencies with an empty file array.	 
	 */
	override fun finish() {
		writeAggregatorIfNeeded()
		ProcessorRoundCache.forceClear()
	}
	
	/**
	 * Clears round cache after a KSP error.
	 *
	 * Side effects: clears [ProcessorRoundCache].	 
	 */
	override fun onError() {
		ProcessorRoundCache.forceClear()
	}
	
	/**
	 * Clears [filesByFqcn] and re-resolves a [KSFile] for every known annotation FQCN via [resolver].
	 *
	 * Side effects: mutates [filesByFqcn].
	 *
	 * @param resolver KSP symbol resolver for the current round.	 
	 */
	private fun refreshFilesByFqcn(resolver: Resolver) {
		filesByFqcn.clear()
		for (fqcn in modelsByAnnotationFqcn.keys) {
			val decl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(fqcn))
			val file = decl?.containingFile
			if (file != null) filesByFqcn[fqcn] = file
		}
	}
	
	/**
	 * Writes the catalog SPI aggregator and service resource when models exist and not yet written.
	 *
	 * Side effects: writes generated Kotlin and service files via [codeGenerator]; sets
	 * [aggregatorWritten].	 
	 */
	private fun writeAggregatorIfNeeded() {
		if (aggregatorWritten || modelsByAnnotationFqcn.isEmpty()) return
		aggregatorWritten = true
		val models = modelsByAnnotationFqcn.values.toList()
		val modulePackage = GeneratedPackageNamer.modulePackage(
			models.map { it.annotationFqcn.substringBeforeLast('.', "") },
		)
		val aggregatorSource = ConstraintCatalogCodeWriter.writeAggregator(
			packageName = modulePackage,
			models = models,
			moduleFallback = modulePackage,
		)
		val files = models.mapNotNull { filesByFqcn[it.annotationFqcn] }.toTypedArray()
		val aggregatorDeps = if (files.isNotEmpty()) {
			Dependencies(aggregating = true, *files)
		}
		else {
			Dependencies(aggregating = true)
		}
		codeGenerator.createNewFile(
			dependencies = aggregatorDeps,
			packageName = modulePackage,
			fileName = GeneratedNames.CONSTRAINT_CATALOG_MODULE,
		).bufferedWriter().use { it.write(aggregatorSource) }
		
		codeGenerator.createNewFile(
			dependencies = aggregatorDeps,
			packageName = "META-INF.services",
			fileName = ProcessorFqns.CONSTRAINT_CATALOG_SPI,
			extensionName = "",
		).bufferedWriter().use { out ->
			out.write("$modulePackage.${GeneratedNames.CONSTRAINT_CATALOG_MODULE}\n")
		}
	}
}
