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
package io.ghaylan.validata.schema.docs

/**
 * Docs-only error code baked into schema IR at compile time (KSP).
 *
 * Mirrors OpenAPI `@ApiError` without depending on the OpenAPI module. The validation engine never
 * reads this type — OpenAPI enrichment publishes codes and messages without re-scanning source
 * annotations. [message] is documentation text for humans / OpenAPI, not a runtime exception message.
 *
 * When [message] is blank and [catalogFqcn] is set, OpenAPI resolves the catalog enum entry
 * (`ConstraintErrorDefinition`) at docs time and uses that entry’s message (and `code` when needed).
 *
 * @property code Machine-readable code (e.g. `EMAIL_TAKEN`). Never blank when emitted by KSP.
 * @property message Human-readable documentation; empty means “resolve from catalog or no description”.
 * @property catalogFqcn Binary name of the `@ApiError` catalog enum (`ConstraintErrorDefinition`);
 *   always set for KSP-emitted docs.
 * 
 * @author Ghaylan Saada
 */
data class SchemaErrorDoc(
	val code: String,
	val message: String = "",
	val catalogFqcn: String? = null)