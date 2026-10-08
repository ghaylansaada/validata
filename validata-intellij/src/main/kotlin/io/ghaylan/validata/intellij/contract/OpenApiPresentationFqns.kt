/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.contract

/**
 * Fully-qualified names of OpenAPI presentation annotations discovered on the **user project**
 * classpath.
 *
 * These types live in `:validata-openapi`. The IntelliJ plugin does not depend on that module;
 * discovery resolves by these FQCN strings (or short-name fallback when the declaration is
 * unresolved). When openapi is absent from a project, annotators / references that key off
 * these FQCNs are no-ops — other Validata IDE features keep working.
 *
 * Keep aligned with `io.ghaylan.validata.openapi.presentation.*` and with processor
 * `OpenApiPresentationFqns`.
 * 
 * @author Ghaylan Saada
 */
internal object OpenApiPresentationFqns {
	
	/**
	 * FQCN of `@ApiError` — docs-only machine code (+ message / catalog) on fields/params.
	 */
	const val API_ERROR: String = "io.ghaylan.validata.openapi.presentation.ApiError"
	
	/**
	 * Required `@ApiError` catalog interface FQCN.
	 */
	const val CONSTRAINT_ERROR_DEFINITION: String = "io.ghaylan.validata.model.ConstraintErrorDefinition"
}
