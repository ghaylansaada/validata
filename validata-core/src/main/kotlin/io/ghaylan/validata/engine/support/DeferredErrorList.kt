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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.model.ConstraintError

/**
 * [MutableList] that allocates its backing [ArrayList] only on the first [add].
 *
 * Success-path validation never calls [add], so the run pays only for this thin wrapper
 * (no element array). [snapshot] returns the shared [emptyList] when nothing was added.*
 * 
 * @author Ghaylan Saada
 */
internal class DeferredErrorList: AbstractMutableList<ConstraintError<*>>() {
	
	private var buf: ArrayList<ConstraintError<*>>? = null
	
	private fun ensure(): ArrayList<ConstraintError<*>> = buf
		?: ArrayList<ConstraintError<*>>(8).also { buf = it }
	
	override val size: Int
		get() = buf?.size ?: 0
	
	override fun get(index: Int): ConstraintError<*> =
		(buf ?: throw IndexOutOfBoundsException(index))[index]
	
	override fun add(
		index: Int,
		element: ConstraintError<*>
	) = ensure().add(index, element)
	
	override fun removeAt(index: Int): ConstraintError<*> =
		(buf ?: throw IndexOutOfBoundsException(index)).removeAt(index)
	
	override fun set(
		index: Int,
		element: ConstraintError<*>
	): ConstraintError<*> =
		(buf ?: throw IndexOutOfBoundsException(index)).set(index, element)
	
	/**
	 * Immutable view for return to callers: shared empty list, or the grown buffer.
	 */
	fun snapshot(): List<ConstraintError<*>> = buf ?: emptyList()
}
