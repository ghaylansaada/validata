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
package io.ghaylan.validata.bootstrap

import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas
import io.ghaylan.validata.web.fixture.app.TestWebApplication
import io.ghaylan.validata.web.fixture.controller.MethodLevelController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

/**
 * Web Boot slice: [EndpointSchemaIndex] maps live `@Validate` handlers to generated schemas.
 * 
 * @author Ghaylan Saada
 */
@SpringBootTest(classes = [TestWebApplication::class])
class EndpointSchemaIndexTest(
	@Autowired
	val applicationContext: ApplicationContext,
	@Autowired
	val registry: ValidationRegistry,
) {
	
	@Test
	@DisplayName("registers an EndpointSchema for every @Validate handler discovered in the web context")
	fun registersHandlersFromWebContext() {
		val schemas = EndpointSchemaIndex.resolveStaticSchemas(applicationContext)
		val searchId = MethodLevelController::class.java.getDeclaredMethod("search", String::class.java, Int::class.javaObjectType)
			.getUniqueIdentifier()
		
		assertThat(schemas).containsKey(searchId)
		assertThat(schemas.getValue(searchId).queryParams).isNotNull
		assertThat(GeneratedRequestSchemas.all()).containsKey(searchId)
	}
	
	@Test
	@DisplayName("initializer onApplicationEvent populates registry static schemas for web handlers")
	fun initializerPopulatesRegistryStaticSchemas() {
		val searchId = MethodLevelController::class.java.getDeclaredMethod("search", String::class.java, Int::class.javaObjectType)
			.getUniqueIdentifier()
		
		assertThat(registry.getSchemaByRequest(searchId)).isNotNull
		assertThat(registry.getSchemaByRequest(searchId)!!.queryParams).isNotNull
	}
}
