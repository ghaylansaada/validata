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

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * One classified handler parameter ready for endpoint schema codegen.
 *
 * Flat sections ([EndpointArgumentKind.QUERY] / [EndpointArgumentKind.HEADER] / [EndpointArgumentKind.PATH]) carry a full [PropertyModel]-shaped
 * payload. [EndpointArgumentKind.BODY] only needs the body type FQCN — the generated factory looks
 * up that type's [ObjectSchema] at class-load time.
 *
 * @property kind Transport classification ([EndpointArgumentKind] from validata-schema).
 * @property declaredName Kotlin parameter name as in source.
 * @property resolvedName Spring-effective name (`name` / `value` / declared name).
 * @property readerExpr Source text for a [ValueReader] over a transport map; unused for body.
 * @property shape Structural type tree for flat parameters; unused for body.
 * @property constraints Property-level constraints for flat parameters; unused for body.
 * @property bodyTypeQualifiedName `@RequestBody` type FQCN when [kind] is [EndpointArgumentKind.BODY]; otherwise null.
 * @property bodyIsCollection `true` when the body is a List/Set/Array of [bodyElementTypeQualifiedName].
 * @property bodyElementTypeQualifiedName Element FQCN when [bodyIsCollection] is true; otherwise null.
 * @property errorDocs Docs-only `@ApiError` codes on this parameter (flat transport params only).
 * 
 * @author Ghaylan Saada
 */
internal data class EndpointParameterModel(
	val kind: EndpointArgumentKind,
	val declaredName: String,
	val resolvedName: String,
	val readerExpr: String = "",
	val shape: ShapeModel = DynamicShapeModel(),
	val constraints: List<ConstraintModel> = emptyList(),
	val bodyTypeQualifiedName: String? = null,
	val bodyIsCollection: Boolean = false,
	val bodyElementTypeQualifiedName: String? = null,
	val errorDocs: List<SchemaErrorDocModel> = emptyList(),
)
