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
package io.ghaylan.validata.schema.runtime

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Checklist wording and factory wiring for [SchemaNotFoundException].
 * 
 * @author Ghaylan Saada
 */
class SchemaNotFoundExceptionTest {
	
	private class MissingDto
	
	@Test
	@DisplayName("messageFor contains the type name and checklist bullets")
	fun messageForContainsTypeAndChecklist() {
		val message = SchemaNotFoundException.messageFor(MissingDto::class.java)
		assertThat(message).contains(MissingDto::class.java.name)
		assertThat(message).contains("First-time setup checklist:")
		assertThat(message).contains("@Validatable")
		assertThat(message).contains("validata-processor")
		assertThat(message).contains("  1.")
		assertThat(message).contains("  2.")
		assertThat(message).contains("  3.")
	}
	
	@Test
	@DisplayName("forMissingSchema sets type and message")
	fun forMissingSchemaSetsTypeAndMessage() {
		val ex = SchemaNotFoundException.forMissingSchema(MissingDto::class.java)
		assertThat(ex.type).isEqualTo(MissingDto::class.java)
		assertThat(ex.message).isEqualTo(SchemaNotFoundException.messageFor(MissingDto::class.java))
	}
}
