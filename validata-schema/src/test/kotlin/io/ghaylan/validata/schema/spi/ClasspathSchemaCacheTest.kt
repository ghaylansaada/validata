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
package io.ghaylan.validata.schema.spi

import io.ghaylan.validata.schema.request.spi.GeneratedRequestSchemas
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Direct unit tests for [ClasspathSchemaCache] (shared by [GeneratedSchemas] /
 * [GeneratedRequestSchemas]).
 * 
 * @author Ghaylan Saada
 */
class ClasspathSchemaCacheTest {
	
	@Test
	@DisplayName("all() loads once and returns the same snapshot until reset")
	fun loadsOnceUntilReset() {
		val loads = AtomicInteger(0)
		val cache = ClasspathSchemaCache {
			loads.incrementAndGet()
			mapOf("a" to 1)
		}
		val first = cache.all()
		val second = cache.all()
		assertThat(second).isSameAs(first)
		assertThat(loads.get()).isEqualTo(1)
		assertThat(cache.get("a")).isEqualTo(1)
		assertThat(loads.get()).isEqualTo(1)
		
		cache.reset()
		val third = cache.all()
		assertThat(third).isNotSameAs(first)
		assertThat(loads.get()).isEqualTo(2)
		assertThat(third).containsEntry("a", 1)
	}
	
	@Test
	@DisplayName("get() triggers load on first use and returns null for unknown keys")
	fun getTriggersLoad() {
		val cache = ClasspathSchemaCache { mapOf("known" to "v") }
		
		assertThat(cache.get("known")).isEqualTo("v")
		assertThat(cache.get("missing")).isNull()
	}
	
	@Test
	@DisplayName("load failures are not cached — next all() retries")
	fun loadFailureNotCached() {
		val attempts = AtomicInteger(0)
		val cache = ClasspathSchemaCache {
			if (attempts.incrementAndGet() == 1) error("boom")
			mapOf("ok" to 1)
		}
		
		assertThatThrownBy { cache.all() }.hasMessageContaining("boom")
		assertThat(cache.all()).containsEntry("ok", 1)
		assertThat(attempts.get()).isEqualTo(2)
	}
}
