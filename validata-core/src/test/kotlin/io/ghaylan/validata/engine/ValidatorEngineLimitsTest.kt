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

import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.Batch
import io.ghaylan.validata.support.EngineTestSupport.NamedItem
import io.ghaylan.validata.support.EngineTestSupport.NestedNode
import io.ghaylan.validata.support.EngineTestSupport.Settings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [ValidationLimits] interaction with [ValidatorEngine] — fail closed on depth/width.
 * 
 * @author Ghaylan Saada

 */
class ValidatorEngineLimitsTest {

	@Test
	@DisplayName("collection within the element ceiling is walked normally")
	fun collectionWithinCeiling() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxElementsPerContainer = 5))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = List(5) { NamedItem(name = if (it == 3) null else "ok") }),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).contains("items[3].name")
		assertThat(errors.map { it.code }).doesNotContain(ConstraintErrorCode.COLLECTION_TOO_LARGE)
	}

	@Test
	@DisplayName("collection past the element ceiling is rejected without per-element errors")
	fun collectionPastCeiling() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxElementsPerContainer = 5))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = List(6) { NamedItem(name = null) }),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.code }).contains(ConstraintErrorCode.COLLECTION_TOO_LARGE)
		assertThat(errors.map { it.path }).noneMatch { it.orEmpty().startsWith("items[") }
	}

	@Test
	@DisplayName("collection rejection message reports max and actual size; constraint is null")
	fun collectionRejectionMetadata() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxElementsPerContainer = 5))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = List(9) { NamedItem(name = "ok") }),
			params = null,
			headers = null,
			pathVariables = null,
		)
		val error = errors.single { it.code == ConstraintErrorCode.COLLECTION_TOO_LARGE }
		assertThat(error.metadata).isNull()
		assertThat(error.message).contains("9")
			.contains("5")
	}

	@Test
	@DisplayName("map past the element ceiling is rejected")
	fun mapPastCeiling() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxElementsPerContainer = 5))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.settingsSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Settings(entries = (1..6).associate { "key$it" to "v" }),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.code }).contains(ConstraintErrorCode.COLLECTION_TOO_LARGE)
		assertThat(errors.single { it.code == ConstraintErrorCode.COLLECTION_TOO_LARGE }.path)
			.isEqualTo("entries")
	}

	@Test
	@DisplayName("nesting deeper than maxDepth emits STRUCTURE_DEPTH_EXCEEDED")
	fun depthPastCeiling() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxDepth = 3))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.nestedNodeSchema())
		var tree = NestedNode(name = "leaf", child = null)
		repeat(10) { tree = NestedNode(name = "n", child = tree) }

		val errors = engine.validateRequest(
			schema = schema,
			body = tree,
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.code }).contains(ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED)
	}

	@Test
	@DisplayName("maxErrors stops collecting once the budget is spent")
	fun errorBudget() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxErrors = 3))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = List(20) { NamedItem(name = null) }),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).isNotEmpty
		assertThat(errors).hasSize(3)
	}

	@Test
	@DisplayName("nesting at exactly maxDepth is accepted; one level deeper fails")
	fun depthExactCeiling() {
		val engine = EngineTestSupport.engine(ValidationLimits(maxDepth = 3))
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.nestedNodeSchema())

		// depth: root=0, child chain of length 3 → deepest name at depth 3 (allowed)
		val atCeiling = NestedNode(
			name = "n0",
			child = NestedNode(
				name = "n1",
				child = NestedNode(
					name = "n2",
					child = NestedNode(name = "n3", child = null),
				),
			),
		)
		val ok = engine.validateRequest(schema, atCeiling, null, null, null)
		assertThat(ok.map { it.code }).doesNotContain(ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED)

		val over = NestedNode(name = "n0", child = atCeiling)
		val errors = engine.validateRequest(schema, over, null, null, null)
		assertThat(errors.map { it.code }).contains(ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED)
	}
}
