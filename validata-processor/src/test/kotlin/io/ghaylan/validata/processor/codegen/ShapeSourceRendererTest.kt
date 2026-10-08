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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [ShapeSourceRenderer] emission branches (no KSP round-trip).
 * 
 * @author Ghaylan Saada
 */
class ShapeSourceRendererTest {
	
	@Test
	@DisplayName("scalar emits ScalarKind enum constant by name")
	fun scalar() {
		val rendered = ShapeSourceRenderer.renderShape(
			ScalarShapeModel(ScalarKind.STRING),
			schemasByQualifiedName = emptyMap(),
		)
		assertThat(rendered).isEqualTo("ScalarShape(ScalarKind.STRING, emptyList())")
	}
	
	@Test
	@DisplayName("iterable nests element shape")
	fun iterable() {
		val rendered = ShapeSourceRenderer.renderShape(
			IterableShapeModel(element = ScalarShapeModel(ScalarKind.INTEGRAL)),
			schemasByQualifiedName = emptyMap(),
		)
		assertThat(rendered).isEqualTo(
			"IterableShape(ScalarShape(ScalarKind.INTEGRAL, emptyList()), emptyList())",
		)
	}
	
	@Test
	@DisplayName("map nests key and value shapes")
	fun map() {
		val rendered = ShapeSourceRenderer.renderShape(
			MapShapeModel(
				key = ScalarShapeModel(ScalarKind.STRING),
				value = ScalarShapeModel(ScalarKind.DECIMAL),
			),
			schemasByQualifiedName = emptyMap(),
		)
		assertThat(rendered).isEqualTo(
			"MapShape(ScalarShape(ScalarKind.STRING, emptyList()), " + "ScalarShape(ScalarKind.DECIMAL, emptyList()), emptyList())",
		)
	}
	
	@Test
	@DisplayName("schema path: in-unit peer uses PeerSchema.build()")
	fun objectRefPeerLookup() {
		val address = SchemaModel(
			packageName = "com.acme",
			simpleName = "Address",
			qualifiedName = "com.acme.Address",
			properties = emptyList(),
			isPolymorphicRoot = false,
			subtypeQualifiedNames = emptyList(),
		)
		val rendered = ShapeSourceRenderer.renderShape(
			ObjectRefShapeModel("com.acme.Address"),
			schemasByQualifiedName = mapOf(address.qualifiedName to address),
		)
		assertThat(rendered).isEqualTo(
			"ObjectRefShape(lazy { AddressSchema.build() }, emptyList())",
		)
	}
	
	@Test
	@DisplayName("schema path: missing peer uses GeneratedSchemaLookup")
	fun objectRefMissingPeer() {
		val rendered = ShapeSourceRenderer.renderShape(
			ObjectRefShapeModel("com.other.Address"),
			schemasByQualifiedName = emptyMap(),
		)
		assertThat(rendered).isEqualTo(
			"ObjectRefShape(lazy { GeneratedSchemaLookup.requireGeneratedSchema(com.other.Address::class.java) }, emptyList())",
		)
	}
	
	@Test
	@DisplayName("endpoint path: always GeneratedSchemaLookup even when peer exists")
	fun objectRefAlwaysLookup() {
		val rendered = ShapeSourceRenderer.renderShape(
			ObjectRefShapeModel("com.acme.Address"),
			schemasByQualifiedName = null,
		)
		assertThat(rendered).isEqualTo(
			"ObjectRefShape(lazy { GeneratedSchemaLookup.requireGeneratedSchema(com.acme.Address::class.java) }, emptyList())",
		)
	}
}
