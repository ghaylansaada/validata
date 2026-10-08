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

import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas
import io.ghaylan.validata.web.fixture.app.TestWebApplication
import io.ghaylan.validata.web.fixture.controller.MethodLevelController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Handshake: runtime [getUniqueIdentifier] matches KSP [GeneratedRequestSchemas] keys for host handlers.
 *
 * Threat: id drift between reflection and KSP would leave annotated handlers without a schema.
 * 
 * @author Ghaylan Saada
 */
@SpringBootTest(classes = [TestWebApplication::class])
class GeneratedRequestSchemaAgreementTest {
	
	@Test
	@DisplayName("runtime Method.getUniqueIdentifier() has a matching GeneratedRequestSchemas entry")
	fun runtimeIdMatchesGenerated() {
		val method = MethodLevelController::class.java.getDeclaredMethod(
			"search",
			String::class.java,
			Int::class.javaObjectType,
		)
		val id = method.getUniqueIdentifier()
		assertThat(GeneratedRequestSchemas.all()).containsKey(id)
		assertThat(GeneratedRequestSchemas.get(id)!!.queryParams).isNotNull
	}
	
	@Test
	@DisplayName("path+header handler also has a generated EndpointSchema")
	fun pathHeaderHandlerPresent() {
		val method = MethodLevelController::class.java.getDeclaredMethod(
			"getOrder",
			String::class.java,
			String::class.java,
		)
		val id = method.getUniqueIdentifier()
		val schema = GeneratedRequestSchemas.get(id)
		assertThat(schema).isNotNull
		assertThat(schema!!.pathVariables).isNotNull
		assertThat(schema.headers).isNotNull
	}
}
