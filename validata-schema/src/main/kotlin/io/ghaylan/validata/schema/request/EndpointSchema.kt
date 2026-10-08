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
package io.ghaylan.validata.schema.request

import io.ghaylan.validata.schema.ObjectSchema
import kotlin.reflect.KClass

/**
 * Compiled validation IR for one `@Validate` endpoint (transport sections + argument layout).
 *
 * Distinct from object-graph IR ([ObjectSchema]). Empty sections stay `null`; the Spring host binds args via [argumentLayout].
 *
 * @property id Endpoint coordinate matching `Method.getUniqueIdentifier()` / compile-time `EndpointIdentifier`
 * @property pathVariables Path-variable schema, or `null` when empty
 * @property headers Header schema, or `null` when empty
 * @property queryParams Query-parameter schema, or `null` when empty
 * @property requestBody Body [ObjectSchema] from classpath SPI (`GeneratedSchemas`), or `null` when absent
 * @property oneErrorPerParam When `true`, stop after the first failure on a single param
 * @property failFast When `true`, stop after the first failure anywhere in the request
 * @property groups Active validation groups from `@Validate`
 * @property argumentLayout Positional parameter → transport slots; indices match Spring's args array
 * 
 * @author Ghaylan Saada
 */
data class EndpointSchema(
	val id: String,
	val pathVariables: ObjectSchema? = null,
	val headers: ObjectSchema? = null,
	val queryParams: ObjectSchema? = null,
	val requestBody: ObjectSchema? = null,
	val oneErrorPerParam: Boolean = true,
	val failFast: Boolean = false,
	val groups: Set<KClass<*>>,
	val argumentLayout: List<EndpointArgumentSlot> = emptyList())
