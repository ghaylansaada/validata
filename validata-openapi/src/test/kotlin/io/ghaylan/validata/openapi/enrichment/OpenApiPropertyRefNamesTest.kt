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

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards `@PropertyRef` declared→wire rewrite for OpenAPI constraint args.
 * 
 * @author Ghaylan Saada
 */
class OpenApiPropertyRefNamesTest {
	
	@Test
	@DisplayName("null schema or blank name returns input unchanged")
	fun nullOrBlankPassthrough() {
		assertThat(OpenApiPropertyRefNames.toWire(null, "confirm")).isEqualTo("confirm")
		assertThat(OpenApiPropertyRefNames.toWire(owner(), "")).isEqualTo("")
		assertThat(OpenApiPropertyRefNames.toWire(owner(), "   ")).isEqualTo("   ")
	}
	
	@Test
	@DisplayName("declared name rewrites to externalName; unknown stays as-is")
	fun declaredToExternalOrUnknown() {
		val schema = owner()
		assertThat(OpenApiPropertyRefNames.toWire(schema, "confirmSecret")).isEqualTo("confirm_secret")
		assertThat(OpenApiPropertyRefNames.toWire(schema, "confirm_secret")).isEqualTo("confirm_secret")
		assertThat(OpenApiPropertyRefNames.toWire(schema, "missing")).isEqualTo("missing")
	}
	
	private fun owner(): ObjectSchema = ObjectSchema(
		type = Any::class.java,
		properties = listOf(
			PropertySpec(
				declaredName = "confirmSecret",
				externalName = "confirm_secret",
				shape = ScalarShape(ScalarKind.STRING),
				read = ValueReader { null },
			),
		),
	)
}
