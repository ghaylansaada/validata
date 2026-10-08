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
 * KSP SPI entry for [ConstraintMetadataProcessor] (Option 2 generated metadata).
 *
 * Runs when a module opts into generated `*Constraint` metadata classes. Registered in the
 * same `SymbolProcessorProvider` service file as [SchemaProcessorProvider].
 *
 * ```kotlin
 * dependencies {
 *     ksp("io.github.ghaylansaada:validata-processor:<version>")
 * }
 * ```*
 * 
 * @author Ghaylan Saada
 */
class ConstraintMetadataProcessorProvider: SymbolProcessorProvider {
	
	/**
	 * Creates a new [ConstraintMetadataProcessor] for this compilation environment.
	 *
	 * Side effects: none.
	 *
	 * @param environment KSP logger, code generator, and options for this round.
	 * @return Metadata processor for this compilation.	 
	 */
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
		ConstraintMetadataProcessor(environment)
}
