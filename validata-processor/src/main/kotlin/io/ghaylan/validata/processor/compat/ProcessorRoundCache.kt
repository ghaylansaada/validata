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
package io.ghaylan.validata.processor.compat

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import java.util.*

/**
 * Per-KSP-round caches shared across analyze / verify / classify paths.
 *
 * **Thread-confined:** mutable depth and identity maps live in a [ThreadLocal] [State].
 * The `object` facade keeps call sites stable; do **not** share one [State] across threads.
 *
 * Processors should call [forceClear] then [begin] at each [com.google.devtools.ksp.processing.SymbolProcessor.process]
 * entry so reused KSP worker threads never inherit a prior round's maps. Call [end] in a matching
 * `finally`. Nesting (refcount > 1) is uncommon and usually only appears in tests that call
 * processors manually. Maps clear when the outermost [end] runs, or immediately via [forceClear]
 * from [com.google.devtools.ksp.processing.SymbolProcessor.finish] / `onError`.
 *
 * **Why an `object` (not a per-processor instance):** annotation / type helpers
 * ([AnnotationFqcn], classification paths) call into this cache without a processor DI graph.
 * ThreadLocal [State] gives round isolation on parallel test workers; a true instance would require
 * threading a cache through every static classify path for little extra safety.*
 * 
 * @author Ghaylan Saada
 */
internal object ProcessorRoundCache {
	
	/**
	 * Per-thread round cache state (depth + identity maps).
	 */
	private class State {
		
		/**
		 * Nesting depth of active [begin] calls on this thread.
		 */
		var depth: Int = 0
		
		/**
		 * Annotation instance → resolved type FQCN, or `null` when inactive.
		 */
		var annotationFqcn: IdentityHashMap<KSAnnotation, String?>? = null
		
		/**
		 * Type instance → assignable supertype FQCN set, or `null` when inactive.
		 */
		var typeSupers: IdentityHashMap<KSType, Set<String>>? = null
	}
	
	/**
	 * Thread-local [State] for round isolation on parallel KSP workers.
	 */
	private val local = ThreadLocal.withInitial { State() }
	
	/**
	 * Returns this thread's active [State].
	 *
	 * Side effects: none.
	 *
	 * @return Current thread-local cache state.	 
	 */
	private fun state(): State =
		local.get()
	
	/**
	 * Arms caches for a processor `process` call. Nested [begin] calls reuse the same maps.
	 *
	 * Side effects: may allocate identity maps; increments [State.depth].	 
	 */
	fun begin() {
		val s = state()
		if (s.depth == 0) {
			s.annotationFqcn = IdentityHashMap()
			s.typeSupers = IdentityHashMap()
		}
		s.depth++
	}
	
	/**
	 * Releases one [begin]. Clears maps when the outermost nesting level ends.
	 *
	 * Side effects: decrements [State.depth]; clears maps when depth reaches zero.
	 * Idempotent when already inactive.	 
	 */
	fun end() {
		val s = state()
		if (s.depth <= 0) {
			s.depth = 0
			s.annotationFqcn = null
			s.typeSupers = null
			return
		}
		s.depth--
		if (s.depth == 0) {
			s.annotationFqcn = null
			s.typeSupers = null
		}
	}
	
	/**
	 * Unconditionally clears depth and maps on this thread.
	 *
	 * Side effects: resets [State.depth] and drops both identity maps.
	 *
	 * Use from processor [com.google.devtools.ksp.processing.SymbolProcessor.finish] /
	 * `onError`, and immediately before [begin] at each `process` entry for worker reuse safety.	 
	 */
	fun forceClear() {
		val s = state()
		s.depth = 0
		s.annotationFqcn = null
		s.typeSupers = null
	}
	
	/**
	 * Resolved annotation type FQCN for [ann], cached by annotation identity within the round.
	 *
	 * Side effects: may insert into the round annotation map when [begin] was called.
	 *
	 * @param ann Annotation whose type FQCN to resolve.
	 * @return Resolved annotation type FQCN, or `null` when unresolved.	 
	 */
	fun annotationFqcn(ann: KSAnnotation): String? {
		val map = state().annotationFqcn
		if (map != null) {
			if (map.containsKey(ann)) return map[ann]
			val fqcn = resolveAnnotationFqcn(ann)
			map[ann] = fqcn
			return fqcn
		}
		return resolveAnnotationFqcn(ann)
	}
	
	/**
	 * Whether [ann]'s type FQCN equals [expected].
	 *
	 * Side effects: may populate the annotation cache.
	 *
	 * @param ann Annotation to test.
	 * @param expected Expected type FQCN.
	 * @return `true` when [ann]'s type FQCN equals [expected].	 
	 */
	fun annotationIs(
		ann: KSAnnotation,
		expected: String
	): Boolean =
		annotationFqcn(ann) == expected
	
	/**
	 * Cached supertype FQCN set for [type] (excludes self; depth-capped), or `null` map miss path
	 * when the round cache is inactive.
	 *
	 * Callers that compute supers themselves should [putSupertypes] after walking.
	 *
	 * Side effects: none (read-only cache lookup).
	 *
	 * @param type Type whose cached supertype set to read.
	 * @return Cached supertype FQCNs, or `null` when the cache is inactive or misses.	 
	 */
	fun getSupertypes(type: KSType): Set<String>? =
		state().typeSupers?.get(type)
	
	/**
	 * Stores [supers] for [type] when the round cache is active.
	 *
	 * Side effects: may insert into the round type-hierarchy map.
	 *
	 * @param type Type key (typically [KSType.makeNotNullable]).
	 * @param supers Assignable supertype FQCN set to cache (excludes self).	 
	 */
	fun putSupertypes(
		type: KSType,
		supers: Set<String>
	) {
		state().typeSupers?.put(type, supers)
	}
	
	/**
	 * Resolves [ann]'s annotation type FQCN without caching.
	 *
	 * Side effects: none.
	 *
	 * @param ann Annotation to resolve.
	 * @return Type FQCN, or `null` when KSP cannot resolve the declaration.	 
	 */
	private fun resolveAnnotationFqcn(ann: KSAnnotation): String? =
		ann.annotationType.resolve().declaration.qualifiedName?.asString()
}
