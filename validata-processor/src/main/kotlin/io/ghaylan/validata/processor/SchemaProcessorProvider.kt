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
 * KSP SPI entry point that constructs [SchemaProcessor].
 *
 * ## How KSP finds this class
 *
 * Declared in `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
 * as this provider’s FQCN. That registration is part of the public contract of the
 * `validata-processor` artifact — do not rename without updating the service file.
 *
 * ## Consumer wiring
 *
 * Apps attach this jar with **`ksp("…:validata-processor")`**, never `implementation`.
 * The processor must not ship on the application runtime classpath.*
 * 
 * @author Ghaylan Saada
 */
class SchemaProcessorProvider: SymbolProcessorProvider {
	
	/**
	 * Creates a new [SchemaProcessor] for this compilation environment.
	 *
	 * Side effects: none.
	 *
	 * @param environment KSP logger, code generator, and options for this round.
	 * @return Processor that emits object/endpoint schema factories for this round.	 
	 */
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
		SchemaProcessor(environment)
}
