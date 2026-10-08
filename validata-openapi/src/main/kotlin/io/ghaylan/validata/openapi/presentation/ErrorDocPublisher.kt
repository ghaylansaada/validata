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
 * SPI that turns declared error facts ([DeclaredErrorSurface]) into OpenAPI documentation.
 *
 * Annotations / IR declare codes, messages, and optional docs only; Validata does not force
 * response schemas or envelopes. Apps register zero or more beans;
 * `io.ghaylan.validata.openapi.springdoc.ValidataOperationCustomizer` invokes each
 * (Spring `Ordered` / `@Order` when present).
 *
 * Evolve via additive [DeclaredErrorSurface] properties / [DeclaredErrorSurface.attributes] and new
 * publisher beans; ignore unknown [ErrorDocPublishContext.attributes] / surface attributes.
 *
 * Response envelopes (Problem Details, custom JSON, …) belong in the host app.
 * */
fun interface ErrorDocPublisher {
	
	/**
	 * Publishes using [ErrorDocPublishContext.surface] (and optional host handles / attributes).
	 *
	 * When [ErrorDocPublishContext.operation] is non-null, publishers may mutate it.
	 *
	 * @param context per-operation publish request	 
	 */
	fun publish(context: ErrorDocPublishContext)
}
