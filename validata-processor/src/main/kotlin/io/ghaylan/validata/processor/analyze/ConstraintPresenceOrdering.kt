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

import io.ghaylan.validata.processor.model.ConstraintModel

/**
 * Sorts built constraints so presence annotations run before format checks, then renumbers
 * [ConstraintModel.order] by replacing list elements with [ConstraintModel.copy] (immutable `order`).*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintPresenceOrdering {
	
	/**
	 * Sorts [out] in place so presence constraints precede others, then renumbers [ConstraintModel.order].
	 *
	 * Mutates [out] (sort + in-place element replacement).
	 *
	 * @param out mutable constraint list built for one subject	 
	 */
	fun sortInPlace(out: MutableList<ConstraintModel>) {
		out.sortWith(
			compareBy<ConstraintModel> {
				if (CompositionOrRules.isPresenceAnnotation(it.annotationSimpleName)) 0 else 1
			}.thenBy { it.order },
		)
		out.forEachIndexed { index, model ->
			if (model.order != index) {
				out[index] = model.copy(order = index)
			}
		}
	}
}
