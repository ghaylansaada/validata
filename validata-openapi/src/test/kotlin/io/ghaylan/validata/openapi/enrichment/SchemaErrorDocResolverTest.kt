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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards catalog message / code fill for baked [SchemaErrorDoc] entries.
 * 
 * @author Ghaylan Saada
 */
class SchemaErrorDocResolverTest {
	
	private enum class ProbeErrors(override val message: String): ConstraintErrorDefinition { EMAIL_TAKEN("Email already registered");
		
		override val code: String get() = name
	}
	
	@BeforeEach
	fun reset() {
		SchemaErrorDocResolver.resetForTests()
	}
	
	@Test
	@DisplayName("blank message is filled from ConstraintErrorDefinition catalog")
	fun blankMessageFilledFromCatalog() {
		val doc = SchemaErrorDoc(
			code = "EMAIL_TAKEN",
			message = "",
			catalogFqcn = ProbeErrors::class.java.name,
		)
		assertThat(SchemaErrorDocResolver.effectiveMessage(doc)).isEqualTo("Email already registered")
		assertThat(SchemaErrorDocResolver.effectiveCode(doc)).isEqualTo("EMAIL_TAKEN")
	}
	
	@Test
	@DisplayName("non-blank message overrides catalog")
	fun nonBlankMessageOverridesCatalog() {
		val doc = SchemaErrorDoc(
			code = "EMAIL_TAKEN",
			message = "Docs override",
			catalogFqcn = ProbeErrors::class.java.name,
		)
		assertThat(SchemaErrorDocResolver.effectiveMessage(doc)).isEqualTo("Docs override")
	}
	
	@Test
	@DisplayName("unloadable catalog FQCN falls back without throwing")
	fun unloadableCatalogFallsBack() {
		val doc = SchemaErrorDoc(
			code = "ANY",
			message = "",
			catalogFqcn = "com.example.DoesNotExistCatalog",
		)
		assertThat(SchemaErrorDocResolver.effectiveMessage(doc)).isEmpty()
		assertThat(SchemaErrorDocResolver.effectiveCode(doc)).isEqualTo("ANY")
	}
	
	@Test
	@DisplayName("unknown enum constant falls back to baked code/message")
	fun unknownEnumConstantFallsBack() {
		val doc = SchemaErrorDoc(
			code = "NOT_A_REAL_CONSTANT",
			message = "",
			catalogFqcn = ProbeErrors::class.java.name,
		)
		assertThat(SchemaErrorDocResolver.effectiveMessage(doc)).isEmpty()
		assertThat(SchemaErrorDocResolver.effectiveCode(doc)).isEqualTo("NOT_A_REAL_CONSTANT")
	}
	
	@Test
	@DisplayName("non-enum catalog FQCN falls back without throwing")
	fun nonEnumCatalogFallsBack() {
		val doc = SchemaErrorDoc(
			code = "EMAIL_TAKEN",
			message = "",
			catalogFqcn = ArrayList::class.java.name,
		)
		assertThat(SchemaErrorDocResolver.effectiveMessage(doc)).isEmpty()
		assertThat(SchemaErrorDocResolver.effectiveCode(doc)).isEqualTo("EMAIL_TAKEN")
	}
}
