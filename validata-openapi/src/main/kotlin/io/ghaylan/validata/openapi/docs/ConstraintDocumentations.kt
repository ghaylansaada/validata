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
package io.ghaylan.validata.openapi.docs

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.openapi.docs.ConstraintDocumentations.NONE
import io.ghaylan.validata.schema.shape.TypeShape
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Discovers [ConstraintDocumentation] implementations via [ServiceLoader] and resolves hints
 * for a metadata occurrence.
 *
 * First SPI documenter with [ConstraintDocumentation.supports] == true wins (ServiceLoader order).
 * A supporting documenter that returns [ConstraintDocHints.EMPTY] means “no native facet”, not
 * “unmapped” — the applicator must not set `x-validata-unmapped` in that case.
 *
 * Resolved owners are cached by metadata class after the first lookup.*
 * 
 * @author Ghaylan Saada
 */
object ConstraintDocumentations {
	
	/**
	 * Cached ServiceLoader snapshot; `null` means not loaded yet.
	 */
	@Volatile
	private var cached: List<ConstraintDocumentation>? = null
	
	/**
	 * Sentinel for “no documenter supports this metadata class”.
	 */
	private val NONE: ConstraintDocumentation = object: ConstraintDocumentation {
		override fun supports(metadata: ConstraintMetadata): Boolean = false
		override fun hints(
			metadata: ConstraintMetadata,
			shape: TypeShape?
		): ConstraintDocHints = ConstraintDocHints.EMPTY
	}
	
	/**
	 * Metadata class → owning documenter, or [NONE] when unsupported.
	 */
	private val byMetadataClass = ConcurrentHashMap<Class<*>, ConstraintDocumentation>()
	
	/**
	 * Returns every documenter on the classpath, loading and caching on first call.
	 *
	 * @return immutable snapshot of discovered documenters	 
	 */
	fun all(): List<ConstraintDocumentation> {
		cached?.let { return it }
		return synchronized(this) {
			cached ?: load().also { cached = it }
		}
	}
	
	/**
	 * Clears the SPI and per-class caches so tests can reload after classpath changes.
	 */
	fun resetForTests() {
		synchronized(this) {
			cached = null
			byMetadataClass.clear()
		}
	}
	
	/**
	 * First [ConstraintDocumentation] that [ConstraintDocumentation.supports] [metadata], or `null`.
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @return owning documenter, or `null` when none claim [metadata]	 
	 */
	fun supporting(metadata: ConstraintMetadata): ConstraintDocumentation? {
		val type = metadata.javaClass
		val existing = byMetadataClass[type]
		if (existing != null) {
			return existing.takeUnless { it === NONE }
		}
		val found = all().firstOrNull { it.supports(metadata) }
		byMetadataClass[type] = found ?: NONE
		return found
	}
	
	/**
	 * Resolves hints for [metadata] using the first supporting documenter.
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @param shape structural hint; may be `null`
	 * @return documentation hints, or [ConstraintDocHints.EMPTY] when no documenter owns [metadata]
	 *   or the owner declines native facets	 
	 */
	fun resolve(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints = supporting(metadata)?.hints(metadata, shape) ?: ConstraintDocHints.EMPTY
	
	/**
	 * Loads documenters from ServiceLoader in provider-file order.
	 *
	 * @return newly loaded snapshot (not yet cached)	 
	 */
	private fun load(): List<ConstraintDocumentation> {
		val loader = ConstraintDocumentation::class.java.classLoader
		return ServiceLoader.load(ConstraintDocumentation::class.java, loader)
			.toList()
	}
}
