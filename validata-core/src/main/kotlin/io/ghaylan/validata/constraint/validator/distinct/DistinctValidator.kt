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
package io.ghaylan.validata.constraint.validator.distinct

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.DistinctConstraint
import io.ghaylan.validata.constraint.validator.distinct.DistinctContainerSupport.ComboKey
import io.ghaylan.validata.constraint.validator.distinct.DistinctContainerSupport.FieldExtractor
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator.comboProbe
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator.planProbe
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator.resolveElementPlan
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.shape.*

/**
 * Rejects duplicate collection elements (optionally keyed by one or more element field names).
 *
 * [DistinctConstraint.by] alone drives the strategy — there is no separate mode:
 * - **empty** → whole-element uniqueness (value / equals for scalars and objects).
 * - **one or more names** → uniqueness of the tuple of those fields, always via [ComboKey].
 *   A single-field `by` is a length-1 [ComboKey]; one path handles every arity.
 *
 * ### Placement
 *
 * `@Distinct` is **type-use on the collection element** only (`List<@Distinct T>`). The engine
 * visits each element with [ValidationContext.array] set; an [ElementPlan] and duplicate sets are
 * memoized in [ValidationContext] attributes so later siblings do not rebuild extractors or
 * rescan. Failures are stamped on indexed paths (e.g. `users[1]`).
 *
 * ### Hot-path rules
 *
 * - Element checks keep a cached duplicate **set** via [DistinctContainerSupport.allDuplicates].
 * - Element [ElementPlan] lookup uses a ThreadLocal probe key (no per-element string alloc);
 *   probes are [ThreadLocal.remove]d in `finally` so pooled threads do not retain them.
 * - Clean scans return [emptySet] — never an empty [HashSet] instance.
 * - Keyed uniqueness (any arity of `by`) uses [ComboKey]; element membership probes with a
 *   ThreadLocal key so the success path never allocates a new key for `contains`.
 *
 * ### Reporting
 *
 * Messages describe the **item** (not the container) and name declared key fields when
 * applicable. The element index lives in [ConstraintError.path] only — not in the message.
 * Element payloads and key-field values stay out of the message. The failing
 * [DistinctConstraint] is attached.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object DistinctValidator : ConstraintValidator<Any, DistinctConstraint>() {
	
	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: DistinctConstraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.COLLECTION_DUPLICATE)
	
	/**
	 * Reusable attribute-map probe for [ElementPlan] lookup.
	 *
	 * Mutated in [resolveElementPlan] before each [ValidationContext.getOrComputeAttribute] call;
	 * never stored in the bag (the store key is [IntPairKey.copy]). Cleared via [ThreadLocal.remove]
	 * in `finally` after each probe use.
	 */
	private val planProbe = ThreadLocal.withInitial { IntPairKey(0, 0) }
	
	/**
	 * Reusable [ComboKey] for element-context keyed membership checks.
	 *
	 * Filled via [ComboKey.fill] then used only as the argument to `Set.contains`; never inserted
	 * into a HashSet (stored keys are immutable [ComboKey.from] instances). Cleared via
	 * [ThreadLocal.remove] in `finally` after each probe use.
	 */
	private val comboProbe = ThreadLocal.withInitial { ComboKey.probe(capacity = 4) }
	
	/**
	 * Always `true`: element evaluation needs [ValidationContext.array] for siblings.
	 */
	override val requiresArrayContext: Boolean get() = true
	
	/**
	 * Evaluates uniqueness for the current collection element.
	 *
	 * [IterableShape] / [MapShape] at the current node are no-ops (`null`) — Distinct is never
	 * placed on the container. Otherwise uses [ValidationContext.array] and a cached [ElementPlan].
	 *
	 * Null values skip.
	 * Side effects: may memoize [ElementPlan] / duplicate sets in [ValidationContext] attributes;
	 *   mutates ThreadLocal probes.
	 *
	 * @param value Element value under test; `null` skips.
	 * @param constraint Metadata for `by`.
	 * @param context Active cursor (shape, array siblings, attribute bag).
	 * @return Path-free [ConstraintError] with [ConstraintErrorCode.COLLECTION_DUPLICATE]
	 *   carrying [constraint], or `null` when valid.
	 */
	override fun validate(
		value: Any,
		constraint: DistinctConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val shape = context.shape
		if (shape is IterableShape || shape is MapShape) return null
		
		val arrayCtx = context.array ?: return null
		val arrayValue = arrayCtx.value?.takeUnless { it.isEmpty() } ?: return null
		val elementShape = (arrayCtx.shape as? IterableShape)?.element ?: return null
		
		val plan = resolveElementPlan(
			context = context,
			constraint = constraint,
			elementShape = elementShape)
		
		if (plan === ElementPlan.NONE) return null

		return when (plan.kind) {
			ElementPlan.Kind.SCALAR -> validateScalarElement(
				currentItem = value,
				array = arrayValue,
				duplicatesKey = plan.duplicatesKey,
				context = context,
				constraint = constraint)
			
			ElementPlan.Kind.KEYED -> validateElementKeyed(
				currentItem = value,
				array = arrayValue,
				plan = plan,
				context = context,
				constraint = constraint)
		}
	}
	
	/**
	 * Loads or builds the [ElementPlan] for this collection × constraint site.
	 *
	 * Probes with ThreadLocal [planProbe] so success-path element visits do not allocate
	 * cache-key strings; [IntPairKey.copy] allocates the immutable store key only on miss.
	 * Always [ThreadLocal.remove]s the probe afterward so pooled worker threads do not retain it.
	 *
	 * @param context Element cursor (path must still include the index segment).
	 * @param constraint Stable generated metadata for this annotation site.
	 * @param elementShape IR shape of each list element (drives [ElementPlan.Kind]).
	 * @return Cached plan, or [ElementPlan.NONE] when Distinct does not apply to [elementShape].
	 */
	private fun resolveElementPlan(
		context: ValidationContext,
		constraint: DistinctConstraint,
		elementShape: TypeShape,
	): ElementPlan {
		val pathId = System.identityHashCode(context.path.beforeLastIndex())
		val constraintId = System.identityHashCode(constraint)
		val probe = planProbe.get()
			.also { it.set(pathId, constraintId) }
		try {
			return context.getOrComputeAttribute(probe, storeKey = { probe.copy() }) {
				ElementPlan.create(constraint, elementShape, pathId, constraintId)
					?: ElementPlan.NONE
			}
		}
		finally {
			planProbe.remove()
		}
	}
	
	/**
	 * Element-context check for scalar / dynamic / whole-object list elements.
	 *
	 * Side effects: memoizes the sibling duplicate set in the [ValidationContext] attribute bag
	 * on first sibling visit.
	 *
	 * @param currentItem Element under test.
	 * @param array Sibling elements from the array context.
	 * @param duplicatesKey Attribute-bag key for the memoized duplicate set.
	 * @param context Element cursor supplying the attribute bag.
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Violation when [currentItem] is a duplicate, or `null` when unique.
	 */
	private fun validateScalarElement(
		currentItem: Any,
		array: List<Any?>,
		duplicatesKey: Any,
		context: ValidationContext,
		constraint: DistinctConstraint,
	): ConstraintError<*>? {
		val duplicates = context.getOrComputeAttribute(duplicatesKey) {
			DistinctContainerSupport.allDuplicates(array)
		}
		if (currentItem !in duplicates) return null
		return ConstraintError(
			code = ConstraintErrorCode.COLLECTION_DUPLICATE,
			message = DistinctContainerSupport.unkeyedMessage(),
			metadata = constraint,
		)
	}
	
	/**
	 * Element-context check for any non-empty [DistinctConstraint.by] (one field or many).
	 *
	 * Side effects: memoizes the sibling duplicate-key set; mutates [comboProbe] and always
	 * [ThreadLocal.remove]s it afterward.
	 *
	 * @param currentItem Element under test.
	 * @param array Sibling elements from the array context.
	 * @param plan Extractors, field names, and attribute key for this annotation site.
	 * @param context Element cursor supplying the attribute bag.
	 * @param constraint Constraint metadata that failed; attached to the violation.
	 * @return Violation when the key tuple is duplicated, or `null` when unique.
	 */
	private fun validateElementKeyed(
		currentItem: Any,
		array: List<Any?>,
		plan: ElementPlan,
		context: ValidationContext,
		constraint: DistinctConstraint,
	): ConstraintError<*>? {
		val duplicateCombos = context.getOrComputeAttribute(plan.duplicatesKey) {
			DistinctContainerSupport.allDuplicates(array) { item -> ComboKey.from(plan.extractors, item) }
		}
		val probe = comboProbe.get()
		try {
			probe.fill(plan.extractors, currentItem)
			if (probe !in duplicateCombos) return null
			return ConstraintError(
				code = ConstraintErrorCode.COLLECTION_DUPLICATE,
				message = DistinctContainerSupport.keyedMessage(fields = plan.fieldsList),
				metadata = constraint,
			)
		}
		finally {
			comboProbe.remove()
		}
	}
	
	/**
	 * Mutable int-pair key for attribute-bag probing (path identity × constraint identity).
	 *
	 * @property a First component, normally a path identity hash.
	 * @property b Second component, normally a constraint identity hash.
	 */
	private class IntPairKey(
		private var a: Int,
		private var b: Int) {
		
		/**
		 * Overwrites both components for the next probe.
		 *
		 * Mutates this key, invalidating any earlier hash or comparison.
		 *
		 * @param first New value for [a].
		 * @param second New value for [b].
		 */
		fun set(
			first: Int,
			second: Int) {
			a = first
			b = second
		}
		
		/**
		 * Snapshots the current components into a key safe to retain in the attribute bag.
		 *
		 * Side effects: none.
		 *
		 * @return Independent key equal to this one at call time.
		 */
		fun copy(): IntPairKey = IntPairKey(a, b)
		
		/**
		 * Structural equality over both components.
		 *
		 * @param other Candidate; only another [IntPairKey] can match.
		 * @return `true` when both components are equal.
		 */
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is IntPairKey) return false
			return a == other.a && b == other.b
		}
		
		/**
		 * Hash over both components.
		 *
		 * @return Hash consistent with [equals]; changes after [set].
		 */
		override fun hashCode(): Int = 31 * a + b
	}
	
	/**
	 * Precomputed element-context strategy for one collection × constraint annotation site.
	 *
	 * @property kind Whether elements are compared whole or by a key tuple.
	 * @property extractors Key-field extractors for [Kind.KEYED]; empty for [Kind.SCALAR].
	 * @property fieldsList Field names matching [extractors]; empty for [Kind.SCALAR].
	 * @property duplicatesKey Attribute-bag key under which the sibling duplicate set is memoized.
	 */
	private class ElementPlan private constructor(
		val kind: Kind,
		val extractors: Array<FieldExtractor>,
		val fieldsList: List<String>,
		val duplicatesKey: Any) {
		
		/**
		 * Element comparison strategy.
		 */
		enum class Kind {
			
			/**
			 * Compare elements as whole values.
			 */
			SCALAR,
			
			/**
			 * Compare the tuple of `by` field values.
			 */
			KEYED,
		}
		
		/**
		 * Plan factories and the no-op sentinel.
		 */
		companion object {
			
			/**
			 * Sentinel meaning Distinct does not apply to this element shape.
			 */
			val NONE = ElementPlan(Kind.SCALAR, emptyArray(), emptyList(), IntPairKey(0, 0))
			
			/**
			 * Builds the plan for one annotation site.
			 *
			 * Side effects: resolves property readers for keyed object elements.
			 *
			 * @param constraint Metadata for `by`.
			 * @param elementShape IR shape of each list element.
			 * @param pathId Identity hash of the owning collection path.
			 * @param constraintId Identity hash of the annotation site.
			 * @return Plan for [elementShape], or `null` when Distinct does not apply (nested
			 *   iterables, or map elements without `by`).
			 * @throws IllegalStateException when a `by` name has no matching property on an object
			 *   element schema.
			 */
			fun create(
				constraint: DistinctConstraint,
				elementShape: TypeShape,
				pathId: Int,
				constraintId: Int,
			): ElementPlan? {
				val duplicatesKey = IntPairKey(pathId, constraintId xor 1)
				
				return when (elementShape) {
					is ScalarShape, is DynamicShape -> ElementPlan(
						Kind.SCALAR,
						emptyArray(),
						emptyList(),
						duplicatesKey,
					)
					
					is MapShape -> {
						if (constraint.by.isEmpty()) return null
						val extractors = DistinctContainerSupport.mapFieldExtractors(constraint.by)
						keyedPlan(extractors, duplicatesKey)
					}
					
					is ObjectRefShape -> {
						if (constraint.by.isEmpty()) {
							return ElementPlan(Kind.SCALAR, emptyArray(), emptyList(), duplicatesKey)
						}
						val schema = elementShape.ref.value
						val extractors = DistinctContainerSupport.objectFieldExtractors(constraint.by, schema)
						keyedPlan(extractors, duplicatesKey)
					}
					
					is IterableShape -> null
				}
			}
			
			/**
			 * Assembles a [Kind.KEYED] plan and materializes its field names once.
			 *
			 * Side effects: none.
			 *
			 * @param extractors Key-field extractors in declaration order.
			 * @param duplicatesKey Attribute-bag key for the memoized duplicate set.
			 * @return Keyed plan for this site.
			 */
			private fun keyedPlan(
				extractors: Array<FieldExtractor>,
				duplicatesKey: Any,
			): ElementPlan = ElementPlan(
				Kind.KEYED,
				extractors,
				DistinctContainerSupport.fieldNames(extractors),
				duplicatesKey)
		}
	}
}
