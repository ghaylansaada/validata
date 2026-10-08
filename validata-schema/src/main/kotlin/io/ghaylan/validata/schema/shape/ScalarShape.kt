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

import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Leaf [TypeShape]: string, number, temporal, enum, and similar scalars.
 *
 * Used for walk dispatch and as the carrier of type-use constraints on scalar type arguments.
 *
 * @property kind Coarse leaf classification for schema-level decisions; validators still see the real value.
 * @property constraints Type-use constraints on this leaf (e.g. `@Email` on a `String` type argument).
 * 
 * @author Ghaylan Saada
 */
data class ScalarShape(
	val kind: ScalarKind,
	override val constraints: List<CompiledConstraint> = emptyList(),
): TypeShape
