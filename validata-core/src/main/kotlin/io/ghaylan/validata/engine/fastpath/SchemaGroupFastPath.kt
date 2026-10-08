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
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.engine.fastpath.SchemaGroupFastPath.defaultCompatible
import io.ghaylan.validata.engine.fastpath.SchemaGroupFastPath.fullyActiveUnder
import io.ghaylan.validata.engine.fastpath.SchemaGroupFastPath.fullyUngrouped
import io.ghaylan.validata.engine.fastpath.SchemaGroupFastPath.scan
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Schema-time analysis that decides whether a validation run can skip per-constraint group
 * intersection checks.
 *
 * ## Why this exists
 *
 * Every constraint currently pays [ConstraintValidator] group matching even when the active groups
 * are the default singleton `{[OnDefault]}` and every compiled constraint either has empty groups
 * or includes [OnDefault]. That intersection is cheap per call but still runs on every constraint
 * of every field. When the schema graph proves every constraint is already active under the run's
 * groups, the engine sets [ValidationContext.skipGroupChecks] and validators return early.
 *
 * ## Caching
 *
 * Results are cached by [ObjectSchema] identity. Generated schemas are process-wide singletons,
 * so the map stays bounded under the **static-schema / JVM-lifetime** contract. Hand-built
 * throwaway schemas in tests are fine — entries are small. The maps are intentionally unbounded:
 * eviction is unnecessary when keys are process-wide schema identities. Dynamic hosts that mint
 * unique [ObjectSchema] instances per request should not share this cache without bounds.
 *
 * ## Cycle safety
 *
 * KSP emits self-references as `ObjectRefShape(lazy { XxxSchema.build() })`. Each forced
 * resolve may produce a **new** [ObjectSchema] instance, so visitation keys by
 * [ObjectSchema.type] rather than object identity.
 *
 * ## What this is not
 *
 * Not a substitute for correctness when custom groups are in play. Custom active groups still
 * walk the graph (or use a dedicated cache entry) to prove every constraint intersects.
 *
 * Visibility is `internal`: only [ValidatorEngine] and same-module tests
 * call this (audit Task 5.4).*
 * 
 * @author Ghaylan Saada
 */
internal object SchemaGroupFastPath {

	/**
	 * Cache: schema → every constraint is empty-groups or includes [OnDefault].
	 */
	private val defaultCompatible = ConcurrentHashMap<ObjectSchema, Boolean>()

	/**
	 * Cache: schema → every constraint has an empty groups set.
	 */
	private val fullyUngrouped = ConcurrentHashMap<ObjectSchema, Boolean>()

	/**
	 * Cache: (schema × active groups) → every constraint intersects `activeGroups` or is ungrouped.
	 */
	private val fullyActiveUnder = ConcurrentHashMap<FullyActiveKey, Boolean>()

	/**
	 * Returns `true` when every constraint under [schemas] would pass group filtering for
	 * [activeGroups], so the engine may set [ValidationContext.skipGroupChecks].
	 *
	 * May write [defaultCompatible] / [fullyUngrouped] on first scan of a schema. No I/O.
	 *
	 * @param activeGroups Groups active for this validation run (already a [Set], never rebuilt
	 *   here).
	 * @param schemas Object schemas that may be walked during the run (body + flat sections).
	 * @return `true` when group intersection can be skipped for the whole run.
	 */
	fun canSkipGroupChecks(
		activeGroups: Set<KClass<*>>,
		schemas: Sequence<ObjectSchema>,
	): Boolean {
		// Stream once — no toList(); empty sequence is vacuously skip-compatible.
		val predicate: (ObjectSchema) -> Boolean = when {
			activeGroups.isEmpty() -> ::isFullyUngrouped
			isDefaultOnly(activeGroups) -> ::isDefaultCompatible
			else -> { schema -> isFullyActiveUnder(schema, activeGroups) }
		}
		for (schema in schemas) {
			if (!predicate(schema)) return false
		}
		return true
	}

	/**
	 * Convenience overload for a small fixed list of optional section schemas.
	 *
	 * Same cache side effects as the [Sequence] overload.
	 *
	 * @param activeGroups Groups active for this validation run.
	 * @param schemas Nullable body / query / header / path schemas (nulls ignored).
	 * @return Same result as the [Sequence] overload over non-null [schemas].
	 */
	fun canSkipGroupChecks(
		activeGroups: Set<KClass<*>>,
		vararg schemas: ObjectSchema?,
	): Boolean = canSkipGroupChecks(activeGroups, schemas.asSequence().filterNotNull())

	/**
	 * Whether [groups] is exactly the singleton `{[OnDefault]}`.
	 *
	 * No I/O or mutation.
	 *
	 * @param groups Active groups for the run.
	 * @return `true` when only [OnDefault] is active.
	 */
	private fun isDefaultOnly(groups: Set<KClass<*>>): Boolean =
		groups.size == 1 && OnDefault::class in groups

	/**
	 * Whether every constraint under [schema] is active when the run uses only [OnDefault].
	 *
	 * Writes [defaultCompatible] on first scan of [schema].
	 *
	 * @param schema Root object schema to scan.
	 * @return `true` when every constraint has empty groups or includes [OnDefault].
	 */
	private fun isDefaultCompatible(schema: ObjectSchema): Boolean =
		defaultCompatible.getOrPut(schema) {
			scan(schema, SchemaTypeVisited()) { groups ->
				groups.isEmpty() || OnDefault::class in groups
			}
		}

	/**
	 * Whether every constraint under [schema] declares no groups at all.
	 *
	 * Required when the run's active group set is empty: only ungrouped constraints would run.
	 * Writes [fullyUngrouped] on first scan of [schema].
	 *
	 * @param schema Root object schema to scan.
	 * @return `true` when every constraint's groups set is empty.
	 */
	private fun isFullyUngrouped(schema: ObjectSchema): Boolean =
		fullyUngrouped.getOrPut(schema) {
			scan(schema, SchemaTypeVisited()) { groups -> groups.isEmpty() }
		}

	/**
	 * Whether every constraint under [schema] intersects [activeGroups] (or is ungrouped).
	 *
	 * Cached by schema identity + frozen active-group set (KClass identity set). Generated schemas
	 * and typical endpoint group sets are process-lifetime / small, so the map stays bounded under
	 * the static-schema contract. Writes [fullyActiveUnder] on miss.
	 *
	 * @param schema Root object schema to scan.
	 * @param activeGroups Groups active for this validation run.
	 * @return `true` when every constraint is already active under [activeGroups].
	 */
	private fun isFullyActiveUnder(
		schema: ObjectSchema,
		activeGroups: Set<KClass<*>>,
	): Boolean {
		val key = FullyActiveKey(schema, activeGroups)
		fullyActiveUnder[key]?.let { return it }
		val result = scan(schema, SchemaTypeVisited()) { groups ->
			groups.isEmpty() || groups.any { it in activeGroups }
		}
		fullyActiveUnder.putIfAbsent(key, result)
		return result
	}

	/**
	 * Identity key for [fullyActiveUnder]: schema singleton × active group set.
	 */
	private class FullyActiveKey(
		private val schema: ObjectSchema,
		private val activeGroups: Set<KClass<*>>,
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is FullyActiveKey) return false
			return schema === other.schema && activeGroups == other.activeGroups
		}

		override fun hashCode(): Int = 31 * System.identityHashCode(schema) + activeGroups.hashCode()
	}

	/**
	 * Depth-first walk of [schema] properties, nested shapes, and subtypes.
	 *
	 * Mutates [visited]; no shared cache writes.
	 *
	 * @param schema Object schema node to inspect.
	 * @param visited Cycle guard keyed by [ObjectSchema.type].
	 * @param predicate Returns `true` when a constraint's groups are acceptable for the run.
	 * @return `true` when every reachable constraint passes [predicate].
	 */
	private fun scan(
		schema: ObjectSchema,
		visited: SchemaTypeVisited,
		predicate: (Set<KClass<*>>) -> Boolean,
	): Boolean {
		if (!visited.add(schema.type)) return true
		for (property in schema.properties) {
			if (!constraintsOk(property.constraints, predicate)) return false
			if (!shapeOk(property.shape, visited, predicate)) return false
		}
		for (subtype in schema.subtypes.values) {
			if (!scan(subtype, visited, predicate)) return false
		}
		return true
	}

	/**
	 * Checks type-use constraints on [shape] and descends into nested IR nodes.
	 *
	 * Mutates [visited] when descending into object refs; no shared cache writes.
	 *
	 * @param shape Property or nested type shape.
	 * @param visited Cycle guard shared with [scan].
	 * @param predicate Group acceptability predicate from the caller.
	 * @return `true` when this shape subtree is skip-compatible.
	 */
	private fun shapeOk(
		shape: TypeShape,
		visited: SchemaTypeVisited,
		predicate: (Set<KClass<*>>) -> Boolean,
	): Boolean {
		return constraintsOk(shape.constraints, predicate) && when (shape) {
			is ScalarShape, is DynamicShape -> true
			is IterableShape -> shapeOk(shape.element, visited, predicate)
			is MapShape -> shapeOk(shape.key, visited, predicate) && shapeOk(shape.value, visited, predicate)
			is ObjectRefShape -> scan(shape.ref.value, visited, predicate)
		}
	}

	/**
	 * Applies [predicate] to every constraint's declared groups, including nested
	 * [CompositionConstraint] children.
	 *
	 * No I/O or mutation.
	 *
	 * @param constraints Compiled constraints on a property or type-use.
	 * @param predicate Group acceptability predicate from the caller.
	 * @return `true` when every constraint passes [predicate].
	 */
	private fun constraintsOk(
		constraints: List<CompiledConstraint>,
		predicate: (Set<KClass<*>>) -> Boolean,
	): Boolean {
		for (constraint in constraints) {
			if (!predicate(constraint.metadata.groups)) return false
			if (constraint.metadata is CompositionConstraint && !constraintsOk((constraint.metadata as CompositionConstraint).children, predicate)) {
				return false
			}
		}
		return true
	}
}
