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
package io.ghaylan.validata.schema.docs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Retention contracts for [SchemaErrorDoc] fields used by OpenAPI enrichment.
 * 
 * @author Ghaylan Saada
 */
class SchemaErrorDocTest {
	
	@Test
	@DisplayName("defaults: empty message and null catalogFqcn")
	fun defaults() {
		val doc = SchemaErrorDoc(code = "EMAIL_TAKEN")
		
		assertThat(doc.code).isEqualTo("EMAIL_TAKEN")
		assertThat(doc.message).isEmpty()
		assertThat(doc.catalogFqcn).isNull()
	}
	
	@Test
	@DisplayName("retains message and catalogFqcn when provided")
	fun retainsPopulatedFields() {
		val doc = SchemaErrorDoc(
			code = "USER_NOT_FOUND",
			message = "No user for id",
			catalogFqcn = "com.example.UserErrors",
		)
		
		assertThat(doc.message).isEqualTo("No user for id")
		assertThat(doc.catalogFqcn).isEqualTo("com.example.UserErrors")
	}
}
