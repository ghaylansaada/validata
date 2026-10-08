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
package io.ghaylan.validata.samples.config

import io.ghaylan.validata.config.ConfigurationValidationPostProcessor
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.schema.Validatable
import io.ghaylan.validata.schema.Validate
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Sample `@ConfigurationProperties` bean validated at startup via [Validate].
 *
 * Exercises [ConfigurationValidationPostProcessor] in the sample app (including the GraalVM
 * path once `nativeTest` is run). Bound from `sample.app.*`.
 *
 * @property tenantId Required tenant identifier, 2–32 characters. Constructor-bound (`val`).
 * 
 * @author Ghaylan Saada
 */
@Validate
@Validatable
@ConfigurationProperties(prefix = "sample.app")
data class SampleAppProperties(
	@field:Required
	@field:Size(min = 2, max = 32)
	val tenantId: String)
