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

/**
 * Immutable OpenAPI error-code **facts** for one endpoint.
 *
 * Built from Validata IR only (validation collector ∪ baked schema docs). Does not force a JSON
 * envelope — consumers map this into OpenAPI via [ErrorDocPublisher] beans.
 *
 * Additive evolution: new optional properties and [attributes] keys may appear in minor releases;
 * publishers must ignore unknown [attributes].
 *
 * @property detailCodes Sorted distinct detail codes (IR validation ∪ schema docs)
 * @property detailErrorDocs Schema-declared detail docs
 *   (with optional wire [PublishedErrorDoc.path] and [PublishedErrorDoc.location])
 * @property attributes Forward-compatible metadata bag (default empty)
 * 
 * @author Ghaylan Saada
 */
data class DeclaredErrorSurface(
	val detailCodes: List<String>,
	val detailErrorDocs: List<PublishedErrorDoc>,
	val attributes: Map<String, Any?> = emptyMap())