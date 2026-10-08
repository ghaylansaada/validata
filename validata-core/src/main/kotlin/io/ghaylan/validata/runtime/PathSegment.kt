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
package io.ghaylan.validata.runtime

/**
 * Immutable linked path segment that extends a validation path in O(1) and materializes it only
 * when an error needs its observable string form.
 *
 * Preserves legacy path syntax: object names, collection indices, and map key/value paths.*
 * 
 * @author Ghaylan Saada
 */
sealed class PathSegment {
	
	/**
	 * Parent segment toward the root, or `null` at [Root].
	 */
	abstract val parent: PathSegment?
	
	/**
	 * Materializes the full path string from the root to this segment.
	 *
	 * @return Dot/bracket path (e.g. `user.address[0].city`); empty at [Root].	 
	 */
	fun toPathString(): String {
		val segments = ArrayDeque<PathSegment>()
		var current: PathSegment? = this
		
		while (current != null && current !is Root) {
			segments.addFirst(current)
			current = current.parent
		}
		
		return buildString {
			for (segment in segments) {
				when (segment) {
					is Root -> Unit
					is Name -> if (segment.name.isNotEmpty()) {
						if (isNotEmpty()) append('.')
						append(segment.name)
					}
					
					is Index -> append('[')
						.append(segment.index)
						.append(']')
					
					is MapKeys -> {
						if (isNotEmpty()) append('.')
						append("keys[")
							.append(segment.keyLabel)
							.append(']')
					}
					
					is MapValue -> append('[')
						.append(segment.keyLabel)
						.append(']')
				}
			}
		}
	}
	
	/**
	 * Returns the path before its nearest collection index, matching
	 * `fieldPath.substringBeforeLast('[')` without materializing the whole field path.
	 *
	 * @return Parent of the nearest [Index], [Root] if that parent is null, or `this` when no index.	 
	 */
	fun beforeLastIndex(): PathSegment {
		val original = this
		var current: PathSegment = this
		while (current !is Root) {
			if (current is Index) return current.parent ?: Root
			current = current.parent ?: Root
		}
		return original
	}
	
	/**
	 * Root of every validation path.
	 */
	data object Root: PathSegment() {
		
		/**
		 * Always `null` at the root.
		 */
		override val parent: PathSegment? = null
	}
	
	/**
	 * Named object property segment.
	 *
	 * @property parent Parent toward the root.
	 * @property name Property / field name (empty names are omitted when materializing).
	 */
	class Name(
		override val parent: PathSegment?,
		val name: String
	): PathSegment()
	
	/**
	 * Collection index segment (`[index]`).
	 *
	 * @property parent Parent toward the root.
	 * @property index Zero-based element index.
	 */
	class Index(
		override val parent: PathSegment?,
		val index: Int
	): PathSegment()
	
	/**
	 * Map key-validation segment (`keys[label]`).
	 *
	 * @property parent Parent toward the root.
	 * @property keyLabel Display label for the map key under validation.
	 */
	class MapKeys(
		override val parent: PathSegment?,
		val keyLabel: String
	): PathSegment()
	
	/**
	 * Map value segment (`[label]`).
	 *
	 * @property parent Parent toward the root.
	 * @property keyLabel Display label for the map entry whose value is under validation.
	 */
	class MapValue(
		override val parent: PathSegment?,
		val keyLabel: String
	): PathSegment()
}
