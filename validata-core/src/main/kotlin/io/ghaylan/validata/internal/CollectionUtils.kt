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
package io.ghaylan.validata.internal

import io.ghaylan.validata.internal.CollectionUtils.arrayHasNulls
import io.ghaylan.validata.internal.CollectionUtils.listHasNulls
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Normalizes arrays and collections for validators that iterate elements.
 *
 * Prefer zero-copy where safe. Primitive arrays use Kotlin's `.asList()` boxed views (no element
 * copy).*
 * 
 * @author Ghaylan Saada
 */
internal object CollectionUtils {
	
	/**
	 * Returns a null-free, index-stable list view of [value], or [emptyList] when [value] is `null`
	 * or not a recognized collection/array type.
	 *
	 * Used by `@Distinct` (and similar) where null elements are omitted from uniqueness. The engine
	 * element cascade must use [toIndexedElements] instead so type-use constraints still see nulls.
	 *
	 * Side effects: none (may allocate a filtered copy or thin view).
	 *
	 * @param value Raw property value (array, [List], other [Collection], or primitive array).
	 * @return Scrubbed list suitable for indexed uniqueness checks.	 
	 */
	fun normalizeList(value: Any?): List<Any> {
		if (value == null) return emptyList()
		
		return when (value) {
			is Array<*> -> normalizeArray(value)
			is List<*> -> normalizeListValue(value)
			is Collection<*> -> normalizeCollection(value)
			is BooleanArray -> value.asList()
			is ByteArray -> value.asList()
			is CharArray -> value.asList()
			is ShortArray -> value.asList()
			is IntArray -> value.asList()
			is LongArray -> value.asList()
			is FloatArray -> value.asList()
			is DoubleArray -> value.asList()
			else -> emptyList()
		}
	}
	
	/**
	 * Returns an index-stable list of elements including `null`s, for engine cascade walks.
	 *
	 * Preserves original indices so `List<@Required T?>` / path segments like `items[2]` stay
	 * aligned with the live payload. Unknown / non-collection values yield [emptyList].
	 *
	 * Side effects: none (may allocate a copy for non-[List] collections).
	 *
	 * @param value Raw iterable / array property value.
	 * @return Element list with nulls retained; empty when [value] is null or unsupported.	 
	 */
	fun toIndexedElements(value: Any?): List<Any?> {
		if (value == null) return emptyList()
		
		return when (value) {
			is Array<*> -> value.asList()
			is List<*> -> value
			is Collection<*> -> value.toList()
			is BooleanArray -> value.asList()
			is ByteArray -> value.asList()
			is CharArray -> value.asList()
			is ShortArray -> value.asList()
			is IntArray -> value.asList()
			is LongArray -> value.asList()
			is FloatArray -> value.asList()
			is DoubleArray -> value.asList()
			else -> emptyList()
		}
	}
	
	/**
	 * Same elements as [toIndexedElements], typed for [ValidationContext.array].
	 *
	 * The list may contain runtime `null`s (generics erased). One shared instance is reused for the
	 * element cascade and Distinct sibling lookups so large lists are not walked twice.
	 *
	 * @param value Raw iterable / array property value.
	 * @return Indexed element list (possibly containing nulls).	 
	 */
	@Suppress("UNCHECKED_CAST")
	fun toArrayContextList(value: Any?): List<Any> = toIndexedElements(value) as List<Any>
	
	/**
	 * Normalizes a reference [Array] to a null-free list.
	 *
	 * @param array Reference array to normalize.
	 * @return Zero-copy view when no nulls; otherwise a filtered copy.	 
	 */
	private fun normalizeArray(array: Array<*>): List<Any> {
		return if (!arrayHasNulls(array)) arrayReadOnlyView(array)
		else array.filterNotNull()
	}
	
	/**
	 * Normalizes a [List] to a null-free list.
	 *
	 * @param list List to normalize.
	 * @return Same instance as `List<Any>` when no nulls; otherwise a filtered copy.	 
	 */
	private fun normalizeListValue(list: List<*>): List<Any> {
		return when {
			list.isEmpty() -> emptyList()
			!listHasNulls(list) -> listReadOnlyView(list)
			else -> list.filterNotNull()
		}
	}
	
	/**
	 * Copies non-list collections (sets, queues) into an index-stable list.
	 *
	 * @param collection Non-[List] collection.
	 * @return Null-filtered list, or [emptyList] when empty.	 
	 */
	private fun normalizeCollection(collection: Collection<*>): List<Any> {
		return if (collection.isEmpty()) emptyList()
		else collection.filterNotNull()
	}
	
	/**
	 * Scans [array] for a null element.
	 *
	 * @param array Array to scan.
	 * @return `true` if any element is null.	 
	 */
	private fun arrayHasNulls(array: Array<*>): Boolean {
		for (e in array) if (e == null) return true
		return false
	}
	
	/**
	 * Scans [list] for a null element via index (no iterator allocation).
	 *
	 * @param list List to scan.
	 * @return `true` if any element is null.	 
	 */
	private fun listHasNulls(list: List<*>): Boolean {
		for (item in list) if (item == null) return true
		return false
	}
	
	/**
	 * Thin read-only view over a proven null-free array (`checkNotNull` after [arrayHasNulls]).
	 *
	 * @param array Source array with no null elements.
	 * @return [AbstractList] proxying [array].	 
	 */
	private fun arrayReadOnlyView(array: Array<*>): List<Any> {
		return object: AbstractList<Any>() {
			override val size: Int = array.size
			override fun get(index: Int): Any = checkNotNull(array[index]) {
				"arrayReadOnlyView index $index was null after arrayHasNulls precheck"
			}
		}
	}
	
	/**
	 * Returns [list] as `List<Any>` without allocating a wrapper.
	 *
	 * After [listHasNulls] is false, the unchecked cast is safe at runtime (generics erased).
	 * The result is a read-only *view* of the same instance — mutations to [list] are visible.
	 *
	 * @param list Source list already verified free of nulls.
	 * @return The same list typed as [List]<[Any]>.
	 */
	@Suppress("UNCHECKED_CAST")
	private fun listReadOnlyView(list: List<*>): List<Any> = list as List<Any>
}
