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
package io.ghaylan.validata.engine.fastpath

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.engine.fastpath.ArrayContextFastPath.cache
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides whether an iterable property walk must allocate a [ValidationContextValue]
 * array wrapper for sibling-aware validators (Plan V2 Phase 10).
 *
 * Most list/map properties only run presence/size/format checks that never read
 * [ValidationContext.array]. Skipping the wrapper (and its `toArrayContextList` call)
 * removes a per-field allocation on those properties.
 *
 * ## Soundness
 *
 * Returns `true` when any [CompiledConstraint] on the property, on the [IterableShape] itself, or
 * on a reachable element/key/value type-use node is backed by a
 * [ConstraintValidator.requiresArrayContext] validator. Prefer false negatives never — a
 * conservative `true` only costs an allocation; a false `false` would break `@Distinct` element
 * context.
 *
 * ## Caching
 *
 * Results are memoized by identity of the property constraint list + [IterableShape] instance.
 * Lookups use a [ThreadLocal] probe key so cache hits allocate nothing (audit C.2). Generated
 * schemas reuse those instances for the JVM lifetime, so the map stays bounded under the
 * static-schema contract (same as [SchemaGroupFastPath]).
 *
 * Visibility is `internal`: only [ValidatorEngine] and same-module tests
 * call this.*
 * 
 * @author Ghaylan Saada
 */
internal object ArrayContextFastPath {
	
	/**
	 * Identity key: property constraint list × iterable shape (both process-lifetime singletons
	 * for generated schemas). Mutable for ThreadLocal probes; immutable copies are stored in
	 * [cache].	 
	 */
	private class NeedsArrayKey(
		private var propertyConstraints: List<CompiledConstraint>,
		private var shape: IterableShape,
	) {
		
		fun set(
			propertyConstraints: List<CompiledConstraint>,
			shape: IterableShape
		) {
			this.propertyConstraints = propertyConstraints
			this.shape = shape
		}
		
		fun copy(): NeedsArrayKey = NeedsArrayKey(propertyConstraints, shape)
		
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is NeedsArrayKey) return false
			return propertyConstraints === other.propertyConstraints && shape === other.shape
		}
		
		override fun hashCode(): Int = 31 * System.identityHashCode(propertyConstraints) + System.identityHashCode(shape)
	}
	
	/**
	 * Placeholder shape for ThreadLocal initial value (never used as a real cache key).
	 */
	private val probePlaceholderShape = IterableShape(element = ScalarShape(ScalarKind.STRING))
	
	/**
	 * Thread-local probe reused for [cache] lookups; cleared after each [needsArrayContext] call.
	 */
	private val probe = ThreadLocal.withInitial {
		NeedsArrayKey(emptyList(), probePlaceholderShape)
	}
	
	/**
	 * Cache: (propertyConstraints identity, shape identity) → needs array context.
	 *
	 * Unbounded by design for static schemas; dynamic hosts that mint unique lists/shapes per
	 * request should not share this cache without bounds.	 
	 */
	private val cache = ConcurrentHashMap<NeedsArrayKey, Boolean>()
	
	/**
	 * Whether [shape] under [propertyConstraints] needs `context.array` populated for this property.
	 *
	 * May write [cache] on first scan. No I/O. Success-path hits allocate no key objects.
	 *
	 * @param propertyConstraints Compiled constraints declared on the property itself.
	 * @param shape Iterable IR shape of the property.
	 * @return `true` when any reachable validator sets [ConstraintValidator.requiresArrayContext].	 
	 */
	fun needsArrayContext(
		propertyConstraints: List<CompiledConstraint>,
		shape: IterableShape,
	): Boolean {
		val lookup = probe.get()
		lookup.set(propertyConstraints, shape)
		try {
			cache[lookup]?.let { return it }
			val result = anyRequiresArray(propertyConstraints) || shapeRequiresArray(shape)
			cache.putIfAbsent(lookup.copy(), result)
			return result
		}
		finally {
			probe.remove()
		}
	}
	
	/**
	 * Walks [shape] and nested element/key/value nodes for any array-context-requiring constraint.
	 *
	 * No I/O or mutation.
	 *
	 * @param shape IR node to inspect (iterable, map, object-ref, or scalar/dynamic leaf).
	 * @return `true` when this subtree needs [ValidationContext.array].	 
	 */
	private fun shapeRequiresArray(shape: TypeShape): Boolean = when (shape) {
		is IterableShape -> anyRequiresArray(shape.constraints) || shapeRequiresArray(shape.element)
		is MapShape -> anyRequiresArray(shape.constraints) || shapeRequiresArray(shape.key) || shapeRequiresArray(shape.value)
		is ObjectRefShape -> anyRequiresArray(shape.constraints)
		else -> anyRequiresArray(shape.constraints)
	}
	
	/**
	 * Returns `true` when any context-aware runner in [constraints] needs array context —
	 * [ValidatorBackedRunner] with [ConstraintValidator.requiresArrayContext], or a
	 * [CompositionConstraint] whose nested children need it.
	 *
	 * Unknown runner types are ignored. No I/O or mutation.
	 *
	 * @param constraints Compiled constraints on a property or type-use node.
	 * @return `true` when at least one validator needs sibling array context.	 
	 */
	private fun anyRequiresArray(constraints: List<CompiledConstraint>): Boolean {
		for (constraint in constraints) {
			when (val metadata = constraint.metadata) {
				is CompositionConstraint -> {
					if (anyRequiresArray(metadata.children)) return true
				}
				else -> {
					val runner = constraint.runner as? ValidatorBackedRunner ?: continue
					if (runner.validator.requiresArrayContext) return true
				}
			}
		}
		return false
	}
}
