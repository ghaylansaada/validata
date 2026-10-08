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

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Emits `sample.DeferredPeer` on the first round so a prior deferred `@Validatable` can resolve later.
 *
 * Registered **after** the probing schema processor so round 1 still sees a missing peer type.
 * 
 * @author Ghaylan Saada
 */
internal class DeferredPeerStubProcessorProvider: SymbolProcessorProvider {
	
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = object: SymbolProcessor {
		private var written = false
		
		override fun process(resolver: Resolver): List<KSAnnotated> {
			if (written) return emptyList()
			written = true
			environment.codeGenerator.createNewFile(
				Dependencies(aggregating = false),
				"sample",
				"DeferredPeer",
			)
				.bufferedWriter()
				.use { out ->
					out.write(
						"""
						package sample
						class DeferredPeer
						""".trimIndent(),
					)
				}
			return emptyList()
		}
	}
}
