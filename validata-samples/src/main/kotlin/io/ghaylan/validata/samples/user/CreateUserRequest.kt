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
package io.ghaylan.validata.samples.user

import com.fasterxml.jackson.annotation.JsonProperty
import io.ghaylan.validata.constraint.annotation.Compare
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.openapi.presentation.ApiError
import io.ghaylan.validata.samples.constraint.OddYears
import io.ghaylan.validata.samples.constraint.SampleFloor
import io.ghaylan.validata.samples.error.SampleApiErrors
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.Validatable

/**
 * Sample body DTO: presence/format constraints, [OddYears], [SampleFloor], and a cross-field
 * [Compare] reference.
 *
 * [name] is published as wire name `first_name` so OpenAPI and error paths exercise
 * [PropertySpec.externalName]. Field-level [ApiError] on [name] bakes into `PropertySpec.errorDocs`.
 *
 * Schema IR is emitted by KSP with no reflective fallback.
 *
 * @property name Display name on the wire as `first_name`; required, 2–40 characters
 * @property minAge Inclusive floor 3 ([SampleFloor]) and must be odd ([OddYears])
 * @property maxAge Required and must be strictly greater than [minAge]
 * 
 * @author Ghaylan Saada
 */
@Validatable
data class CreateUserRequest(
	@field:ApiError(code = "NAME_INVALID", catalog = SampleApiErrors::class)
	@field:Required
	@field:Size(min = 2, max = 40)
	@get:JsonProperty("first_name")
	val name: String,
	
	@field:Required
	@field:SampleFloor("3")
	@field:OddYears
	@get:JsonProperty("min_age")
	val minAge: Int,
	
	@field:Required
	@field:Compare(ref = "minAge", operation = Compare.Operation.GT)
	@get:JsonProperty("max_age")
	val maxAge: Int)
