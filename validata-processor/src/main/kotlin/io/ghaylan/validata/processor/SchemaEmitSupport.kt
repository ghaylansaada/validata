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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import io.ghaylan.validata.processor.codegen.EndpointPathConstantsCodeWriter
import io.ghaylan.validata.processor.codegen.EndpointSchemaCodeWriter
import io.ghaylan.validata.processor.codegen.FieldsObjectCodeWriter
import io.ghaylan.validata.processor.codegen.SchemaCodeWriter
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.EndpointModel
import io.ghaylan.validata.processor.model.SchemaModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.processor.naming.GeneratedPackageNamer

/**
 * File and SPI emission helpers for [SchemaProcessor].
 *
 * @param codeGenerator KSP file writer for generated Kotlin and service resources.
 * @param resolveClass Callback to resolve a type FQCN to its [KSClassDeclaration] this round.*
 * 
 * @author Ghaylan Saada
 */
internal class SchemaEmitSupport(
	private val codeGenerator: CodeGenerator,
	private val resolveClass: (Resolver, String) -> KSClassDeclaration?,
) {
	
	/**
	 * Writes per-type schema factory and Fields files for [byQualified].
	 *
	 * Side effects: writes generated Kotlin files via [CodeGenerator.createNewFile].
	 *
	 * @param resolver KSP symbol resolver for dependency file lookup.
	 * @param byQualified Newly ready schemas to emit this call.
	 * @param allSchemas Full schema graph for peer resolution in generated source.	 
	 */
	fun writeObjectSchemaFiles(
		resolver: Resolver,
		byQualified: LinkedHashMap<String, SchemaModel>,
		allSchemas: Map<String, SchemaModel>,
	) {
		val packages = (allSchemas.values + byQualified.values).map { it.packageName }
		val modulePackage = GeneratedPackageNamer.modulePackage(packages)
		val models = byQualified.values.sortedBy { it.qualifiedName }
		
		for (model in models) {
			val typePkg = GeneratedPackageNamer.typePackage(model.packageName, modulePackage)
			val root = resolveClass(resolver, model.qualifiedName)
			val containingFile = root?.containingFile
			val schemaDeps = if (containingFile != null) {
				Dependencies(aggregating = false, containingFile)
			}
			else {
				Dependencies(aggregating = false)
			}
			val schemaSource = SchemaCodeWriter.writeSchema(
				packageName = typePkg,
				model = model,
				schemasByQualifiedName = allSchemas,
				moduleFallback = modulePackage,
			)
			codeGenerator.createNewFile(
				dependencies = schemaDeps,
				packageName = typePkg,
				fileName = GeneratedNames.schemaFileName(model.packageName, model.qualifiedName),
			).bufferedWriter().use { it.write(schemaSource) }
			val fieldsSource = FieldsObjectCodeWriter.write(typePkg, model, allSchemas)
			codeGenerator.createNewFile(
				dependencies = schemaDeps,
				packageName = typePkg,
				fileName = FieldsObjectCodeWriter.fileName(model),
			).bufferedWriter().use { it.write(fieldsSource) }
		}
	}
	
	/**
	 * Writes the object-schema SPI aggregator and `META-INF/services` resource.
	 *
	 * Side effects: writes generated Kotlin and service files via [CodeGenerator.createNewFile].
	 *
	 * @param byQualified All schema models in this compilation unit.
	 * @param roots `@Validatable` class declarations for aggregating dependencies.	 
	 */
	fun writeObjectSchemaAggregator(
		byQualified: Map<String, SchemaModel>,
		roots: List<KSClassDeclaration>,
	) {
		val modulePackage = GeneratedPackageNamer.modulePackage(byQualified.values.map { it.packageName })
		val models = byQualified.values.sortedBy { it.qualifiedName }
		val aggregatorDeps = Dependencies(
			aggregating = true,
			*roots.mapNotNull { it.containingFile }.toTypedArray(),
		)
		val aggregatorSource = SchemaCodeWriter.writeAggregator(modulePackage, models, modulePackage)
		codeGenerator.createNewFile(
			dependencies = aggregatorDeps,
			packageName = modulePackage,
			fileName = GeneratedNames.OBJECT_SCHEMAS_MODULE,
		).bufferedWriter().use { it.write(aggregatorSource) }
		
		codeGenerator.createNewFile(
			dependencies = aggregatorDeps,
			packageName = "META-INF.services",
			fileName = ProcessorFqns.OBJECT_SCHEMA_MODULE_SPI,
			extensionName = "",
		).bufferedWriter().use { out ->
				out.write("$modulePackage.${GeneratedNames.OBJECT_SCHEMAS_MODULE}\n")
			}
	}
	
	/**
	 * Writes endpoint factory files (and flat path/query/header `Controller_method_` constants) for
	 * [endpoints].
	 *
	 * Side effects: writes generated Kotlin files via [CodeGenerator.createNewFile].
	 *
	 * @param resolver KSP symbol resolver for dependency file lookup.
	 * @param endpoints Newly ready endpoints to emit this call.
	 * @param allEndpoints Full endpoint set for module package computation.	 
	 */
	fun writeEndpointSchemaFiles(
		resolver: Resolver,
		endpoints: List<EndpointModel>,
		allEndpoints: Collection<EndpointModel>,
	) {
		val modulePackage = GeneratedPackageNamer.modulePackage(
			(allEndpoints + endpoints).map { it.packageName },
		)
		
		for (endpoint in endpoints) {
			val typePkg = GeneratedPackageNamer.typePackage(endpoint.packageName, modulePackage)
			val fileName = EndpointSchemaCodeWriter.endpointFileName(endpoint)
			val source = EndpointSchemaCodeWriter.writeEndpoint(typePkg, endpoint)
			val containingFile = resolveEndpointContainingFile(resolver, endpoint)
			val deps = if (containingFile != null) {
				Dependencies(aggregating = false, containingFile)
			}
			else {
				Dependencies(aggregating = false)
			}
			codeGenerator.createNewFile(
				dependencies = deps,
				packageName = typePkg,
				fileName = fileName,
			).bufferedWriter().use { it.write(source) }

			val pathConsts = EndpointPathConstantsCodeWriter.write(typePkg, endpoint)
			if (pathConsts != null) {
				codeGenerator.createNewFile(
					dependencies = deps,
					packageName = typePkg,
					fileName = EndpointPathConstantsCodeWriter.fileName(endpoint),
				).bufferedWriter().use { it.write(pathConsts) }
			}
		}
	}
	
	/**
	 * Writes the request-schema SPI aggregator and `META-INF/services` resource.
	 *
	 * Side effects: writes generated Kotlin and service files via [CodeGenerator.createNewFile].
	 *
	 * @param resolver KSP symbol resolver for dependency file lookup.
	 * @param endpoints All endpoint models in this compilation unit.	 
	 */
	fun writeEndpointSchemaAggregator(
		resolver: Resolver,
		endpoints: List<EndpointModel>
	) {
		val modulePackage = GeneratedPackageNamer.modulePackage(endpoints.map { it.packageName })
		val aggregatorSource = EndpointSchemaCodeWriter.writeAggregator(
			packageName = modulePackage,
			models = endpoints.sortedBy { it.identifier },
			moduleFallback = modulePackage,
		)
		val files = endpoints.mapNotNull { resolveEndpointContainingFile(resolver, it) }.toTypedArray()
		val aggregatorDeps = if (files.isNotEmpty()) {
			Dependencies(aggregating = true, *files)
		}
		else {
			Dependencies(aggregating = true)
		}
		codeGenerator.createNewFile(
			dependencies = aggregatorDeps,
			packageName = modulePackage,
			fileName = GeneratedNames.REQUEST_SCHEMAS_MODULE,
		).bufferedWriter().use { it.write(aggregatorSource) }
		
		codeGenerator.createNewFile(
			dependencies = aggregatorDeps,
			packageName = "META-INF.services",
			fileName = ProcessorFqns.REQUEST_SCHEMA_MODULE_SPI,
			extensionName = "",
		).bufferedWriter().use { out ->
			out.write("$modulePackage.${GeneratedNames.REQUEST_SCHEMAS_MODULE}\n")
		}
	}
	
	/**
	 * Re-resolves the declaring class's [KSFile] for [endpoint] from the current round's [resolver].
	 *
	 * Side effects: none.
	 *
	 * @param resolver KSP symbol resolver for the current round.
	 * @param endpoint Endpoint whose handler owner file to locate.
	 * @return Containing source file, or `null` when the owner cannot be resolved.	 
	 */
	fun resolveEndpointContainingFile(
		resolver: Resolver,
		endpoint: EndpointModel
	): KSFile? {
		val ownerFqcn = endpoint.functionQualifiedName.substringBeforeLast('.')
		return resolveClass(resolver, ownerFqcn)?.containingFile
	}
}
