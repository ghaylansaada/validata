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
import io.ghaylan.validata.schema.spi.ObjectSchemaModule

/**
 * Intermediate schema graph for one `@Validatable` type, built **before** Kotlin source is emitted.
 *
 * ## Pipeline position
 * ```
 * KSClassDeclaration  →  SchemaModel  →  SchemaCodeWriter  →  ObjectSchema in generated .kt
 * ```
 *
 * Kept `internal`: consumers never see these types. The public artifact of the processor is the
 * generated [ObjectSchemaModule], not this model.
 *
 * ## Polymorphic roots
 * When [isPolymorphicRoot] is `true`, [properties] is empty and [subtypeQualifiedNames] lists the
 * concrete types the runtime may dispatch to. Concrete subtypes are separate [SchemaModel]s.
 *
 * @property packageName Kotlin package of the source type (feeds GeneratedPackageNamer)
 * @property simpleName Unqualified class name (diagnostics / readability)
 * @property qualifiedName Fully qualified class name — runtime map key for [ObjectSchema]
 * @property properties Readable properties in declaration order (empty for polymorphic roots)
 * @property isPolymorphicRoot `true` for sealed/abstract/interface roots that only declare subtypes
 * @property subtypeQualifiedNames Concrete subtype FQCNs for polymorphic dispatch
 * 
 * @author Ghaylan Saada
 */
internal data class SchemaModel(
	val packageName: String,
	val simpleName: String,
	val qualifiedName: String,
	val properties: List<PropertyModel>,
	val isPolymorphicRoot: Boolean,
	val subtypeQualifiedNames: List<String>
)
