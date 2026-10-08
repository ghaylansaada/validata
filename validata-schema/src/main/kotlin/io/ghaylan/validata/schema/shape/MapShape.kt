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
 * Map [TypeShape] with independent key and value shapes.
 *
 * Named [MapShape] rather than `Map` so it does not shadow Kotlin stdlib `Map` at call sites.
 * Example: `Map<@Size(min=1) String, @Required AddressDto>` becomes a [MapShape] whose [key] is a
 * constrained [ScalarShape] and whose [value] is a constrained [ObjectRefShape].
 *
 * @property key Shape of map keys.
 * @property value Shape of map values.
 * @property constraints Constraints on the map type-use node itself (rare).
 * 
 * @author Ghaylan Saada
 */
data class MapShape(
	val key: TypeShape,
	val value: TypeShape,
	override val constraints: List<CompiledConstraint> = emptyList(),
): TypeShape
