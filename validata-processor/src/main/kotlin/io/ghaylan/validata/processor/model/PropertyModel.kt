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

import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader

/**
 * Intermediate model for one property on a [SchemaModel].
 *
 * Maps 1:1 onto a generated [PropertySpec] (names, shape, reader lambda, constraints).
 *
 * ## Two names
 * | Field | Used for |
 * |---|---|
 * | [declaredName] | Cross-field refs (`@Compare(ref = …)`, `@RequiredWhen(ref = …)`) |
 * | [externalName] | Wire / JSON name after `@JsonProperty`; error paths clients see |
 *
 * When there is no `@JsonProperty`, both names are equal.
 *
 * ## Reader
 * [readerExpr] is **source text**, not a live lambda — e.g. `{ (it as com.acme.User).name }` —
 * pasted as the `read =` SAM lambda by the code writer (shortened to an imported simple name when
 * possible). The engine only calls that lambda; it never reads fields or properties reflectively.
 *
 * @property declaredName Kotlin property name as in source
 * @property externalName Wire name ([declaredName] unless `@JsonProperty` overrides it)
 * @property readerExpr Kotlin lambda body for an [ValueReader]
 * @property shape Structural type tree (scalars, collections, nested objects, …)
 * @property constraints Property-level constraints in execution order (type-use constraints live on [shape])
 * @property noCascade When `true`, object-typed values are not emitted as [ObjectRefShapeModel]
 * @property errorDocs Docs-only `@ApiError` codes for this property (ignored by the validation engine)
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyModel(
	val declaredName: String,
	val externalName: String,
	val readerExpr: String,
	val shape: ShapeModel,
	val constraints: List<ConstraintModel>,
	val noCascade: Boolean,
	val errorDocs: List<SchemaErrorDocModel> = emptyList()
)