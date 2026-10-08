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
package io.ghaylan.validata.web.fixture.dto.upload

import com.fasterxml.jackson.annotation.JsonProperty
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.schema.Validatable

@Validatable
data class AvatarUploadDto(
	override val context: UploadContext?,
	override val contentType: String?,
	@get:Size(min = 1, max = 64)
	@get:JsonProperty("display_name")
	val displayName: String?,
): UploadDto
