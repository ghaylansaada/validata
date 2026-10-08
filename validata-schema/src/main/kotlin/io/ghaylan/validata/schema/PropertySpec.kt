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
package io.ghaylan.validata.schema

import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * One property in an [ObjectSchema] (KSP-emitted or hand-written for tests).
 *
 * Cross-field references use [declaredName]; client-visible error paths use [externalName].
 *
 * @property declaredName Source / `@PropertyRef` name.
 * @property externalName Wire / error-path name.
 * @property shape Structural type, including nested type-use constraints.
 * @property read Extracts the value from a container.
 * @property constraints Property-level constraints only; type-use constraints live on [shape].
 * @property errorDocs Docs-only OpenAPI `@ApiError` codes; ignored by the engine.
 * 
 * @author Ghaylan Saada
 */
data class PropertySpec(
	val declaredName: String,
	val externalName: String,
	val shape: TypeShape,
	val read: ValueReader,
	val constraints: List<CompiledConstraint> = emptyList(),
	val errorDocs: List<SchemaErrorDoc> = emptyList())
