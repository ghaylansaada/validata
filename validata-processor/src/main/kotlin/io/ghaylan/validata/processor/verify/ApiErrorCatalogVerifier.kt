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
package io.ghaylan.validata.processor.verify

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.fqns.OpenApiPresentationFqns

/**
 * Validates `@ApiError` catalog contracts at compile time.
 *
 * - Blank [code] → error
 * - `catalog` must be an `enum class` that implements [OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION]
 * - `code` must equal an enum constant name
 *
 * @property logger KSP diagnostics sink*
 * 
 * @author Ghaylan Saada
 */
internal class ApiErrorCatalogVerifier(
	private val logger: KSPLogger,
) {
	
	/**
	 * Verifies [code] against [catalog] and reports diagnostics on [node] when invalid.
	 *
	 * Side effects: may emit [KSPLogger.error] when validation fails.
	 *
	 * @param code Machine code from the annotation.
	 * @param catalog Resolved catalog type.
	 * @param node Symbol to attach diagnostics to.
	 * @param annotationName Label used in error messages (`@ApiError`).
	 * @return `true` when the pair is valid for emission.	 
	 */
	fun verify(
		code: String,
		catalog: KSType,
		node: KSNode,
		annotationName: String = "@ApiError",
	): Boolean {
		if (code.isBlank()) {
			logger.error(
				"$annotationName code must not be blank — set a non-empty machine code string.",
				node,
			)
			return false
		}
		val catalogFqcn = catalog.declaration.qualifiedName?.asString()
		if (catalogFqcn == null) {
			logger.error(
				"$annotationName catalog type could not be resolved — use an enum class that " + "implements ${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}.",
				node,
			)
			return false
		}
		val decl = catalog.declaration as? KSClassDeclaration
		if (decl == null || decl.classKind != ClassKind.ENUM_CLASS) {
			logger.error(
				"$annotationName catalog must be an enum class implementing " + "${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}, but was '$catalogFqcn'.",
				node,
			)
			return false
		}
		if (!implementsConstraintErrorDefinition(decl)) {
			logger.error(
				"$annotationName catalog '$catalogFqcn' must implement " + "${OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION}.",
				node,
			)
			return false
		}
		val names = decl.declarations.filterIsInstance<KSClassDeclaration>().filter { it.classKind == ClassKind.ENUM_ENTRY }.map { it.simpleName.asString() }.toList()
		if (code !in names) {
			val preview = names.take(MAX_NAMES_IN_MESSAGE).joinToString(", ")
			val suffix = if (names.size > MAX_NAMES_IN_MESSAGE) ", …" else ""
			logger.error("$annotationName code '$code' is not a constant of enum '$catalogFqcn'. Allowed: [$preview$suffix].", node)
			return false
		}
		return true
	}
	
	/**
	 * Whether [decl] (or any of its supertypes) implements the constraint error catalog interface.
	 *
	 * Side effects: none.
	 *
	 * @param decl Catalog enum declaration.
	 * @return `true` when the interface is in the supertype graph.	 
	 */
	private fun implementsConstraintErrorDefinition(decl: KSClassDeclaration): Boolean {
		val target = OpenApiPresentationFqns.CONSTRAINT_ERROR_DEFINITION
		val queue = ArrayDeque<KSType>()
		decl.superTypes.map { it.resolve() }.forEach { queue.add(it) }
		val seen = HashSet<String>()
		while (queue.isNotEmpty()) {
			val type = queue.removeFirst()
			val fq = type.declaration.qualifiedName?.asString()
				?: continue
			if (!seen.add(fq)) continue
			if (fq == target) return true
			val next = type.declaration as? KSClassDeclaration
				?: continue
			next.superTypes.map { it.resolve() }.forEach { queue.add(it) }
		}
		return false
	}
	
	private companion object {
		
		/**
		 * Maximum enum constant names listed in a single invalid-code diagnostic.
		 */
		const val MAX_NAMES_IN_MESSAGE = 12
	}
}
