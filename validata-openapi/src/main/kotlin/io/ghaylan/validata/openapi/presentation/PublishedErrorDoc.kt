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

import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * One published docs-only detail error entry for [ErrorDocPublisher]s.
 *
 * @property code Machine-readable code
 * @property message Documentation message (may be empty)
 * @property path Wire path when this doc came from a [PropertySpec]; `null` when path-less
 * @property location Request section ([EndpointArgumentKind]) when known; `null` when path-less
 * @property attributes Forward-compatible metadata bag (default empty)
 * 
 * @author Ghaylan Saada
 */
data class PublishedErrorDoc(
	val code: String,
	val message: String = "",
	val path: String? = null,
	val location: EndpointArgumentKind? = null,
	val attributes: Map<String, Any?> = emptyMap())
