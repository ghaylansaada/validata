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
 * One property on a generated `*Constraint` metadata class.
 *
 * @property name Kotlin property name
 * @property typeSource type as rendered in source (e.g. `Int`, `Set<String>`, `Distinct.DistinctMode`)
 * @property isMessage `true` when this is `override val message`
 * @property isGroups `true` when this is `override val groups`
 * @property markerAnnotationsSource full annotation lines to emit above the property (e.g.
 *   `@ConstraintArg(…)`), empty for message/groups roles
 * @property typeImports FQCNs to import for [typeSource] (enums, nested annotation types, …)
 * 
 * @author Ghaylan Saada
 */
internal data class GeneratedMetadataPropertyModel(
	val name: String,
	val typeSource: String,
	val isMessage: Boolean = false,
	val isGroups: Boolean = false,
	val markerAnnotationsSource: List<String> = emptyList(),
	val typeImports: List<String> = emptyList(),
)
