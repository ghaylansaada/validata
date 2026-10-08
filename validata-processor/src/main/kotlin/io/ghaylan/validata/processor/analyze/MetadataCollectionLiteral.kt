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
package io.ghaylan.validata.processor.analyze

/**
 * Chooses `setOf`/`emptySet` vs `listOf`/`emptyList` for generated metadata constructor args.
 *
 * Prefer the named helpers ([emptySet], [emptyList], [setOf], [listOf]) at call sites.
 * [render] remains for callers that still branch on booleans.*
 * 
 * @author Ghaylan Saada
 */
internal object MetadataCollectionLiteral {
	
	/**
	 * Kotlin source for an empty set literal.
	 *
	 * No side effects.
	 *
	 * @return `"emptySet()"`	 
	 */
	fun emptySet(): String =
		"emptySet()"
	
	/**
	 * Kotlin source for an empty list literal.
	 *
	 * No side effects.
	 *
	 * @return `"emptyList()"`	 
	 */
	fun emptyList(): String =
		"emptyList()"
	
	/**
	 * Kotlin source for a non-empty set literal.
	 *
	 * No side effects.
	 *
	 * @param elements comma-separated element expressions (may be empty for `setOf()`)
	 * @return `"setOf($elements)"`	 
	 */
	fun setOf(elements: String): String =
		"setOf($elements)"
	
	/**
	 * Kotlin source for a non-empty list literal.
	 *
	 * No side effects.
	 *
	 * @param elements comma-separated element expressions (may be empty for `listOf()`)
	 * @return `"listOf($elements)"`	 
	 */
	fun listOf(elements: String): String =
		"listOf($elements)"
	
	/**
	 * Dispatches to the named helper from [useSet] and [empty].
	 *
	 * No side effects.
	 *
	 * @param useSet when `true`, emit set literals; otherwise list literals
	 * @param empty when `true`, emit empty collection; otherwise wrap [elements]
	 * @param elements comma-separated element expressions for non-empty collections
	 * @return Kotlin collection literal source text	 
	 */
	fun render(
		useSet: Boolean,
		empty: Boolean,
		elements: String
	): String =
		when {
			empty && useSet -> emptySet()
			empty -> emptyList()
			useSet -> setOf(elements)
			else -> listOf(elements)
		}
}
