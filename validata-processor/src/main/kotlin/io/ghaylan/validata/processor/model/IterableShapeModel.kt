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
package io.ghaylan.validata.processor.model

/**
 * List / set / collection / array shape.
 *
 * Arrays are modelled the same way as lists so the runtime engine walks elements uniformly.
 * Element type-use constraints (e.g. `List<@Email String>`) live on [element], not on this node’s
 * [constraints] (those are constraints on the iterable type use itself, if any).
 *
 * @property element Shape of each element (scalar, object-ref, nested iterable, …)
 * @property constraints Constraints on the iterable type-use node
 * 
 * @author Ghaylan Saada
 */
internal data class IterableShapeModel(
	val element: ShapeModel,
	override val constraints: List<ConstraintModel> = emptyList(),
): ShapeModel
