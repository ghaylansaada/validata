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
 * Nested object shape referencing another `@Validatable` type by FQCN.
 *
 * Codegen emits `ObjectRefShape(lazy { PeerSchema.build() }, …)`. The lazy factory:
 * - Loads the nested schema on first use
 * - Breaks cycles between mutually recursive types without truncating the graph at build time
 *
 * @property typeQualifiedName Target type FQCN (must match a [SchemaModel.qualifiedName] in the same round when possible)
 * @property constraints Type-use constraints on the object node (usually empty)
 * 
 * @author Ghaylan Saada
 */
internal data class ObjectRefShapeModel(
	val typeQualifiedName: String,
	override val constraints: List<ConstraintModel> = emptyList(),
): ShapeModel