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
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.fqns.OpenApiPresentationFqns
import io.ghaylan.validata.processor.model.SchemaErrorDocModel
import io.ghaylan.validata.processor.verify.ApiErrorCatalogVerifier

/**
 * Reads OpenAPI presentation annotations by FQN and builds docs IR models.
 *
 * Missing openapi types on the classpath yield empty results. Invalid catalogs are reported via
 * verifiers and omitted from the returned models.
 *
 * When the annotation `message` is blank, attempts to fill it from the catalog enum entry
 * (`code` / `message` properties on catalog enums) via reflection; OpenAPI resolves again at
 * docs time when the class was not yet loadable during KSP.
 *
 * Annotation FQCNs and parameter names come from [OpenApiPresentationFqns] — do not hardcode them
 * at call sites.
 *
 * @property logger Diagnostics sink.
 * @property catalogVerifier enum catalog contract checks for `@ApiError`*
 * 
 * @author Ghaylan Saada
 */
internal class ApiErrorAnnotationParser(
	private val logger: KSPLogger,
	private val catalogVerifier: ApiErrorCatalogVerifier = ApiErrorCatalogVerifier(logger),
) {
	
	/**
	 * Collects stacked `@ApiError` markers on [annotated] (field, property, or parameter).
	 *
	 * @param annotated Annotated member.
	 * @return Validated docs models; invalid entries omitted after diagnostics.	 
	 */
	fun parseMemberErrorDocs(annotated: KSAnnotated): List<SchemaErrorDocModel> {
		val result = ArrayList<SchemaErrorDocModel>()
		for (ann in annotated.annotations) {
			if (annotationFqcn(ann) == OpenApiPresentationFqns.API_ERROR) {
				parseApiError(ann, annotated)?.let { result += it }
			}
		}
		return result
	}
	
	/**
	 * Parses one `@ApiError` usage into IR, or `null` when the catalog contract fails.
	 *
	 * @param ann `@ApiError` application.
	 * @param node Symbol for diagnostic location.
	 * @return Docs model, or `null` when omitted after an error.	 
	 */
	private fun parseApiError(
		ann: KSAnnotation,
		node: KSNode
	): SchemaErrorDocModel? {
		var code = ""
		var message = ""
		var catalog: KSType? = null
		ann.arguments.forEachIndexed { index, arg ->
			val name = arg.name?.asString()
			when {
				name == OpenApiPresentationFqns.Attr.CODE || (name == null && index == OpenApiPresentationFqns.Attr.CODE_INDEX) -> {
					code = arg.value as? String ?: ""
				}
				name == OpenApiPresentationFqns.Attr.MESSAGE || (name == null && index == OpenApiPresentationFqns.Attr.MESSAGE_INDEX) -> {
					message = arg.value as? String ?: ""
				}
				name == OpenApiPresentationFqns.Attr.CATALOG || (name == null && index == OpenApiPresentationFqns.Attr.CATALOG_INDEX) -> {
					catalog = arg.value as? KSType
				}
			}
		}
		
		val catalogType = catalog ?: run {
			logger.error("@ApiError catalog could not be resolved — pass an enum class that implements ${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}.",node)
			return null
		}
		if (!catalogVerifier.verify(code, catalogType, node, annotationName = "@ApiError")) {
			return null
		}
		val catalogFqcn = catalogType.declaration.qualifiedName?.asString() ?: run {
			logger.error("@ApiError catalog type name could not be resolved.", node)
			return null
		}
		
		val resolvedCode = if (code.isNotBlank()) {
			CatalogErrorDefinitionLookup.codeOf(catalogFqcn, code) ?: code
		} else code
		
		val resolvedMessage = message.ifBlank {
			CatalogErrorDefinitionLookup.messageOf(catalogFqcn, code).orEmpty()
		}
		
		return SchemaErrorDocModel(
			code = resolvedCode,
			message = resolvedMessage,
			catalogFqcn = catalogFqcn)
	}
	
	/**
	 * Resolves the fully qualified annotation type of [ann].
	 *
	 * @param ann Annotation application.
	 * @return FQCN, or `null` when unresolved.	 
	 */
	private fun annotationFqcn(ann: KSAnnotation): String? =
		AnnotationFqcn.of(ann)
}
