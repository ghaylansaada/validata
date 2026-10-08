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

import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.TinyBody
import io.ghaylan.validata.support.EngineTestSupport.TwoFields
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Whole-request and standalone fail-fast behaviour for [ValidatorEngine].
 * 
 * @author Ghaylan Saada
 */
class ValidatorEngineFailFastTest {
	
	private val engine = EngineTestSupport.engine()
	
	@Test
	@DisplayName("failFast=false collects errors from multiple body fields")
	fun collectMultipleFields() {
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.twoFieldsSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = TwoFields(left = null, right = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactlyInAnyOrder("left", "right")
	}
	
	@Test
	@DisplayName("failFast=true stops after the first body field violation")
	fun failFastStopsAfterFirstField() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.twoFieldsSchema(),
			failFast = true,
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = TwoFields(left = null, right = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).hasSize(1)
		assertThat(errors.single().path).isEqualTo("left")
	}
	
	@Test
	@DisplayName("failFast skips later HTTP sections after the body fails")
	fun failFastSkipsLaterSections() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			query = EngineTestSupport.flatMapSchema("q"),
			failFast = true,
			id = "fail-fast-sections",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = TinyBody(name = null),
			params = mapOf("q" to null),
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).hasSize(1)
		assertThat(errors.single().path).isEqualTo("name")
	}
	
	@Test
	@DisplayName("oneErrorPerParam=true keeps one error per path when collecting")
	fun oneErrorPerParam() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.twoFieldsSchema(),
			oneErrorPerParam = true,
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = TwoFields(left = null, right = "ok"),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).hasSize(1)
		assertThat(errors.single().path).isEqualTo("left")
	}
	
	@Test
	@DisplayName("valid body with failing query still reports query when failFast=false")
	fun queryValidatedAfterValidBody() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			query = EngineTestSupport.flatMapSchema("q"),
			failFast = false,
			id = "query-after-body",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = TinyBody(name = "ok"),
			params = mapOf("q" to null),
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactly("q")
	}
	
	@Test
	@DisplayName("empty active groups still run ungrouped constraints via default schema groups")
	fun defaultGroupsOnRequestSchema() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.twoFieldsSchema(groups = setOf(OnDefault::class)),
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = TwoFields(left = "a", right = "b"),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).isEmpty()
	}
}
