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
package io.ghaylan.validata.ext

import java.util.Collections
import java.util.IdentityHashMap
import java.util.Optional
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Returns whether this value is deeply null or empty.
 *
 * A value is considered absent when it is:
 *
 * - `null`;
 * - [Unit];
 * - a blank [CharSequence];
 * - an empty primitive array;
 * - an empty reference array;
 * - an empty [Collection];
 * - an empty [Map];
 * - an empty [Optional]; or
 * - a supported structured value whose nested values are all absent.
 *
 * [Sequence] values and unsupported object types are considered present.
 * Sequences are deliberately not evaluated because doing so may execute or
 * consume a lazy computation.
 *
 * Nested structures are traversed recursively using identity-based cycle
 * detection. A cycle is considered present at the cycle boundary.
 *
 * Map keys are not evaluated; only map values participate in determining
 * whether a map is deeply empty.
 *
 * Scalar values take a fast path and do not allocate cycle-detection state.
 * Identity-tracking state is allocated only for non-empty structured values.
 *
 * @return `true` if this value is deeply null or empty; `false` otherwise.
 */
@OptIn(ExperimentalContracts::class)
fun Any?.isDeepNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isDeepNullOrEmpty != null)
	}
	
	return this == null || when (this) {
		is Unit -> true
		is Optional<*> -> !isPresent
		is CharSequence -> isBlank()
		is CharArray -> isEmpty()
		is ByteArray -> isEmpty()
		is ShortArray -> isEmpty()
		is IntArray -> isEmpty()
		is LongArray -> isEmpty()
		is FloatArray -> isEmpty()
		is DoubleArray -> isEmpty()
		is BooleanArray -> isEmpty()
		is Sequence<*> -> false
		is Collection<*> -> {
			isEmpty() || deepNullOrEmpty(newIdentitySet())
		}
		
		is Array<*> -> {
			isEmpty() || deepNullOrEmpty(newIdentitySet())
		}
		
		is Map<*, *> -> {
			isEmpty() || deepNullOrEmpty(newIdentitySet())
		}
		
		is Pair<*, *>,
		is Triple<*, *, *> -> deepNullOrEmpty(newIdentitySet())
		else -> false
	}
}

/**
 * Recursively evaluates a value while preserving the identity set used for
 * cycle detection.
 *
 * This overload exists exclusively for recursive traversal and must never
 * allocate a new identity set.
 */
private fun Any?.deepNullOrEmpty(
	visited: MutableSet<Any>,
): Boolean {
	return this == null || when (this) {
		is Unit -> true
		is Optional<*> -> !isPresent
		is CharSequence -> isBlank()
		is CharArray -> isEmpty()
		is ByteArray -> isEmpty()
		is ShortArray -> isEmpty()
		is IntArray -> isEmpty()
		is LongArray -> isEmpty()
		is FloatArray -> isEmpty()
		is DoubleArray -> isEmpty()
		is BooleanArray -> isEmpty()
		is Sequence<*> -> false
		is Collection<*> -> deepNullOrEmptyStructured(visited)
		is Array<*> -> deepNullOrEmptyStructured(visited)
		is Map<*, *> -> deepNullOrEmptyStructured(visited)
		is Pair<*, *> -> deepNullOrEmptyStructured(visited)
		is Triple<*, *, *> -> deepNullOrEmptyStructured(visited)
		else -> false
	}
}

/**
 * Traverses a structured value using identity-based cycle detection.
 *
 * The identity set represents the current recursion path rather than all
 * objects encountered during the traversal. Consequently, shared references
 * in separate branches are evaluated independently.
 */
private fun Any.deepNullOrEmptyStructured(
	visited: MutableSet<Any>,
): Boolean {
	if (!visited.add(this)) {
		return false
	}
	
	try {
		return when (this) {
			is Collection<*> -> all { it.deepNullOrEmpty(visited) }
			is Array<*> -> all { it.deepNullOrEmpty(visited) }
			is Map<*,*> -> values.all { it.deepNullOrEmpty(visited) }
			is Pair<*,*> -> first.deepNullOrEmpty(visited) && second.deepNullOrEmpty(visited)
			is Triple<*,*,*> -> first.deepNullOrEmpty(visited) && second.deepNullOrEmpty(visited) && third.deepNullOrEmpty(visited)
			else -> false
		}
	} finally {
		visited.remove(this)
	}
}

/**
 * Creates an identity-based set for cycle detection.
 *
 * [IdentityHashMap] is required because cycle detection must use reference
 * identity rather than [Any.equals].
 */
private fun newIdentitySet(): MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())