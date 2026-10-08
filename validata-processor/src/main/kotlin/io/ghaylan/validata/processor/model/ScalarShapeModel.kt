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

import io.ghaylan.validata.schema.shape.ScalarKind

/**
 * Leaf shape for a scalar / temporal / UUID-like type.
 *
 * [kind] is the [ScalarKind] enum value so writers emit `ScalarKind.${kind.name}` without
 * stringly-typed typos that only fail when generated sources compile.
 *
 * @property kind Leaf classification from TypeClassification.scalarKind
 *   (or [ScalarKind.ENUM] / [ScalarKind.OTHER] at construction sites)
 * @property typeQualifiedName Concrete leaf FQCN when known (needed for temporal literal checks)
 * @property constraints Type-use constraints on this leaf (rare; usually empty)
 * 
 * @author Ghaylan Saada
 */
internal data class ScalarShapeModel(
	val kind: ScalarKind,
	val typeQualifiedName: String? = null,
	override val constraints: List<ConstraintModel> = emptyList(),
): ShapeModel
