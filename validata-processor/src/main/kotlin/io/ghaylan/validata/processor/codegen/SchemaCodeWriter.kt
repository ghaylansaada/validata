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
import io.ghaylan.validata.schema.spi.ObjectSchemaModule

/**
 * Emits object-schema factories and the thin [ObjectSchemaModule] aggregator.
 *
 * ## Layout (SRP)
 * - One file per `@Validatable` type: `object <Type>Schema { val schema by lazy; fun build() }`
 * - One aggregator per compilation unit: `ObjectSchemasModule` under the module package
 *
 * Object refs:
 * - Peer in this compilation unit → `lazy { PeerSchema.build() }` (cycle-safe; `build()` is memoized)
 * - Peer missing from this unit → `lazy { GeneratedSchemaLookup.requireGeneratedSchema(...) }`
 *   (cross-module; never emit an empty ObjectSchema)
 */
internal object SchemaCodeWriter {
	
	/**
	 * One schema factory file for [model].
	 *
	 * @param packageName `<typePkg>.ghaylan.validata`
	 * @param model schema to emit
	 * @param schemasByQualifiedName all schemas in this round (object-ref / subtype resolution)
	 * @param moduleFallback module package used when resolving peer type packages
	 */
	fun writeSchema(
		packageName: String,
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		moduleFallback: String,
	): String {
		val b = KotlinSourceBuilder(packageName)
		val imports = ImportScope(b)
		b.addFileAnnotation("@file:Suppress(\"UNCHECKED_CAST\", \"RedundantVisibilityModifier\")")
		b.addImport(CodegenFqns.OBJECT_SCHEMA)
		addUsageImports(b, model, schemasByQualifiedName)
		importPeerSchemas(b, model, schemasByQualifiedName, moduleFallback, packageName)
		val objectName = GeneratedNames.schemaObjectName(model.packageName, model.qualifiedName)
		val typeRef = imports.ref(model.qualifiedName)
		
		b.line("/**")
		b.line(" * Generated ObjectSchema factory for [$typeRef].")
		b.line(" *")
		b.line(" * Do not edit — re-run the build after changing the source type.")
		b.line(" */")
		b.block("object $objectName") {
			block("val schema: ObjectSchema by lazy") {
				line("ObjectSchema(")
				indented {
					line("type = $typeRef::class.java,")
					if (model.isPolymorphicRoot) {
						writeSubtypes(model, schemasByQualifiedName, imports)
					}
					else {
						writeProperties(model, schemasByQualifiedName, imports)
					}
				}
				line(")")
			}
			line()
			line("fun build(): ObjectSchema = schema")
		}
		return b.build(model.qualifiedName)
	}
	
	/**
	 * Thin SPI aggregator that registers every schema factory in this compilation unit.
	 */
	fun writeAggregator(
		packageName: String,
		models: List<SchemaModel>,
		moduleFallback: String,
	): String {
		val b = KotlinSourceBuilder(packageName)
		val imports = ImportScope(b)
		b.addImport(CodegenFqns.OBJECT_SCHEMA)
		b.addImport(CodegenFqns.OBJECT_SCHEMA_MODULE)
		for (model in models) {
			val peerPkg = GeneratedPackageNamer.typePackage(model.packageName, moduleFallback)
			if (peerPkg != packageName) {
				b.addImport("${peerPkg}.${GeneratedNames.schemaObjectName(model.packageName, model.qualifiedName)}")
			}
			imports.ref(model.qualifiedName)
		}
		
		b.line("/**")
		b.line(" * Generated ObjectSchemaModule for this compilation unit.")
		b.line(" *")
		b.line(" * Registers every `@Validatable` schema factory emitted in this module.")
		b.line(" */")
		b.block("class ${GeneratedNames.OBJECT_SCHEMAS_MODULE} : ObjectSchemaModule") {
			block("override fun schemas(): Map<Class<*>, ObjectSchema>") {
				if (models.isEmpty()) {
					line("return emptyMap()")
				}
				else {
					line("return mapOf(")
					indented {
						models.forEachIndexed { index, model ->
							val comma = if (index == models.lastIndex) "" else ","
							val factory = GeneratedNames.schemaObjectName(model.packageName, model.qualifiedName)
							val typeRef = imports.ref(model.qualifiedName)
							line("$typeRef::class.java to $factory.build()$comma")
						}
					}
					line(")")
				}
			}
		}
		return b.build("module aggregator (${models.size} schema(s))")
	}
	
	private fun addUsageImports(
		b: KotlinSourceBuilder,
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
	) {
		if (model.properties.isNotEmpty()) {
			b.addImport(CodegenFqns.PROPERTY_SPEC)
		}
		if (model.properties.any { it.errorDocs.isNotEmpty() }) {
			b.addImport(CodegenFqns.SCHEMA_ERROR_DOC)
		}
		if (needsGeneratedLookup(model, schemasByQualifiedName)) {
			b.addImport(CodegenFqns.GENERATED_SCHEMA_LOOKUP)
		}
		val allConstraints = collectConstraints(model)
		if (ConstraintSourceRenderer.usesCompiledConstraints(allConstraints)) {
			b.addImport(CodegenFqns.COMPILED_CONSTRAINTS)
		}
		if (ConstraintSourceRenderer.usesComposition(allConstraints)) {
			b.addImport(CodegenFqns.COMPOSITION_CONSTRAINT)
		}
		fun shapes(shape: ShapeModel) {
			when (shape) {
				is ScalarShapeModel -> {
					b.addImport(CodegenFqns.SCALAR_SHAPE)
					b.addImport(CodegenFqns.SCALAR_KIND)
				}
				
				is DynamicShapeModel -> b.addImport(CodegenFqns.DYNAMIC_SHAPE)
				is IterableShapeModel -> {
					b.addImport(CodegenFqns.ITERABLE_SHAPE)
					shapes(shape.element)
				}
				
				is MapShapeModel -> {
					b.addImport(CodegenFqns.MAP_SHAPE)
					shapes(shape.key)
					shapes(shape.value)
				}
				
				is ObjectRefShapeModel -> b.addImport(CodegenFqns.OBJECT_REF_SHAPE)
			}
		}
		for (property in model.properties) shapes(property.shape)
	}
	
	private fun collectConstraints(model: SchemaModel): List<ConstraintModel> {
		val out = mutableListOf<ConstraintModel>()
		fun fromShape(shape: ShapeModel) {
			out += shape.constraints
			when (shape) {
				is IterableShapeModel -> fromShape(shape.element)
				is MapShapeModel -> {
					fromShape(shape.key)
					fromShape(shape.value)
				}
				
				is ObjectRefShapeModel, is ScalarShapeModel, is DynamicShapeModel -> Unit
			}
			for (constraint in shape.constraints) {
				constraint.compositionChildren?.let { out += it }
			}
		}
		for (property in model.properties) {
			out += property.constraints
			for (constraint in property.constraints) {
				constraint.compositionChildren?.let { out += it }
			}
			fromShape(property.shape)
		}
		return out
	}
	
	private fun KotlinSourceBuilder.writeSubtypes(
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		imports: ImportScope,
	) {
		line("properties = emptyList(),")
		line("subtypes = mapOf(")
		indented {
			model.subtypeQualifiedNames.forEachIndexed { i, sub ->
				val comma = if (i == model.subtypeQualifiedNames.lastIndex) "" else ","
				val peer = schemasByQualifiedName[sub]
				val typeRef = imports.ref(sub)
				val schemaExpr = if (peer != null) {
					"${GeneratedNames.schemaObjectName(peer.packageName, peer.qualifiedName)}.build()"
				}
				else {
					"GeneratedSchemaLookup.requireGeneratedSchema($typeRef::class.java)"
				}
				line("$typeRef::class.java to $schemaExpr$comma")
			}
		}
		line(")")
	}
	
	private fun KotlinSourceBuilder.writeProperties(
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		imports: ImportScope,
	) {
		line("properties = listOf(")
		indented {
			model.properties.forEachIndexed { i, prop ->
				val comma = if (i == model.properties.lastIndex) "" else ","
				line("PropertySpec(")
				indented {
					line("declaredName = \"${KotlinStringLiteral.escape(prop.declaredName)}\",")
					line("externalName = \"${KotlinStringLiteral.escape(prop.externalName)}\",")
					line("shape = ${renderShape(prop.shape, schemasByQualifiedName, imports)},")
					line("read = ${imports.shortenExpression(prop.readerExpr)},")
					line("constraints = ${ConstraintSourceRenderer.renderConstraints(prop.constraints, imports)},")
					if (prop.errorDocs.isNotEmpty()) {
						line("errorDocs = ${ErrorDocSourceRenderer.renderErrorDocs(prop.errorDocs)},")
					}
				}
				line(")$comma")
			}
		}
		line(")")
	}
	
	private fun importPeerSchemas(
		b: KotlinSourceBuilder,
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		moduleFallback: String,
		thisPackage: String,
	) {
		val peers = LinkedHashSet<SchemaModel>()
		fun collectFromShape(shape: ShapeModel) {
			when (shape) {
				is ObjectRefShapeModel -> schemasByQualifiedName[shape.typeQualifiedName]?.let { peers += it }
				is IterableShapeModel -> collectFromShape(shape.element)
				is MapShapeModel -> {
					collectFromShape(shape.key)
					collectFromShape(shape.value)
				}
				
				else -> Unit
			}
		}
		for (property in model.properties) collectFromShape(property.shape)
		for (sub in model.subtypeQualifiedNames) {
			schemasByQualifiedName[sub]?.let { peers += it }
		}
		for (peer in peers) {
			if (peer.qualifiedName == model.qualifiedName) continue
			val peerPkg = GeneratedPackageNamer.typePackage(peer.packageName, moduleFallback)
			if (peerPkg != thisPackage) {
				b.addImport("${peerPkg}.${GeneratedNames.schemaObjectName(peer.packageName, peer.qualifiedName)}")
			}
		}
	}
	
	private fun needsGeneratedLookup(
		model: SchemaModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
	): Boolean {
		fun shapeNeeds(shape: ShapeModel): Boolean =
			when (shape) {
				is ObjectRefShapeModel -> shape.typeQualifiedName !in schemasByQualifiedName
				is IterableShapeModel -> shapeNeeds(shape.element)
				is MapShapeModel -> shapeNeeds(shape.key) || shapeNeeds(shape.value)
				else -> false
			}
		
		val propertiesNeedLookup = model.properties.any { shapeNeeds(it.shape) }
		val subtypesNeedLookup = model.subtypeQualifiedNames.any {
			it !in schemasByQualifiedName
		}
		
		return propertiesNeedLookup || subtypesNeedLookup
	}
	
	private fun renderShape(
		shape: ShapeModel,
		schemasByQualifiedName: Map<String, SchemaModel>,
		imports: ImportScope,
	): String =
		when (shape) {
			is ScalarShapeModel -> "ScalarShape(ScalarKind.${shape.kind}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is DynamicShapeModel -> "DynamicShape(${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is IterableShapeModel -> "IterableShape(${renderShape(shape.element, schemasByQualifiedName, imports)}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is MapShapeModel -> "MapShape(${renderShape(shape.key, schemasByQualifiedName, imports)}, ${renderShape(shape.value, schemasByQualifiedName, imports)}, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			is ObjectRefShapeModel -> {
				val peer = schemasByQualifiedName[shape.typeQualifiedName]
				val call = if (peer != null) {
					"${GeneratedNames.schemaObjectName(peer.packageName, peer.qualifiedName)}.build()"
				}
				else {
					val typeRef = imports.ref(shape.typeQualifiedName)
					"GeneratedSchemaLookup.requireGeneratedSchema($typeRef::class.java)"
				}
				"ObjectRefShape(lazy { $call }, ${ConstraintSourceRenderer.renderConstraints(shape.constraints, imports)})"
			}
		}
}
