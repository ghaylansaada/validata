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
package io.ghaylan.validata.samples.error

/**
 * Sample app-owned business failure (not a Validata type).
 *
 * Field-level OpenAPI docs for related codes use [SampleApiErrors] + `@ApiError` separately.
 *
 * @param httpStatus HTTP status to write on the envelope
 * @param code Catalog constant name (e.g. `USER_NOT_FOUND`)
 * @param message Human-readable explanation shown to the API client
 * 
 * @author Ghaylan Saada
 */
class SampleBusinessException(
	val httpStatus: Int,
	val code: String,
	message: String,
): RuntimeException(message, null, false, false)
