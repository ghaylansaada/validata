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
package io.ghaylan.validata.web

import io.ghaylan.validata.engine.ValidatorEngine

/**
 * Body plus optional transport-section maps produced by [RequestArgumentAssembler].
 *
 * Maps are `null` when the endpoint's [HandlerValidationPlan.Active] flags say that section has
 * no layout slots — callers must pass those nulls through to [ValidatorEngine.validateRequest]
 * so the engine skips empty work without the interceptor allocating throwaway [MutableMap]
 * instances.
 *
 * @property body First `@RequestBody` argument value, or `null` when absent / not a body endpoint.
 * @property headers Header name → value map, or `null` when no HEADER slots.
 * @property queryParams Query-parameter name → value map, or `null` when no QUERY slots.
 * @property pathVariables Path-variable name → value map, or `null` when no PATH slots.
 * 
 * @author Ghaylan Saada
 */
internal data class AssembledRequestArguments(
	val body: Any?,
	val headers: Map<String, Any?>?,
	val queryParams: Map<String, Any?>?,
	val pathVariables: Map<String, Any?>?)