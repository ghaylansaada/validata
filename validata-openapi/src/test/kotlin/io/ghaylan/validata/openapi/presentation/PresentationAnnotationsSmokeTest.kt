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
package io.ghaylan.validata.openapi.presentation

import io.ghaylan.validata.model.ConstraintErrorDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/**
 * Compile-time / reflection smoke coverage for presentation annotations.
 *
 * Guards Kotlin `@Repeatable` stacked `@ApiError` (would fail if repeatable wiring broke).
 * Does not exercise OpenAPI enrichment.
 * 
 * @author Ghaylan Saada
 */
class PresentationAnnotationsSmokeTest {
	
	private enum class SampleCatalog(override val message: String): ConstraintErrorDefinition { EMAIL_TAKEN("Email already registered"),
		EMAIL_INVALID("Bad format");
		
		override val code: String get() = name
	}
	
	private data class SampleBody(
		@property:ApiError(
			code = "EMAIL_TAKEN",
			catalog = SampleCatalog::class,
		)
		@property:ApiError(
			code = "EMAIL_INVALID",
			message = "Docs override",
			catalog = SampleCatalog::class,
		)
		val email: String?,
	)
	
	@Test
	@DisplayName("@ApiError retains code, message, and catalog on a property")
	fun apiErrorOnPropertyRetainsAttributes() {
		val property = SampleBody::class.memberProperties.single { it.name == "email" }
		val docs = property.findAnnotations<ApiError>()
		assertThat(docs).hasSize(2)
		assertThat(docs.map { it.code }).containsExactlyInAnyOrder("EMAIL_TAKEN", "EMAIL_INVALID")
		val taken = docs.single { it.code == "EMAIL_TAKEN" }
		assertThat(taken.message).isEmpty()
		assertThat(taken.catalog).isEqualTo(SampleCatalog::class)
		val invalid = docs.single { it.code == "EMAIL_INVALID" }
		assertThat(invalid.message).isEqualTo("Docs override")
	}
}
