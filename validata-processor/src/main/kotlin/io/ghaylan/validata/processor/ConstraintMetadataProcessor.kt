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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.ghaylan.validata.processor.analyze.ConstraintMetadataModelBuilder
import io.ghaylan.validata.processor.analyze.MetadataFqcnResolver
import io.ghaylan.validata.processor.codegen.ConstraintMetadataCodeWriter
import io.ghaylan.validata.processor.compat.ProcessorRoundCache
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.GeneratedMetadataClassModel

/**
 * Generates `*Constraint` metadata classes for `@Constraint` annotations that opt into Option 2
 * via `@ConstraintMessage` / `@ConstraintGroups` role markers.
 *
 * Annotations without role markers keep hand-written metadata (dual-run). This processor may run
 * every KSP round so newly generated metadata is visible to catalog/schema processors afterward.*
 * 
 * @author Ghaylan Saada
 */
class ConstraintMetadataProcessor(
	environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

	/**
	 * KSP logger for metadata model build diagnostics.
	 */
	private val logger = environment.logger

	/**
	 * KSP file writer for generated metadata data classes.
	 */
	private val codeGenerator = environment.codeGenerator

	/**
	 * Builds [GeneratedMetadataClassModel] for Option 2 annotations.
	 */
	private val modelBuilder = ConstraintMetadataModelBuilder(logger)
	/**
	 * FQCNs of metadata classes already written this compilation.
	 */
	private val generatedFqcn = mutableSetOf<String>()

	/**
	 * Generates a `*Constraint` metadata class for each `@Constraint` annotation visible to
	 * [resolver] that opts into Option 2, skipping annotations already generated in a prior round.
	 *
	 * Side effects: writes generated Kotlin files via [codeGenerator]; mutates [generatedFqcn].
	 * Unlike [ConstraintCatalogProcessor] and [SchemaProcessor], this processor runs on every KSP
	 * round (no single-invocation guard) so metadata it generates is visible to those processors
	 * in the same or a later round.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @return Always an empty list; nothing is deferred to later rounds.
	 */
	override fun process(resolver: Resolver): List<KSAnnotated> {
		ProcessorRoundCache.forceClear()
		ProcessorRoundCache.begin()
		try {
			val annotations = resolver
				.getSymbolsWithAnnotation(ProcessorFqns.CONSTRAINT)
				.filterIsInstance<KSClassDeclaration>()
				.toList()
			if (annotations.isEmpty()) return emptyList()

			for (annotationDecl in annotations) {
				// Skip re-building models already emitted in an earlier round.
				val previewFqcn = MetadataFqcnResolver.conventionFqcn(annotationDecl)
				if (previewFqcn in generatedFqcn) continue

				val model = modelBuilder.buildIfOptedIn(annotationDecl) ?: continue
				val fqcn = "${model.packageName}.${model.simpleName}"
				if (!generatedFqcn.add(fqcn)) continue

				val source = ConstraintMetadataCodeWriter.write(model)
				val file = annotationDecl.containingFile
				val deps = if (file != null) {
					Dependencies(aggregating = false, file)
				} else {
					Dependencies(aggregating = false)
				}
				codeGenerator.createNewFile(
					dependencies = deps,
					packageName = model.packageName,
					fileName = model.simpleName,
				).bufferedWriter().use { it.write(source) }
			}

			return emptyList()
		} finally {
			ProcessorRoundCache.end()
		}
	}

	/**
	 * Clears round cache when KSP finishes successfully.
	 *
	 * Side effects: clears [ProcessorRoundCache].
	 */
	override fun finish() {
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
}
