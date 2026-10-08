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
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.schema.Validatable

@Validatable(
	discriminator = "context",
	subtypes = [
		Validatable.Subtype(name = "PROFILE_AVATAR", type = AvatarUploadDto::class),
	],
)
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXISTING_PROPERTY,
	property = "context",
	visible = true,
)
@JsonSubTypes(JsonSubTypes.Type(AvatarUploadDto::class, name = "PROFILE_AVATAR"))
sealed interface UploadDto {
	
	@get:Required
	@get:JsonProperty("context")
	val context: UploadContext?
	
	@get:Required
	@get:JsonProperty("content_type")
	val contentType: String?
}
