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
package io.ghaylan.validata.schema.shape

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Structured object [TypeShape] whose properties are described by another [ObjectSchema].
 *
 * [ref] is [Lazy] so self-referencing models form a cycle in the schema graph rather than an
 * infinite expansion at build time. Evaluation depth is bounded by data and the engine's
 * traversal ceiling.
 *
 * Package note: this type intentionally references [ObjectSchema] in the parent package while
 * [ObjectSchema.selfRef] references this type — a closed IR object-graph edge, not an accidental
 * layering leak. FQN stays under `schema.shape` so KSP/`CodegenFqns` and generated sources remain stable.
 *
 * @property ref Lazily resolved nested object schema.
 * @property constraints Type-use constraints on the object type itself (rare).
 * 
 * @author Ghaylan Saada
 */
data class ObjectRefShape(
	val ref: Lazy<ObjectSchema>,
	override val constraints: List<CompiledConstraint> = emptyList(),
): TypeShape
