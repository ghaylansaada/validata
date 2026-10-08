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
 * List, set, collection, or array [TypeShape].
 *
 * Element type-use constraints (e.g. `List<@Email String>`) live on [element]. Property-level
 * constraints on the list field itself (`@Size` / `@Required` on `emails`) live on the owning
 * property's constraint list (`PropertySpec.constraints`).
 *
 * @property element Shape of each element (may itself be nested).
 * @property constraints Constraints on the iterable type-use node (rare).
 * 
 * @author Ghaylan Saada
 */
data class IterableShape(
	val element: TypeShape,
	override val constraints: List<CompiledConstraint> = emptyList(),
): TypeShape
