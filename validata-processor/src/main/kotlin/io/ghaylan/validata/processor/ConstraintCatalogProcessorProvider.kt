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

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP SPI entry point that constructs [ConstraintCatalogProcessor].
 *
 * Declared alongside [SchemaProcessorProvider] in
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider` so both
 * processors run independently in the same compilation.
 *
 * ```kotlin
 * dependencies {
 *     ksp("io.github.ghaylansaada:validata-processor:<version>")
 * }
 * ```
 * Custom `@Constraint` annotations in this module are then emitted into the catalog SPI.*
 * 
 * @author Ghaylan Saada
 */
class ConstraintCatalogProcessorProvider: SymbolProcessorProvider {
	
	/**
	 * Creates a new [ConstraintCatalogProcessor] for this compilation environment.
	 *
	 * Side effects: none.
	 *
	 * @param environment KSP logger, code generator, and options for this round.
	 * @return Catalog processor for this compilation.	 
	 */
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
		ConstraintCatalogProcessor(environment)
}
