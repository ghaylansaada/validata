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

import io.ghaylan.validata.processor.fqns.CodegenFqns
import io.ghaylan.validata.processor.model.ConstraintCatalogAnnotationModel
import io.ghaylan.validata.processor.model.ConstraintCatalogEntryModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.processor.naming.GeneratedPackageNamer

/**
 * Renders constraint-catalog helpers: one file per `@Constraint` annotation, plus one thin SPI
 * aggregator per compilation unit.
 */
internal object ConstraintCatalogCodeWriter {
	
	/**
	 * Generates the per-annotation helper that returns that annotation's catalog entries.
	 *
	 * @param packageName `<annotationPkg>.ghaylan.validata`
	 * @param model annotation + its validator bindings
	 */
	fun writeAnnotationEntries(
		packageName: String,
		model: ConstraintCatalogAnnotationModel
	): String {
		val builder = KotlinSourceBuilder(packageName)
		builder.addImport(CodegenFqns.CONSTRAINT_CATALOG_ENTRY)
		builder.addImport(CodegenFqns.REFLECTION_UTILS)
		for (model in model.entries) {
			builder.addImport(model.annotationFqcn)
			builder.addImport(model.metadataFqcn)
			builder.addImport(model.valueTypeFqcn)
			builder.addImport(model.validatorFqcn)
		}
		val funName = entriesFunctionName(model.annotationSimpleName)
		builder.line("/** Generated catalog entries for [${model.annotationSimpleName}]. */")
		builder.block("fun $funName(): List<ConstraintCatalogEntry>") {
			line("return listOf(")
			model.entries.forEachIndexed { index, entry ->
				writeEntryLiteral(entry, trailingComma = index != model.entries.lastIndex)
			}
			line(")")
		}
		return builder.build(model.annotationFqcn)
	}
	
	/**
	 * Thin SPI aggregator implementing `ConstraintCatalog` from **validata-core**.
	 *
	 * @param packageName module package (`….ghaylan.validata`)
	 * @param models all annotations in this compilation, already sorted
	 * @param moduleFallback used to resolve per-annotation helper packages
	 */
	fun writeAggregator(
		packageName: String,
		models: List<ConstraintCatalogAnnotationModel>,
		moduleFallback: String = packageName,
	): String {
		val builder = KotlinSourceBuilder(packageName)
		builder.addImport(CodegenFqns.CONSTRAINT_CATALOG)
		builder.addImport(CodegenFqns.CONSTRAINT_CATALOG_ENTRY)
		for (model in models) {
			val annPkg = model.annotationFqcn.substringBeforeLast('.', "")
			val peerPkg = GeneratedPackageNamer.typePackage(annPkg, moduleFallback)
			if (peerPkg != packageName) {
				builder.addImport("${peerPkg}.${entriesFunctionName(model.annotationSimpleName)}")
			}
		}
		
		builder.line("/** Generated constraint catalog for this compilation unit. */")
		builder.block("class ${GeneratedNames.CONSTRAINT_CATALOG_MODULE} : ConstraintCatalog") {
			block("override fun entries(): List<ConstraintCatalogEntry>") {
				if (models.isEmpty()) {
					line("return emptyList()")
				}
				else {
					line("return buildList {")
					indented {
						for (model in models) {
							line("addAll(${entriesFunctionName(model.annotationSimpleName)}())")
						}
					}
					line("}")
				}
			}
		}
		return builder.build("module aggregator (${models.size} constraint annotation(s))")
	}
	
	/**
	 * Name of the top-level function returning [annotationSimpleName]'s catalog entries, as
	 * called from both the annotation's own file and the aggregator.
	 */
	fun entriesFunctionName(annotationSimpleName: String): String =
		GeneratedNames.catalogEntriesFunctionName(annotationSimpleName)
	
	/** File name (without extension) for [annotationSimpleName]'s generated catalog-entries file. */
	fun annotationFileName(annotationSimpleName: String): String =
		GeneratedNames.catalogEntriesFileName(annotationSimpleName)
	
	private fun KotlinSourceBuilder.writeEntryLiteral(
		entry: ConstraintCatalogEntryModel,
		trailingComma: Boolean,
	) {
		val factory = if (entry.objectSingleton) {
			"{ ${simpleName(entry.validatorFqcn)} }"
		}
		else {
			"{ ${simpleName(entry.validatorFqcn)}() }"
		}
		val comma = if (trailingComma) "," else ""
		line("ConstraintCatalogEntry(")
		indented {
			line("annotationType = ${simpleName(entry.annotationFqcn)}::class.java,")
			line("metadataType = ${simpleName(entry.metadataFqcn)}::class.java,")
			line("valueType = ReflectionUtils.infoFromClass(${simpleName(entry.valueTypeFqcn)}::class.java),")
			line("validatorType = ${simpleName(entry.validatorFqcn)}::class.java,")
			line("defaultInstanceFactory = $factory,")
		}
		line(")$comma")
	}
	
	private fun simpleName(fqcn: String): String =
		fqcn.substringAfterLast('.')
}
