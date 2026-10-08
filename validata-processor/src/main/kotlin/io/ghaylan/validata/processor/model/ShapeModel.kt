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

import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Intermediate structural shape mirroring the runtime [TypeShape] hierarchy.
 *
 * ## Variants (one file each)
 * | Model | Runtime counterpart | Meaning |
 * |---|---|---|
 * | [ScalarShapeModel] | `ScalarShape` | Leaf (String, Int, Instant, …) |
 * | [ObjectRefShapeModel] | `ObjectRefShape` | Nested `@Validatable` object |
 * | [IterableShapeModel] | `IterableShape` | List / set / array |
 * | [MapShapeModel] | `MapShape` | Map with key + value nodes |
 * | [DynamicShapeModel] | `DynamicShape` | Opaque / unresolved / `@NoCascade` |
 *
 * Type-use constraints (e.g. `List<@Email String>`) hang on [constraints] of the relevant node,
 * not on [PropertyModel.constraints].
 *
 * Built by `ShapeModelBuilder`; rendered by `SchemaCodeWriter`.
 * 
 * @author Ghaylan Saada
 */
internal sealed interface ShapeModel {
	
	/**
	 * Type-use constraints attached to this node (may be empty).
	 *
	 * Property-level annotations such as `@field:Required` live on [PropertyModel], not here.
	 */
	val constraints: List<ConstraintModel>
}
