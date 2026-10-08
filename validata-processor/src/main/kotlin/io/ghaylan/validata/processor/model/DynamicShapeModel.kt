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
 * Opaque / unresolved shape.
 *
 * Used when the processor cannot (or must not) describe structure further:
 * - `Any` / unknown types
 * - `@NoCascade` object properties
 * - Unmarked cascade targets after a cascade diagnostic (soft-fail so analysis continues)
 *
 * The engine still runs [constraints] on the value when present, but does not walk nested properties.
 *
 * @property constraints Type-use constraints on the dynamic node
 * 
 * @author Ghaylan Saada
 */
internal data class DynamicShapeModel(
	override val constraints: List<ConstraintModel> = emptyList(),
): ShapeModel
