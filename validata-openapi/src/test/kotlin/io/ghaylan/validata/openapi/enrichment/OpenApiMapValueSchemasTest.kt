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
package io.ghaylan.validata.openapi.enrichment

import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards map-value schema resolution without clobbering `additionalProperties: false`.
 * 
 * @author Ghaylan Saada
 */
class OpenApiMapValueSchemasTest {
	
	@Test
	@DisplayName("existing Schema additionalProperties is reused")
	fun reusesExistingSchema() {
		val value = Schema<Any>()
		val schema = Schema<Any>().apply { additionalProperties = value }
		assertThat(OpenApiMapValueSchemas.resolve(schema)).isSameAs(value)
		assertThat(schema.additionalProperties).isSameAs(value)
	}
	
	@Test
	@DisplayName("additionalProperties false skips enrichment")
	fun falseReturnsNull() {
		val schema = Schema<Any>().apply { additionalProperties = false }
		assertThat(OpenApiMapValueSchemas.resolve(schema)).isNull()
		assertThat(schema.additionalProperties).isEqualTo(false)
	}
	
	@Test
	@DisplayName("true or unset upgrades to a Schema object")
	fun upgradesTrueOrUnset() {
		val unset = Schema<Any>()
		val created = OpenApiMapValueSchemas.resolve(unset)
		assertThat(created).isNotNull
		assertThat(unset.additionalProperties).isSameAs(created)
		val asTrue = Schema<Any>().apply { additionalProperties = true }
		val upgraded = OpenApiMapValueSchemas.resolve(asTrue)
		assertThat(upgraded).isNotNull
		assertThat(asTrue.additionalProperties).isSameAs(upgraded)
	}
}
