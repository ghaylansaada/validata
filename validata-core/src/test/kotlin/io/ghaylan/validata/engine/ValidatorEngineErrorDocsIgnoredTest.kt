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
package io.ghaylan.validata.engine

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.TwoFields
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Docs-only IR fields must never change validation outcomes.
 * 
 * @author Ghaylan Saada
 */
class ValidatorEngineErrorDocsIgnoredTest {
	
	@Test
	@DisplayName("PropertySpec.errorDocs do not change validateRequest errors")
	fun errorDocsDoNotAffectValidation() {
		val engine = EngineTestSupport.engine()
		val baseSchema = EngineTestSupport.twoFieldsSchema()
		val decoratedProps = baseSchema.properties.map { prop ->
			prop.copy(
				errorDocs = listOf(
					SchemaErrorDoc(
						code = "DOCS_ONLY_${prop.declaredName.uppercase()}",
						message = "ignored by engine",
						catalogFqcn = "test.DocsEnum",
					),
				),
			)
		}
		val decoratedBody = ObjectSchema(
			type = baseSchema.type,
			properties = decoratedProps,
			subtypes = baseSchema.subtypes,
		)
		val plainEndpoint = EngineTestSupport.bodyRequestSchema(body = baseSchema)
		val decoratedEndpoint = EngineTestSupport.bodyRequestSchema(body = decoratedBody)
		val payload = TwoFields(left = null, right = "ok")
		val plainErrors = engine.validateRequest(
			schema = plainEndpoint,
			body = payload,
			params = null,
			headers = null,
			pathVariables = null,
		)
		val decoratedErrors = engine.validateRequest(
			schema = decoratedEndpoint,
			body = payload,
			params = null,
			headers = null,
			pathVariables = null,
		)
		
		assertThat(decoratedErrors).isEqualTo(plainErrors)
		assertThat(plainErrors.map { it.path }).containsExactly("left")
		assertThat(decoratedProps).allMatch { it.errorDocs.isNotEmpty() }
	}
	
	@Test
	@DisplayName("copying PropertySpec with errorDocs preserves constraints used by the engine")
	fun propertySpecCopyRetainsConstraints() {
		val original = EngineTestSupport.twoFieldsSchema().properties.first()
		val withDocs = original.copy(
			errorDocs = listOf(SchemaErrorDoc(code = "X", message = "y")),
		)
		assertThat(withDocs.constraints).isEqualTo(original.constraints)
		assertThat(withDocs.errorDocs).hasSize(1)
		assertThat(withDocs).isInstanceOf(PropertySpec::class.java)
	}
}
