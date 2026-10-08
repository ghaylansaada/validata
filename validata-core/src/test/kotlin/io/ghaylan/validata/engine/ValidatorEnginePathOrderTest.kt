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

import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.Batch
import io.ghaylan.validata.support.EngineTestSupport.NamedItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Deterministic natural ordering of multi-error responses from [ValidatorEngine].
 * 
 * @author Ghaylan Saada
 */
class ValidatorEnginePathOrderTest {
	
	@Test
	@DisplayName("indexed paths sort numerically (items[2] before items[10])")
	fun naturalIndexOrder() {
		val engine = EngineTestSupport.engine()
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val items = MutableList(11) { NamedItem(name = "ok") }
		items[2] = NamedItem(name = null)
		items[10] = NamedItem(name = null)
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = items),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactly("items[2].name", "items[10].name")
	}
}
