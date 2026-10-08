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
package io.ghaylan.validata.web

import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas
import io.ghaylan.validata.web.fixture.app.TestWebApplication
import io.ghaylan.validata.web.fixture.controller.MethodLevelController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Host regression: flat query/header/path schemas must stay identity-stable across requests.
 * 
 * @author Ghaylan Saada
 */
@SpringBootTest(classes = [TestWebApplication::class])
class FlatParamSchemaStabilityTest(
	@Autowired
	val engine: ValidatorEngine
) {
	
	@Test
	@DisplayName("query/header/path ObjectSchemas keep the same property+constraint instances across validations")
	fun flatSchemasAreNotRebuiltPerRequest() {
		val method = MethodLevelController::class.java.getDeclaredMethod(
			"getOrder",
			String::class.java,
			String::class.java,
		)
		val id = method.getUniqueIdentifier()
		val schema = GeneratedRequestSchemas.get(id)!!
		val pathProps = schema.pathVariables!!.properties
		val headerProps = schema.headers!!.properties
		val pathConstraints = pathProps.flatMap { it.constraints }
		val headerConstraints = headerProps.flatMap { it.constraints }
		
		repeat(1_000) {
			engine.validateRequest(
				id = id,
				body = null,
				params = null,
				headers = mapOf("X-Tenant" to "acme"),
				pathVariables = mapOf("orderId" to "ORD123"),
			)
		}
		
		assertThat(GeneratedRequestSchemas.get(id)!!.pathVariables!!.properties).isSameAs(pathProps)
		assertThat(GeneratedRequestSchemas.get(id)!!.headers!!.properties).isSameAs(headerProps)
		assertThat(GeneratedRequestSchemas.get(id)!!.pathVariables!!.properties.flatMap { it.constraints }).isEqualTo(pathConstraints)
		assertThat(GeneratedRequestSchemas.get(id)!!.headers!!.properties.flatMap { it.constraints }).isEqualTo(headerConstraints)
		assertThat(GeneratedRequestSchemas.get(id)!!.pathVariables!!.properties[0].constraints[0]).isSameAs(pathConstraints[0])
	}
}
