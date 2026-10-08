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

import io.ghaylan.validata.processor.compat.KotlinStringLiteral
import io.ghaylan.validata.processor.fqns.CodegenFqns
import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.processor.naming.GeneratedPackageNamer
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * Renders [EndpointModel]s into Kotlin source: one factory file per endpoint, plus one thin
 * request-schema SPI aggregator per compilation unit.
 *
 * Package: `<controllerPkg>.ghaylan.validata` for factories;
 * `<commonPrefix>.ghaylan.validata` for [GeneratedNames.REQUEST_SCHEMAS_MODULE].
 *
 * Import FQCNs come from [CodegenFqns] (verified by `HardcodedFqcnExistenceTest`).
 */
internal object EndpointSchemaCodeWriter {
	
	/** File name (without extension) for [model]'s generated endpoint factory file. */
	fun endpointFileName(model: EndpointModel): String =
		GeneratedNames.endpointFileName(model.functionQualifiedName, model.identifier)
	
	/** Name of the top-level function returning [model]'s generated `EndpointSchema`. */
	fun factoryFunctionName(model: EndpointModel): String =
		GeneratedNames.endpointFactoryName(model.functionQualifiedName, model.identifier)
	
	/**
	 * Writes one endpoint factory file.
	 *
	 * @param packageName package of the generated file
	 * @param model endpoint to emit
	 * @return complete Kotlin source
	 */
	fun writeEndpoint(
		packageName: String,
		model: EndpointModel
	): String {
		val b = KotlinSourceBuilder(packageName)
		val imports = ImportScope(b)
		b.addFileAnnotation("@file:Suppress(\"UNCHECKED_CAST\")")
		b.addImport(CodegenFqns.ENDPOINT_SCHEMA)
		b.addImport(CodegenFqns.ENDPOINT_ARGUMENT_SLOT)
		b.addImport(CodegenFqns.ENDPOINT_ARGUMENT_KIND)
		b.addImport(CodegenFqns.OBJECT_SCHEMA)
		addUsageImports(b, model)
		val funName = factoryFunctionName(model)
		b.line("/** Generated request schema for [${model.functionQualifiedName}]. */")
		b.block("fun $funName(): EndpointSchema") {
			line("return EndpointSchema(")
			indented {
				line("id = \"${escape(model.identifier)}\",")
				writeSection("queryParams", model.parameters.filter { it.kind == EndpointArgumentKind.QUERY }, imports)
				writeSection("headers", model.parameters.filter { it.kind == EndpointArgumentKind.HEADER }, imports)
				writeSection("pathVariables", model.parameters.filter { it.kind == EndpointArgumentKind.PATH }, imports)
				writeBody(model, imports)
				line("oneErrorPerParam = ${model.oneErrorPerParam},")
				line("failFast = ${model.failFast},")
				val groups = model.groupsFqcn.joinToString(", ") { "${imports.ref(it)}::class" }
				line("groups = setOf($groups),")
				writeArgumentLayout(model)
			}
			line(")")
		}
		return b.build(model.functionQualifiedName)
	}
	
	private fun addUsageImports(
		b: KotlinSourceBuilder,
		model: EndpointModel
	) {
		val params = model.parameters
		val transportParams = params.filter {
			it.kind == EndpointArgumentKind.QUERY || it.kind == EndpointArgumentKind.HEADER || it.kind == EndpointArgumentKind.PATH
		}
		val body = params.firstOrNull { it.kind == EndpointArgumentKind.BODY }
		val needsPropertySpec = transportParams.isNotEmpty() || body?.bodyIsCollection == true
		if (needsPropertySpec) b.addImport(CodegenFqns.PROPERTY_SPEC)
		if (params.any { it.errorDocs.isNotEmpty() }) {
			b.addImport(CodegenFqns.SCHEMA_ERROR_DOC)
		}
		val needsLookup = body != null || transportParams.any { shapeNeedsLookup(it.shape) }
		if (needsLookup) b.addImport(CodegenFqns.GENERATED_SCHEMA_LOOKUP)
		val allConstraints = collectConstraints(params)
		if (ConstraintSourceRenderer.usesCompiledConstraints(allConstraints)) {
			b.addImport(CodegenFqns.COMPILED_CONSTRAINTS)
		}
		if (ConstraintSourceRenderer.usesComposition(allConstraints)) {
			b.addImport(CodegenFqns.COMPOSITION_CONSTRAINT)
		}
		for (param in transportParams) addShapeImports(b, param.shape)
		if (body?.bodyIsCollection == true) {
			b.addImport(CodegenFqns.ITERABLE_SHAPE)
			b.addImport(CodegenFqns.OBJECT_REF_SHAPE)
		}
	}
	
	private fun shapeNeedsLookup(shape: ShapeModel): Boolean =
		when (shape) {
			is ObjectRefShapeModel -> true
			is IterableShapeModel -> shapeNeedsLookup(shape.element)
			is MapShapeModel -> shapeNeedsLookup(shape.key) || shapeNeedsLookup(shape.value)
			else -> false
		}
	
	private fun addShapeImports(
		b: KotlinSourceBuilder,
		shape: ShapeModel
	) {
		when (shape) {
			is ScalarShapeModel -> {
				b.addImport(CodegenFqns.SCALAR_SHAPE)
				b.addImport(CodegenFqns.SCALAR_KIND)
			}
			
			is DynamicShapeModel -> b.addImport(CodegenFqns.DYNAMIC_SHAPE)
			is IterableShapeModel -> {
				b.addImport(CodegenFqns.ITERABLE_SHAPE)
				addShapeImports(b, shape.element)
			}
			
			is MapShapeModel -> {
				b.addImport(CodegenFqns.MAP_SHAPE)
				addShapeImports(b, shape.key)
				addShapeImports(b, shape.value)
			}
			
			is ObjectRefShapeModel -> b.addImport(CodegenFqns.OBJECT_REF_SHAPE)
		}
	}
	
	private fun collectConstraints(params: List<EndpointParameterModel>): List<ConstraintModel> {
		val out = mutableListOf<ConstraintModel>()
		fun fromShape(shape: ShapeModel) {
			out += shape.constraints
			for (constraint in shape.constraints) {
				constraint.compositionChildren?.let { out += it }
			}
			when (shape) {
				is IterableShapeModel -> fromShape(shape.element)
				is MapShapeModel -> {
					fromShape(shape.key)
					fromShape(shape.value)
				}
				
				else -> Unit
			}
		}
		for (param in params) {
			out += param.constraints
			for (c in param.constraints) {
				c.compositionChildren?.let { out += it }
			}
			fromShape(param.shape)
		}
		return out
	}
	
	private fun KotlinSourceBuilder.writeArgumentLayout(model: EndpointModel) {
		if (model.argumentLayout.isEmpty()) {
			line("argumentLayout = emptyList(),")
			return
		}
		line("argumentLayout = listOf(")
		indented {
			for (model in model.argumentLayout) {
				val nameLit = if (model.name.isEmpty()) "" else ", name = \"${escape(model.name)}\""
				line("EndpointArgumentSlot(kind = EndpointArgumentKind.${model.kind.name}$nameLit),")
			}
		}
		line("),")
	}
	
	/**
	 * Thin SPI aggregator for all endpoints in this compilation unit.
	 */
	fun writeAggregator(
		packageName: String,
		models: List<EndpointModel>,
		moduleFallback: String = packageName,
	): String {
		val b = KotlinSourceBuilder(packageName)
		b.addImport(CodegenFqns.REQUEST_SCHEMA_MODULE)
		b.addImport(CodegenFqns.ENDPOINT_SCHEMA)
		for (model in models) {
			val peerPkg = GeneratedPackageNamer.typePackage(model.packageName, moduleFallback)
			if (peerPkg != packageName) {
				b.addImport("${peerPkg}.${factoryFunctionName(model)}")
			}
		}
		
		b.line("/**")
		b.line(" * Generated request-schema module for this compilation unit.")
		b.line(" */")
		b.block("class ${GeneratedNames.REQUEST_SCHEMAS_MODULE} : RequestSchemaModule") {
			block("override fun schemas(): Map<String, EndpointSchema>") {
				if (models.isEmpty()) {
					line("return emptyMap()")
				}
				else {
					line("return mapOf(")
					indented {
						models.forEachIndexed { index, model ->
							val comma = if (index == models.lastIndex) "" else ","
							line("\"${escape(model.identifier)}\" to ${factoryFunctionName(model)}()$comma")
						}
					}
					line(")")
				}
			}
		}
		return b.build("module aggregator (${models.size} endpoint(s))")
	}
	
	private fun KotlinSourceBuilder.writeSection(
		fieldName: String,
		params: List<EndpointParameterModel>,
		imports: ImportScope,
	) {
		if (params.isEmpty()) {
			line("$fieldName = null,")
			return
		}
		line("$fieldName = ObjectSchema(")
		indented {
			line("type = Map::class.java,")
			line("properties = listOf(")
			indented {
				params.sortedBy { it.resolvedName }.forEachIndexed { index, param ->
					val comma = if (index == params.lastIndex) "" else ","
					line("PropertySpec(")
					indented {
						line("declaredName = \"${escape(param.declaredName)}\",")
						line("externalName = \"${escape(param.resolvedName)}\",")
						line("shape = ${renderShape(param.shape, imports)},")
						line("read = ${imports.shortenExpression(param.readerExpr)},")
						line("constraints = ${ConstraintSourceRenderer.renderConstraints(param.constraints, imports)},")
						if (param.errorDocs.isNotEmpty()) {
							line("errorDocs = ${ErrorDocSourceRenderer.renderErrorDocs(param.errorDocs)},")
						}
					}
					line(")$comma")
				}
			}
			line("),")
		}
		line("),")
	}
	
	private fun KotlinSourceBuilder.writeBody(
		model: EndpointModel,
		imports: ImportScope
	) {
		val body = model.parameters.firstOrNull { it.kind == EndpointArgumentKind.BODY }
		if (body == null) {
			line("requestBody = null,")
			return
		}
		when {
			body.bodyIsCollection && body.bodyElementTypeQualifiedName != null -> {
				val elem = imports.ref(body.bodyElementTypeQualifiedName)
				val bodyType = body.bodyTypeQualifiedName?.let { imports.ref(it) }
				line("requestBody = ObjectSchema(")
				indented {
					line("type = $bodyType::class.java,")
					line("properties = listOf(")
					indented {
						line("PropertySpec(")
						indented {
							line("declaredName = \"\",")
							line("externalName = \"\",")
							line("shape = IterableShape(ObjectRefShape(lazy { GeneratedSchemaLookup.requireGeneratedSchema($elem::class.java) })),")
							line("read = { it },")
							line("constraints = emptyList(),")
						}
						line("),")
					}
					line("),")
				}
				line("),")
			}
			
			body.bodyTypeQualifiedName != null -> {
				val typeRef = imports.ref(body.bodyTypeQualifiedName)
				line("requestBody = GeneratedSchemaLookup.requireGeneratedSchema($typeRef::class.java),")
			}
			
			else -> line("requestBody = null,")
		}
	}
	
	private fun renderShape(
		shape: ShapeModel,
		imports: ImportScope
	): String =
		when (shape) {
			is ScalarShapeModel -> "ScalarShape(ScalarKind.${shape.kind}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is DynamicShapeModel -> "DynamicShape(${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is IterableShapeModel -> "IterableShape(${renderShape(shape.element, imports)}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is MapShapeModel -> "MapShape(${renderShape(shape.key, imports)}, ${renderShape(shape.value, imports)}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is ObjectRefShapeModel -> {
				val typeRef = imports.ref(shape.typeQualifiedName)
				"ObjectRefShape(lazy { GeneratedSchemaLookup.requireGeneratedSchema($typeRef::class.java) }, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			}
		}
	
	private fun escape(s: String): String =
		KotlinStringLiteral.escape(s)
}
