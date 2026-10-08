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

import io.ghaylan.validata.schema.runtime.GeneratedSchemaLookup
import io.ghaylan.validata.schema.runtime.SchemaNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Characterization for [GeneratedSchemaLookup] error messaging (Phase 6 decoupling).
 *
 * Guards: missing generated schema must fail loud with an actionable checklist
 * (not a vague ClassNotFound-style message).
 * 
 * @author Ghaylan Saada
 */
class GeneratedSchemaLookupTest {
	
	@Test
	@DisplayName("missing schema throws actionable SchemaNotFoundException")
	fun missingSchemaThrowsActionableSchemaNotFoundException() {
		val type = NeverValidatedDto::class.java
		val thrown = assertThrows<SchemaNotFoundException> {
			GeneratedSchemaLookup.requireGeneratedSchema(type)
		}
		assertThat(thrown.type).isSameAs(type)
		assertThat(thrown.message).contains(type.name)
			.contains("validata-processor")
			.contains("First-time setup checklist")
			.contains("@Validatable")
			.contains("sibling modules is not enough")
	}
	
	private class NeverValidatedDto
}
