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
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Endpoint / transport IR construction contracts for Spring host assemblers.
 *
 * Does not assert HTTP binding or interceptor behavior — that belongs in `validata`.
 * 
 * @author Ghaylan Saada
 */
class EndpointSchemaSmokeTest {
	
	@Test
	@DisplayName("EndpointSchema constructor defaults: oneErrorPerParam true, failFast false")
	fun constructorDefaults() {
		val schema = EndpointSchema(
			id = "pkg.Ctrl#defaults()",
			groups = emptySet())
		
		assertThat(schema.oneErrorPerParam).isTrue()
		assertThat(schema.failFast).isFalse()
		assertThat(schema.pathVariables).isNull()
		assertThat(schema.headers).isNull()
		assertThat(schema.queryParams).isNull()
		assertThat(schema.requestBody).isNull()
		assertThat(schema.argumentLayout).isEmpty()
		assertThat(schema.groups).isEmpty()
	}
	
	@Test
	@DisplayName("non-null transport sections, groups, and fail-fast flags are retained")
	fun sectionsAndFlagsRetained() {
		val pathSchema = ObjectSchema(
			type = Any::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "id",
					externalName = "id",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
				),
			),
		)
		val bodySchema = ObjectSchema(type = Any::class.java, properties = emptyList())
		val schema = EndpointSchema(
			id = "pkg.Ctrl#full()",
			pathVariables = pathSchema,
			headers = ObjectSchema(type = Any::class.java, properties = emptyList()),
			queryParams = ObjectSchema(type = Any::class.java, properties = emptyList()),
			requestBody = bodySchema,
			oneErrorPerParam = false,
			failFast = true,
			groups = setOf(OnCreate::class),
		)
		
		assertThat(schema.pathVariables).isSameAs(pathSchema)
		assertThat(schema.requestBody).isSameAs(bodySchema)
		assertThat(schema.headers).isNotNull
		assertThat(schema.queryParams).isNotNull
		assertThat(schema.oneErrorPerParam).isFalse()
		assertThat(schema.failFast).isTrue()
		assertThat(schema.groups).containsExactly(OnCreate::class)
	}
	
	@Test
	@DisplayName("EndpointArgumentSlot carries kind and optional transport name")
	fun argumentSlots() {
		val body = EndpointArgumentSlot(kind = EndpointArgumentKind.BODY)
		val query = EndpointArgumentSlot(kind = EndpointArgumentKind.QUERY, name = "q")
		
		assertThat(body.name).isEmpty()
		assertThat(query.name).isEqualTo("q")
	}
	
	@Test
	@DisplayName("argumentLayout retains PATH, HEADER, OTHER, BODY slots in order")
	fun argumentLayoutKinds() {
		val layout = listOf(
			EndpointArgumentSlot(EndpointArgumentKind.PATH, "id"),
			EndpointArgumentSlot(EndpointArgumentKind.HEADER, "X-Token"),
			EndpointArgumentSlot(EndpointArgumentKind.OTHER),
			EndpointArgumentSlot(EndpointArgumentKind.BODY),
			EndpointArgumentSlot(EndpointArgumentKind.QUERY, "q"),
		)
		val schema = EndpointSchema(
			id = "pkg.Ctrl#layout()",
			groups = emptySet(),
			argumentLayout = layout,
		)
		
		assertThat(schema.argumentLayout).containsExactlyElementsOf(layout)
		assertThat(schema.argumentLayout.map { it.kind }).containsExactly(
			EndpointArgumentKind.PATH,
			EndpointArgumentKind.HEADER,
			EndpointArgumentKind.OTHER,
			EndpointArgumentKind.BODY,
			EndpointArgumentKind.QUERY,
		)
		assertThat(schema.argumentLayout[2].name).isEmpty()
	}
	
	/**
	 * Marker group used only to prove [EndpointSchema.groups] retention.
	 */
	private annotation class OnCreate
}
