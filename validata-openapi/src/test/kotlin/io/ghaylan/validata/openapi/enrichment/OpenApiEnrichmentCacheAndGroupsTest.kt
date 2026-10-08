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

import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Cache identity/generation semantics and [ConstraintGroupFilter] boundaries.
 * 
 * @author Ghaylan Saada
 */
class OpenApiEnrichmentCacheAndGroupsTest {
	
	@BeforeEach
	fun reset() {
		OpenApiEnrichmentCache.resetForTests()
		EndpointErrorCodeCollector.resetForTests()
	}
	
	@Test
	@DisplayName("markSchema returns true once per identity then false")
	fun markSchemaIsIdempotentPerInstance() {
		val schema = Schema<Any>()
		assertThat(OpenApiEnrichmentCache.markSchema(schema)).isTrue()
		assertThat(OpenApiEnrichmentCache.markSchema(schema)).isFalse()
		assertThat(OpenApiEnrichmentCache.markSchema(Schema<Any>())).isTrue()
	}
	
	@Test
	@DisplayName("markOperation returns true once per identity then false")
	fun markOperationIsIdempotentPerInstance() {
		val operation = Operation()
		assertThat(OpenApiEnrichmentCache.markOperation(operation)).isTrue()
		assertThat(OpenApiEnrichmentCache.markOperation(operation)).isFalse()
	}
	
	@Test
	@DisplayName("clear allows the same schema identity to be marked again")
	fun clearResetsMarks() {
		val schema = Schema<Any>()
		assertThat(OpenApiEnrichmentCache.markSchema(schema)).isTrue()
		OpenApiEnrichmentCache.clear()
		assertThat(OpenApiEnrichmentCache.markSchema(schema)).isTrue()
	}
	
	@Test
	@DisplayName("isActive is true when constraint groups or active groups are empty")
	fun isActiveWhenEitherGroupSetEmpty() {
		val withGroups = SizeConstraint(1, 2, "", setOf(OnCreate::class))
		val noGroups = SizeConstraint(1, 2, "", emptySet())
		
		assertThat(ConstraintGroupFilter.isActive(noGroups, setOf(OnDefault::class))).isTrue()
		assertThat(ConstraintGroupFilter.isActive(withGroups, emptySet())).isTrue()
		assertThat(ConstraintGroupFilter.isActive(withGroups, setOf(OnDefault::class))).isFalse()
		assertThat(ConstraintGroupFilter.isActive(withGroups, setOf(OnCreate::class))).isTrue()
	}
}
