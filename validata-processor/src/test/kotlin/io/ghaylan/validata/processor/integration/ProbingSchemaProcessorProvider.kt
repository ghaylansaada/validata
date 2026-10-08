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
package io.ghaylan.validata.processor.integration

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import io.ghaylan.validata.processor.SchemaProcessor

/**
 * Wraps [SchemaProcessor] so tests can observe multi-round deferral without duplicating emit logic.
 * 
 * @author Ghaylan Saada
 */
internal class ProbingSchemaProcessorProvider: SymbolProcessorProvider {
	
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = object: SymbolProcessor {
		private val delegate = SchemaProcessor(environment)
		
		override fun process(resolver: Resolver): List<KSAnnotated> {
			val deferred = delegate.process(resolver)
			DeferredRoundProbe.deferredSizes += deferred.size
			return deferred
		}
	}
}
