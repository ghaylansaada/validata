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
 * Intermediate model for one docs-only error code (mirrors `SchemaErrorDoc` for codegen).
 *
 * @property code Machine-readable code
 * @property message Documentation message (may be empty until OpenAPI resolves the catalog)
 * @property catalogFqcn Enum catalog FQCN (`ConstraintErrorDefinition`); always set when emitted
 * 
 * @author Ghaylan Saada
 */
internal data class SchemaErrorDocModel(
	val code: String,
	val message: String = "",
	val catalogFqcn: String? = null
)