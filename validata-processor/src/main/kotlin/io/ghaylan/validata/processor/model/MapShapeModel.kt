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
 * Map shape with independent key and value nodes.
 *
 * Keys and values can carry different type-use constraints
 * (`Map<@Size String, @Email String>` → constraints on [key] and [value] respectively).
 *
 * @property key Shape of map keys
 * @property value Shape of map values
 * @property constraints Constraints on the map type-use node itself
 * 
 * @author Ghaylan Saada
 */
internal data class MapShapeModel(
	val key: ShapeModel,
	val value: ShapeModel,
	override val constraints: List<ConstraintModel> = emptyList(),
): ShapeModel
