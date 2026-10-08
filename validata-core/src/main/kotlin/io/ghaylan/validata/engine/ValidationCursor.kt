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
package io.ghaylan.validata.engine

import io.ghaylan.validata.engine.support.ValidationCursorFrame
import io.ghaylan.validata.runtime.AttributeBag
import io.ghaylan.validata.runtime.PathSegment
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.shape.TypeShape
import java.time.Clock
import kotlin.reflect.KClass

/**
 * Mutable, single-threaded walk state for one [ValidatorEngine] validation run.
 *
 * Replaces per-node [ValidationContext] copies with one object and an explicit push/pop frame stack.
 * Create with [root] once per `validate` / `validateRequest`; call a `push*` before entering a child
 * node, then [pop] in `finally`. Validators receive this as [ValidationContext] and must not call
 * `push*` / [pop]. Not thread-safe; do not pool across requests.
 *
 * ## Path allocation
 *
 * Push/pop store path components in parallel arrays (no [PathSegment] on the success path).
 * [path] / [fieldPath] materialize only when read (error emission, `@Distinct` element plans).
 * Materialized [PathSegment] nodes are cached per depth so sibling indices share the same parent
 * identity ([PathSegment.beforeLastIndex] for Distinct).
 *
 * @property oneErrorPerParam Copied onto every frame; fixed for the run.
 * @property failFast Whole-request abort after first error; fixed for the run.
 * @property groups Active groups; fixed for the run.
 * @property skipGroupChecks When `true`, validators skip group intersection.
 * @property attributeBag Shared bag for `@Distinct` and similar; never replaced on push/pop.
 * @property clock Clock for `@Past` / `@Future`; fixed for the run.*
 * 
 * @author Ghaylan Saada
 */
class ValidationCursor private constructor(
	override val oneErrorPerParam: Boolean,
	override val failFast: Boolean,
	override val groups: Set<KClass<*>>,
	override val skipGroupChecks: Boolean,
	override val attributeBag: AttributeBag,
	override val clock: Clock,
) : ValidationContext {

	/**
	 * Current wire / parameter field name; updated by `push*` / [pop].
	 */
	override var fieldName: String = ""
		private set

	/**
	 * Current IR shape; updated by `push*` / [pop] / [bindRoot].
	 */
	override var shape: TypeShape? = null
		private set

	/**
	 * Current iterable element index, or [ValidationContext.NO_ELEMENT_INDEX].
	 */
	override var elementIndex: Int = ValidationContext.NO_ELEMENT_INDEX
		private set

	/**
	 * Array wrapper for cross-element constraints; updated by `push*` / [pop].
	 */
	override var array: ValidationContextValue<List<Any>>? = null
		private set

	/**
	 * Parent object / section container; updated by `push*` / [pop] / [bindRoot].
	 */
	override var containerObject: ValidationContextValue<Any>? = null
		private set

	/**
	 * Nesting depth from the validation root; updated by `push*` / [pop].
	 */
	override var depth: Int = 0
		private set

	/**
	 * Paths that already produced a violation when [oneErrorPerParam] is true.
	 *
	 * Lazily allocated — success path never touches this.
	 */
	private var failedPaths: HashSet<String>? = null

	/**
	 * Cached [fieldPath] for the current depth; cleared on every path-changing push/pop.
	 */
	private var cachedFieldPath: String? = null

	/**
	 * Growable stack of reusable [ValidationCursorFrame] slots for this run only.
	 */
	private var frameStack: Array<ValidationCursorFrame?> = arrayOfNulls(INITIAL_FRAME_CAPACITY)

	/**
	 * Number of saved frames currently on [frameStack].
	 */
	private var frameDepth: Int = 0

	/**
	 * Path-component kind at each depth (`0` = root). Parallel to [pathNames] / [pathIndexes].
	 */
	private var pathOps: ByteArray = ByteArray(INITIAL_FRAME_CAPACITY)

	/**
	 * Property / map-key labels at each depth.
	 */
	private var pathNames: Array<String?> = arrayOfNulls(INITIAL_FRAME_CAPACITY)

	/**
	 * Iterable indexes at each depth (`-1` when unused).
	 */
	private var pathIndexes: IntArray = IntArray(INITIAL_FRAME_CAPACITY) { -1 }

	/**
	 * Cached [PathSegment] per depth for Distinct parent-identity stability; cleared on invalidate.
	 */
	private var pathSegCache: Array<PathSegment?> = arrayOfNulls(INITIAL_FRAME_CAPACITY)

	/**
	 * Whether [pathSegCache] at that depth is still valid for the current component arrays.
	 */
	private var pathSegValid: BooleanArray = BooleanArray(INITIAL_FRAME_CAPACITY)

	/**
	 * Lazy path rope; materializes (and caches) [PathSegment] nodes only on first read.
	 */
	override val path: PathSegment get() = ensurePathMaterialized()

	/**
	 * Materialized path string for error payloads — builds from component arrays without requiring
	 * [PathSegment] when only the string is needed.
	 */
	override val fieldPath: String get() {
		cachedFieldPath?.let { return it }
		return buildFieldPathString().also { cachedFieldPath = it }
	}

	/**
	 * Pushes a named object/map property frame (path `parent.name`, depth + 1).
	 */
	fun pushProperty(
		name: String,
		propertyShape: TypeShape?,
		container: ValidationContextValue<Any>?,
		arrayContext: ValidationContextValue<List<Any>>?,
	) {
		saveFrame()
		fieldName = name
		shape = propertyShape
		containerObject = container
		array = arrayContext
		elementIndex = ValidationContext.NO_ELEMENT_INDEX
		depth += 1
		ensurePathCapacity(depth)
		pathOps[depth] = OP_NAME
		pathNames[depth] = name
		pathIndexes[depth] = -1
		invalidatePathCacheFrom(depth)
	}

	/**
	 * Pushes an iterable element frame (path `parent[index]`, depth + 1).
	 */
	fun pushIndex(
		index: Int,
		elementShape: TypeShape?,
		arrayContext: ValidationContextValue<List<Any>>?,
	) {
		saveFrame()
		shape = elementShape
		array = arrayContext
		elementIndex = index
		containerObject = null
		depth += 1
		ensurePathCapacity(depth)
		pathOps[depth] = OP_INDEX
		pathNames[depth] = null
		pathIndexes[depth] = index
		invalidatePathCacheFrom(depth)
	}

	/**
	 * Pushes a map-key validation frame (path `parent.keys[label]`, depth + 1).
	 */
	fun pushMapKeys(keyLabel: String, keyShape: TypeShape?) {
		saveFrame()
		fieldName = keyLabel
		shape = keyShape
		containerObject = null
		depth += 1
		ensurePathCapacity(depth)
		pathOps[depth] = OP_MAP_KEYS
		pathNames[depth] = keyLabel
		pathIndexes[depth] = -1
		invalidatePathCacheFrom(depth)
	}

	/**
	 * Pushes a map-value validation frame (path `parent[label]`, depth + 1).
	 */
	fun pushMapValue(keyLabel: String, valueShape: TypeShape?) {
		saveFrame()
		fieldName = keyLabel
		shape = valueShape
		depth += 1
		ensurePathCapacity(depth)
		pathOps[depth] = OP_MAP_VALUE
		pathNames[depth] = keyLabel
		pathIndexes[depth] = -1
		invalidatePathCacheFrom(depth)
	}

	/**
	 * Pushes a frame that only replaces [containerObject] (flat query/header/path section root).
	 */
	fun pushSectionContainer(container: ValidationContextValue<Any>?) {
		saveFrame()
		containerObject = container
	}

	/**
	 * Sets root [shape] / [containerObject] without pushing a frame or changing [depth].
	 */
	fun bindRoot(shape: TypeShape?, container: ValidationContextValue<Any>?) {
		this.shape = shape
		this.containerObject = container
	}

	override fun hasFailedPath(): Boolean {
		val set = failedPaths ?: return false
		return fieldPath in set
	}

	override fun markPathFailed() {
		val set = failedPaths ?: HashSet<String>(8).also { failedPaths = it }
		set.add(fieldPath)
	}

	/**
	 * Restores the previous frame. Must pair every successful `push*` on every exit path.
	 *
	 * @throws IllegalStateException When the frame stack is empty (missing push or double pop).
	 */
	fun pop() {
		check(frameDepth > 0) {
			"ValidationCursor.pop() with empty stack — missing push or double pop"
		}
		frameDepth -= 1
		val frame = checkNotNull(frameStack[frameDepth]) {
			"ValidationCursor.pop() missing frame at depth $frameDepth"
		}
		val previousDepth = depth
		fieldName = frame.fieldName
		shape = frame.shape
		elementIndex = frame.elementIndex
		array = frame.array
		containerObject = frame.containerObject
		depth = frame.depth
		// Path components above the restored depth are stale; keep lower depths valid.
		invalidatePathCacheFrom(depth + 1)
		if (previousDepth != depth) {
			cachedFieldPath = null
		}
	}

	/**
	 * Saves the current walk state into the next [frameStack] slot and increments [frameDepth].
	 */
	private fun saveFrame() {
		if (frameDepth == frameStack.size) {
			frameStack = frameStack.copyOf(frameStack.size * 2)
		}
		var slot = frameStack[frameDepth]
		if (slot == null) {
			slot = ValidationCursorFrame()
			frameStack[frameDepth] = slot
		}
		slot.fieldName = fieldName
		slot.shape = shape
		slot.elementIndex = elementIndex
		slot.array = array
		slot.containerObject = containerObject
		slot.depth = depth
		frameDepth += 1
	}

	private fun ensurePathCapacity(neededDepth: Int) {
		if (neededDepth < pathOps.size) return
		var cap = pathOps.size
		while (cap <= neededDepth) cap *= 2
		pathOps = pathOps.copyOf(cap)
		pathNames = pathNames.copyOf(cap)
		pathIndexes = pathIndexes.copyOf(cap)
		pathSegCache = pathSegCache.copyOf(cap)
		pathSegValid = pathSegValid.copyOf(cap)
	}

	private fun invalidatePathCacheFrom(fromDepth: Int) {
		cachedFieldPath = null
		val start = fromDepth.coerceAtLeast(0)
		for (d in start until pathSegValid.size) {
			pathSegValid[d] = false
			pathSegCache[d] = null
		}
	}

	/**
	 * Builds / refreshes [pathSegCache] from depth 1..[depth], reusing valid parent nodes so
	 * sibling indexes share [PathSegment.beforeLastIndex] identity.
	 */
	private fun ensurePathMaterialized(): PathSegment {
		if (depth == 0) return PathSegment.Root
		var parent: PathSegment = PathSegment.Root
		for (d in 1..depth) {
			if (pathSegValid[d]) {
				parent = pathSegCache[d]!!
				continue
			}
			val next = appendSegment(parent, pathOps[d], pathNames[d], pathIndexes[d])
			pathSegCache[d] = next
			pathSegValid[d] = true
			parent = next
		}
		return parent
	}

	private fun buildFieldPathString(): String {
		if (depth == 0) return ""
		return buildString {
			for (d in 1..depth) {
				when (pathOps[d]) {
					OP_NAME -> {
						val name = pathNames[d] ?: continue
						if (name.isEmpty()) continue
						if (isNotEmpty()) append('.')
						append(name)
					}
					OP_INDEX -> append('[').append(pathIndexes[d]).append(']')
					OP_MAP_KEYS -> {
						if (isNotEmpty()) append('.')
						append("keys[").append(pathNames[d]).append(']')
					}
					OP_MAP_VALUE -> append('[').append(pathNames[d]).append(']')
				}
			}
		}
	}

	companion object {

		private const val INITIAL_FRAME_CAPACITY: Int = 8

		internal const val OP_ROOT: Byte = 0
		internal const val OP_NAME: Byte = 1
		internal const val OP_INDEX: Byte = 2
		internal const val OP_MAP_KEYS: Byte = 3
		internal const val OP_MAP_VALUE: Byte = 4

		/**
		 * Creates a root cursor for one validation run.
		 */
		fun root(
			oneErrorPerParam: Boolean,
			failFast: Boolean = false,
			groups: Set<KClass<*>>,
			skipGroupChecks: Boolean = false,
			shape: TypeShape? = null,
			containerObject: ValidationContextValue<Any>? = null,
			attributeBag: AttributeBag = AttributeBag(),
			clock: Clock = Clock.systemDefaultZone(),
		): ValidationCursor {
			val cursor = ValidationCursor(
				oneErrorPerParam = oneErrorPerParam,
				failFast = failFast,
				groups = groups,
				skipGroupChecks = skipGroupChecks,
				attributeBag = attributeBag,
				clock = clock)
			cursor.shape = shape
			cursor.containerObject = containerObject
			cursor.pathOps[0] = OP_ROOT
			return cursor
		}

		private fun appendSegment(
			parent: PathSegment,
			op: Byte,
			name: String?,
			index: Int,
		): PathSegment = when (op) {
			OP_NAME -> {
				val n = name.orEmpty()
				if (n.isEmpty()) parent else PathSegment.Name(parent, n)
			}
			OP_INDEX -> PathSegment.Index(parent, index)
			OP_MAP_KEYS -> PathSegment.MapKeys(parent, name.orEmpty())
			OP_MAP_VALUE -> PathSegment.MapValue(parent, name.orEmpty())
			else -> parent
		}
	}
}
