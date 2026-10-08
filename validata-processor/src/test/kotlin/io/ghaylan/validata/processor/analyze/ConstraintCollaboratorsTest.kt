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
package io.ghaylan.validata.processor.analyze

import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.ConstraintModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure-slice unit coverage for [ConstraintModelBuilder] collaborators (T9.1).
 * 
 * @author Ghaylan Saada
 */
class ConstraintCollaboratorsTest {
	
	@Nested
	@DisplayName("ConstraintValidatorSelector.pickBest")
	inner class Selector {
		
		@Test
		@DisplayName("empty scored list yields null")
		fun empty() {
			assertThat(ConstraintValidatorSelector.pickBest(emptyList())).isNull()
		}
		
		@Test
		@DisplayName("lowest rank wins; FQCN breaks ties")
		fun rankThenFqcn() {
			assertThat(
				ConstraintValidatorSelector.pickBest(
					listOf(2 to "b.Validator", 0 to "z.Exact", 0 to "a.Exact"),
				),
			).isEqualTo("a.Exact")
		}
	}
	
	@Nested
	@DisplayName("MetadataCollectionLiteral")
	inner class CollectionLiteral {
		
		@Test
		@DisplayName("set vs list and empty variants")
		fun variants() {
			assertThat(MetadataCollectionLiteral.emptySet()).isEqualTo("emptySet()")
			assertThat(MetadataCollectionLiteral.emptyList()).isEqualTo("emptyList()")
			assertThat(MetadataCollectionLiteral.setOf("1, 2")).isEqualTo("setOf(1, 2)")
			assertThat(MetadataCollectionLiteral.listOf("\"a\"")).isEqualTo("listOf(\"a\")")
			assertThat(MetadataCollectionLiteral.render(useSet = true, empty = true, elements = "")).isEqualTo("emptySet()")
			assertThat(MetadataCollectionLiteral.render(useSet = false, empty = false, elements = "\"a\"")).isEqualTo("listOf(\"a\")")
		}
	}
	
	@Nested
	@DisplayName("CompositionPayloadExprs")
	inner class PayloadExprs {
		
		@Test
		@DisplayName("message escapes and defaults")
		fun message() {
			assertThat(CompositionPayloadExprs.message(null)).isEqualTo("\"\"")
			assertThat(CompositionPayloadExprs.message("hi \"there\"")).isEqualTo("\"hi \\\"there\\\"\"")
		}
		
		@Test
		@DisplayName("groups default to OnDefault")
		fun groups() {
			assertThat(CompositionPayloadExprs.groups(null)).isEqualTo("setOf(${ProcessorFqns.ON_DEFAULT}::class)")
			assertThat(CompositionPayloadExprs.groups(listOf("a.G1", "b.G2"))).isEqualTo("setOf(a.G1::class, b.G2::class)")
		}
	}
	
	@Nested
	@DisplayName("ConstraintPresenceOrdering")
	inner class PresenceOrdering {
		
		@Test
		@DisplayName("Required sorts before Email and orders are renumbered")
		fun requiredFirst() {
			val out = mutableListOf(
				stub("Email", order = 0),
				stub("Required", order = 1),
				stub("Size", order = 2),
			)
			ConstraintPresenceOrdering.sortInPlace(out)
			assertThat(out.map { it.annotationSimpleName }).containsExactly("Required", "Email", "Size")
			assertThat(out.map { it.order }).containsExactly(0, 1, 2)
		}
		
		private fun stub(
			name: String,
			order: Int
		) = ConstraintModel(
			metadataConstructorCall = "",
			validatorExpression = "",
			order = order,
			annotationSimpleName = name,
		)
	}
	
	@Nested
	@DisplayName("ConstraintRoundSession.cachedNullable")
	inner class CachedNullable {
		
		@Test
		@DisplayName("caches null hits so compute runs once")
		fun cachesNull() {
			var calls = 0
			val cache = HashMap<String, String?>()
			val first = ConstraintRoundSession.cachedNullable(cache, "k") {
				calls++
				null
			}
			val second = ConstraintRoundSession.cachedNullable(cache, "k") {
				calls++
				"should-not-run"
			}
			assertThat(first).isNull()
			assertThat(second).isNull()
			assertThat(calls).isEqualTo(1)
			assertThat(cache).containsEntry("k", null)
		}
	}
}
