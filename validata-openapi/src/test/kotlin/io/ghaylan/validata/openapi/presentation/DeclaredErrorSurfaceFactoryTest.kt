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

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Merge contracts for [DeclaredErrorSurfaceFactory] (IR collector ∪ baked schema docs).
 * 
 * @author Ghaylan Saada
 */
class DeclaredErrorSurfaceFactoryTest {
	
	@Test
	@DisplayName("merges property detail docs with structural collector codes")
	fun mergesDetailDocsAndCollectorCodes() {
		val endpoint = EndpointSchema(
			id = "sample.Ctrl#create()",
			groups = emptySet(),
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "email",
						externalName = "email",
						shape = ScalarShape(ScalarKind.STRING),
						read = { null },
						errorDocs = listOf(
							SchemaErrorDoc(code = "EMAIL_TAKEN", message = "taken"),
						),
					),
				),
			),
		)
		val surface = DeclaredErrorSurfaceFactory.from(endpoint)
		
		assertThat(surface.detailCodes).contains("EMAIL_TAKEN", "VALUE_TYPE_MISMATCH", "STRUCTURE_DEPTH_EXCEEDED")
		assertThat(surface.detailErrorDocs.map { it.code }).contains("EMAIL_TAKEN")
		val emailDoc = surface.detailErrorDocs.single { it.code == "EMAIL_TAKEN" }
		assertThat(emailDoc.path).isEqualTo("email")
		assertThat(emailDoc.location).isEqualTo(EndpointArgumentKind.BODY)
	}
	
	@Test
	@DisplayName("nested object and list paths use wire names with [] for elements")
	fun nestedObjectAndListPaths() {
		class Nested
		class Root
		
		val nestedSchema = ObjectSchema(
			type = Nested::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "sku",
					externalName = "sku_code",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
					errorDocs = listOf(
						SchemaErrorDoc(code = "SKU_TAKEN", message = "taken"),
					),
				),
			),
		)
		val endpoint = EndpointSchema(
			id = "sample.Ctrl#nested()",
			groups = emptySet(),
			requestBody = ObjectSchema(
				type = Root::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "items",
						externalName = "items",
						shape = IterableShape(ObjectRefShape(lazyOf(nestedSchema))),
						read = { null },
					),
					PropertySpec(
						declaredName = "address",
						externalName = "address",
						shape = ObjectRefShape(lazyOf(nestedSchema)),
						read = { null },
						errorDocs = listOf(
							SchemaErrorDoc(code = "ADDR_INVALID", message = "bad"),
						),
					),
				),
			),
		)
		val surface = DeclaredErrorSurfaceFactory.from(endpoint)
		val paths = surface.detailErrorDocs.associate { it.code to it.path }
		assertThat(paths["SKU_TAKEN"]).isIn("items[].sku_code", "address.sku_code")
		assertThat(surface.detailErrorDocs.filter { it.code == "SKU_TAKEN" }
			.map { it.path }).containsExactlyInAnyOrder("items[].sku_code", "address.sku_code")
		assertThat(paths["ADDR_INVALID"]).isEqualTo("address")
	}
	
	@Test
	@DisplayName("map value nested docs use * path segments")
	fun mapValuePathsUseStar() {
		class Theme
		class Root
		
		val themeSchema = ObjectSchema(
			type = Theme::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "theme",
					externalName = "theme",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
					errorDocs = listOf(
						SchemaErrorDoc(code = "THEME_UNKNOWN", message = "unknown"),
					),
				),
			),
		)
		val endpoint = EndpointSchema(
			id = "sample.Ctrl#prefs()",
			groups = emptySet(),
			requestBody = ObjectSchema(
				type = Root::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "prefs",
						externalName = "prefs",
						shape = MapShape(
							key = ScalarShape(ScalarKind.STRING),
							value = ObjectRefShape(lazyOf(themeSchema)),
						),
						read = { null },
					),
				),
			),
		)
		val surface = DeclaredErrorSurfaceFactory.from(endpoint)
		assertThat(surface.detailErrorDocs.single { it.code == "THEME_UNKNOWN" }.path).isEqualTo("prefs.*.theme")
	}
}
