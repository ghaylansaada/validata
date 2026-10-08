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
package io.ghaylan.validata.schema

import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Proves docs IR fields on [PropertySpec] default empty and retain values.
 *
 * Does not assert validation-engine behavior — only IR construction contracts.
 * 
 * @author Ghaylan Saada
 */
class SchemaDocsIrRetentionTest {
	
	@Test
	@DisplayName("PropertySpec errorDocs defaults empty and retains provided docs")
	fun propertySpecErrorDocsDefaultsAndRetention() {
		val withoutDocs = PropertySpec(
			declaredName = "email",
			externalName = "email",
			shape = ScalarShape(ScalarKind.STRING),
			read = { null },
		)
		assertThat(withoutDocs.errorDocs).isEmpty()
		val docs = listOf(
			SchemaErrorDoc(
				code = "EMAIL_TAKEN",
				message = "already registered",
				catalogFqcn = "com.example.UserErrors",
			),
		)
		val withDocs = PropertySpec(
			declaredName = "email",
			externalName = "email",
			shape = ScalarShape(ScalarKind.STRING),
			read = { null },
			errorDocs = docs,
		)
		
		assertThat(withDocs.errorDocs).isEqualTo(docs)
	}
}
