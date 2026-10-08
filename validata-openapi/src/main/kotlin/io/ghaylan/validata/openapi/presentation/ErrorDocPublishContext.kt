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
package io.ghaylan.validata.openapi.presentation

import io.ghaylan.validata.ext.MethodUniqueIdentifiers
import io.swagger.v3.oas.models.Operation
import org.springframework.web.method.HandlerMethod

/**
 * Per-operation input for [ErrorDocPublisher].
 *
 * Host-agnostic facts live on [surface] / [endpointId] / [attributes]. Spring MVC / springdoc
 * handles are optional so publishers and unit tests can run without a web stack when they only
 * need the declared error surface.
 *
 * [attributes] is a forward-compatible bag — unknown keys must be ignored.
 *
 * @property surface merged IR + docs error facts for this endpoint
 * @property endpointId Validata endpoint id (from [MethodUniqueIdentifiers] when built by springdoc)
 * @property operation springdoc operation being customized, or `null` when unavailable
 * @property handlerMethod Spring MVC handler, or `null` when unavailable
 * @property attributes optional forward-compatible metadata (default empty)
 * 
 * @author Ghaylan Saada
 */
data class ErrorDocPublishContext(
	val surface: DeclaredErrorSurface,
	val endpointId: String,
	val operation: Operation? = null,
	val handlerMethod: HandlerMethod? = null,
	val attributes: Map<String, Any?> = emptyMap())