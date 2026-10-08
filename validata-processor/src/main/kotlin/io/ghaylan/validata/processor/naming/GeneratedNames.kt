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
package io.ghaylan.validata.processor.naming

/**
 * Stable, human-readable names for generated types and files.
 *
 * Avoids FQCN-mangled identifiers (`Generatedio_acme_UserFields`) while remaining unique for
 * nested types and overloaded handlers.
 *
 * Takes primitives only — no dependency on `processor.model`.*
 * 
 * @author Ghaylan Saada
 */
internal object GeneratedNames {
	
	/**
	 * Relative type name within [packageName]: `User` or `Outer_Inner` for nested classes.
	 *
	 * Side effects: none.
	 *
	 * @param packageName Declaring package of [qualifiedName].
	 * @param qualifiedName Fully qualified type name.
	 * @return Underscore-separated relative simple name.	 
	 */
	fun relativeName(
		packageName: String,
		qualifiedName: String
	): String {
		val relative = if (packageName.isNotEmpty() && qualifiedName.startsWith("$packageName.")) {
			qualifiedName.removePrefix("$packageName.")
		}
		else {
			qualifiedName.substringAfterLast('.')
		}
		return relative.replace('.', '_')
	}
	
	/**
	 * Generated top-level object simple name for an `ObjectSchema` factory.
	 *
	 * Side effects: none.
	 *
	 * @param packageName Declaring package of [qualifiedName].
	 * @param qualifiedName Fully qualified validated type name.
	 * @return Object name such as `UserRequestSchema`.	 
	 */
	fun schemaObjectName(
		packageName: String,
		qualifiedName: String
	): String =
		relativeName(packageName, qualifiedName) + "Schema"
	
	/**
	 * File name (without extension) for a generated schema file.
	 *
	 * Side effects: none.
	 *
	 * @param packageName Declaring package of [qualifiedName].
	 * @param qualifiedName Fully qualified validated type name.
	 * @return File simple name matching [schemaObjectName].	 
	 */
	fun schemaFileName(
		packageName: String,
		qualifiedName: String
	): String =
		schemaObjectName(packageName, qualifiedName)
	
	/**
	 * Generated top-level object simple name for Fields path constants.
	 *
	 * Side effects: none.
	 *
	 * @param packageName Declaring package of [qualifiedName].
	 * @param qualifiedName Fully qualified validated type name.
	 * @return Object name such as `UserRequest_`.	 
	 */
	fun fieldsObjectName(
		packageName: String,
		qualifiedName: String
	): String =
		relativeName(packageName, qualifiedName) + "_"
	
	/**
	 * File name (without extension) for a generated Fields file.
	 *
	 * Side effects: none.
	 *
	 * @param packageName Declaring package of [qualifiedName].
	 * @param qualifiedName Fully qualified validated type name.
	 * @return File simple name matching [fieldsObjectName].	 
	 */
	fun fieldsFileName(
		packageName: String,
		qualifiedName: String
	): String =
		fieldsObjectName(packageName, qualifiedName)
	
	/**
	 * Readable endpoint factory base: `Owner_method` plus a short param fingerprint when the
	 * JVM signature has parameters.
	 *
	 * Side effects: none.
	 *
	 * @param functionQualifiedName Handler `Owner.method` qualified name.
	 * @param identifier Endpoint id including JVM param signature.
	 * @return Base name for file and factory functions.	 
	 */
	fun endpointBaseName(
		functionQualifiedName: String,
		identifier: String,
	): String {
		val method = functionQualifiedName.substringAfterLast('.')
		val ownerFq = functionQualifiedName.substringBeforeLast('.')
		val ownerSimple = ownerFq.substringAfterLast('.')
		val paramSig = identifier.substringAfter('(', "").removeSuffix(")")
		val suffix = if (paramSig.isBlank()) "" else "_" + shortFingerprint(paramSig)
		return "${ownerSimple}_${method}$suffix"
	}
	
	/**
	 * File name (without extension) for a generated endpoint factory file.
	 *
	 * Side effects: none.
	 *
	 * @param functionQualifiedName Handler qualified name.
	 * @param identifier Endpoint id string.
	 * @return File simple name ending in `Endpoint`.	 
	 */
	fun endpointFileName(
		functionQualifiedName: String,
		identifier: String
	): String =
		endpointBaseName(functionQualifiedName, identifier) + "Endpoint"
	
	/**
	 * Top-level function name returning a generated `EndpointSchema`.
	 *
	 * Side effects: none.
	 *
	 * @param functionQualifiedName Handler qualified name.
	 * @param identifier Endpoint id string.
	 * @return Factory function simple name.	 
	 */
	fun endpointFactoryName(
		functionQualifiedName: String,
		identifier: String
	): String =
		"build" + endpointBaseName(functionQualifiedName, identifier) + "Endpoint"

	/**
	 * Generated top-level object for path / query / header wire-path constants on one endpoint.
	 *
	 * @return Object name such as `UserController_lookup_1988a7ee_`.
	 */
	fun endpointPathConstantsObjectName(
		functionQualifiedName: String,
		identifier: String,
	): String =
		endpointBaseName(functionQualifiedName, identifier) + "_"

	/**
	 * File name (without extension) for endpoint path-constant sources.
	 *
	 * @return File simple name matching [endpointPathConstantsObjectName].
	 */
	fun endpointPathConstantsFileName(
		functionQualifiedName: String,
		identifier: String,
	): String =
		endpointPathConstantsObjectName(functionQualifiedName, identifier)

	/**
	 * Top-level function name returning [annotationSimpleName]'s catalog entries.
	 *
	 * Side effects: none.
	 *
	 * @param annotationSimpleName Unqualified `@Constraint` annotation name.
	 * @return Entries function simple name.	 
	 */
	fun catalogEntriesFunctionName(annotationSimpleName: String): String =
		"${annotationSimpleName}ConstraintEntries"
	
	/**
	 * File name (without extension) for [annotationSimpleName]'s generated catalog-entries file.
	 *
	 * Side effects: none.
	 *
	 * @param annotationSimpleName Unqualified `@Constraint` annotation name.
	 * @return File simple name matching [catalogEntriesFunctionName].	 
	 */
	fun catalogEntriesFileName(annotationSimpleName: String): String =
		catalogEntriesFunctionName(annotationSimpleName)
	
	/**
	 * Class name of the generated `ObjectSchemaModule` SPI aggregator for object schemas.
	 */
	const val OBJECT_SCHEMAS_MODULE = "ObjectSchemasModule"
	
	/**
	 * Class name of the generated `RequestSchemaModule` SPI aggregator for endpoint schemas.
	 */
	const val REQUEST_SCHEMAS_MODULE = "RequestSchemasModule"
	
	/**
	 * Class name of the generated `ConstraintCatalog` SPI aggregator.
	 */
	const val CONSTRAINT_CATALOG_MODULE = "ConstraintCatalogModule"
	
	/**
	 * Stable short fingerprint for overload disambiguation (not a cryptographic hash).
	 *
	 * Side effects: none.
	 *
	 * @param text JVM parameter signature text to hash.
	 * @return Eight-digit lowercase hex fingerprint.	 
	 */
	fun shortFingerprint(text: String): String {
		var h = 0x811c9dc5.toInt()
		for (ch in text) {
			h = h xor ch.code
			h *= 0x01000193
		}
		return (h.toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')
	}
}
