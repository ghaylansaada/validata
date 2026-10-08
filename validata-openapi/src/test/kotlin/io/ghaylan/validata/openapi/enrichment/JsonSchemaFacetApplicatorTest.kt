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

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Facet → swagger Schema mapping (descriptions never written; merge-aware overlaps).
 *
 * Guards `@Size` on a list being published as `minLength` (wrong keyword) via collection facets.
 * 
 * @author Ghaylan Saada
 */
class JsonSchemaFacetApplicatorTest {
	
	@Nested
	@DisplayName("mapping")
	inner class Mapping {
		
		@Test
		@DisplayName("empty hints leave schema unchanged and return false")
		fun emptyHintsAreNoOp() {
			val schema = Schema<Any>().apply { description = "keep" }
			assertThat(JsonSchemaFacetApplicator.apply(ConstraintDocHints.EMPTY, schema)).isFalse()
			assertThat(schema.description).isEqualTo("keep")
			assertThat(schema.extensions).isNull()
		}
		
		@Test
		@DisplayName("Required facet marks nullable=false and returns true (no vendor extension)")
		fun requiredFacetReturnsTrue() {
			val schema = Schema<Any>()
			val hints = ConstraintDocHints(
				facets = setOf(JsonSchemaFacet.Required, JsonSchemaFacet.NotNullable),
			)
			assertThat(JsonSchemaFacetApplicator.apply(hints, schema)).isTrue()
			assertThat(schema.nullable).isFalse()
			assertThat(schema.extensions).isNull()
		}
		
		@Test
		@DisplayName("numeric and string facets map onto schema fields without touching description")
		fun mapsNumericAndStringFacets() {
			val schema = Schema<Any>().apply { description = "from @Schema" }
			val hints = ConstraintDocHints(
				facets = setOf(
					JsonSchemaFacet.MinLength(1),
					JsonSchemaFacet.MaxLength(10),
					JsonSchemaFacet.Pattern("^[a-z]+$"),
					JsonSchemaFacet.Minimum(BigDecimal.ONE, exclusive = true),
					JsonSchemaFacet.Maximum(BigDecimal.TEN, exclusive = false),
					JsonSchemaFacet.MultipleOf(BigDecimal("2")),
					JsonSchemaFacet.Format("email"),
					JsonSchemaFacet.EnumValues(listOf("a", "b")),
				),
			)
			assertThat(JsonSchemaFacetApplicator.apply(hints, schema)).isFalse()
			assertThat(schema.minLength).isEqualTo(1)
			assertThat(schema.maxLength).isEqualTo(10)
			assertThat(schema.pattern).isEqualTo("^[a-z]+$")
			assertThat(schema.minimum).isEqualByComparingTo(BigDecimal.ONE)
			assertThat(schema.exclusiveMinimum).isTrue()
			assertThat(schema.maximum).isEqualByComparingTo(BigDecimal.TEN)
			assertThat(schema.multipleOf).isEqualByComparingTo(BigDecimal("2"))
			assertThat(schema.format).isEqualTo("email")
			assertThat(schema.enum).containsExactly("a", "b")
			assertThat(schema.description).isEqualTo("from @Schema")
		}
		
		@Test
		@DisplayName("applying facets never overrides an existing schema description")
		fun neverOverridesDescription() {
			val schema = Schema<Any>().apply { description = "base" }
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.MinLength(2))),
				schema,
			)
			assertThat(schema.description).isEqualTo("base")
			assertThat(schema.minLength).isEqualTo(2)
		}
		
		@Test
		@DisplayName("collection and map size facets map to items/properties bounds")
		fun mapsCollectionAndMapFacets() {
			val schema = Schema<Any>()
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(
					facets = setOf(
						JsonSchemaFacet.MinItems(1),
						JsonSchemaFacet.MaxItems(5),
						JsonSchemaFacet.MinProperties(2),
						JsonSchemaFacet.MaxProperties(4),
					),
				),
				schema,
			)
			assertThat(schema.minItems).isEqualTo(1)
			assertThat(schema.maxItems).isEqualTo(5)
			assertThat(schema.minProperties).isEqualTo(2)
			assertThat(schema.maxProperties).isEqualTo(4)
		}
		
		@Test
		@DisplayName("overlapping length bounds keep the strictest intersection")
		fun strictestLengthBounds() {
			val schema = Schema<Any>()
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.MinLength(2), JsonSchemaFacet.MaxLength(40))),
				schema,
			)
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.MinLength(5), JsonSchemaFacet.MaxLength(20))),
				schema,
			)
			assertThat(schema.minLength).isEqualTo(5)
			assertThat(schema.maxLength).isEqualTo(20)
		}
		
		@Test
		@DisplayName("conflicting format keeps the first value")
		fun formatKeepsFirst() {
			val schema = Schema<Any>()
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.Format("email"))),
				schema,
			)
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.Format("uri"))),
				schema,
			)
			assertThat(schema.format).isEqualTo("email")
		}
		
		@Test
		@DisplayName("overlapping enums keep the intersection")
		fun enumIntersection() {
			val schema = Schema<Any>()
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.EnumValues(listOf("A", "B", "C")))),
				schema,
			)
			JsonSchemaFacetApplicator.apply(
				ConstraintDocHints(facets = setOf(JsonSchemaFacet.EnumValues(listOf("B", "C", "D")))),
				schema,
			)
			assertThat(schema.enum).containsExactly("B", "C")
		}
	}
}
