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
package io.ghaylan.validata.processor.compat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks [ProcessorRoundCache] begin/end lifecycle so KS identity maps do not outlive a round.
 * 
 * @author Ghaylan Saada

 */
class ProcessorRoundCacheTest {

	@AfterEach
	fun tearDown() {
		// Drain any nested depth left by a failed assertion.
		ProcessorRoundCache.forceClear()
	}

	@Test
	@DisplayName("end() nulls internal maps armed by begin()")
	fun endClearsArmedMaps() {
		ProcessorRoundCache.begin()
		assertThat(typeSupersMap()).isNotNull()
		assertThat(annotationFqcnMap()).isNotNull()
		assertThat(depth()).isEqualTo(1)

		ProcessorRoundCache.end()
		assertThat(typeSupersMap()).isNull()
		assertThat(annotationFqcnMap()).isNull()
		assertThat(depth()).isEqualTo(0)
	}

	@Test
	@DisplayName("nested begin/end shares maps until the outermost end")
	fun nestedBeginSharesMaps() {
		ProcessorRoundCache.begin()
		val firstSupers = typeSupersMap()
		ProcessorRoundCache.begin()
		assertThat(depth()).isEqualTo(2)
		assertThat(typeSupersMap()).isSameAs(firstSupers)

		ProcessorRoundCache.end()
		assertThat(depth()).isEqualTo(1)
		assertThat(typeSupersMap()).isSameAs(firstSupers)

		ProcessorRoundCache.end()
		assertThat(depth()).isEqualTo(0)
		assertThat(typeSupersMap()).isNull()
	}

	@Test
	@DisplayName("end() is idempotent when already inactive")
	fun endIsIdempotent() {
		ProcessorRoundCache.end()
		ProcessorRoundCache.end()
		assertThat(typeSupersMap()).isNull()
		assertThat(depth()).isEqualTo(0)
	}

	@Test
	@DisplayName("forceClear() drops nested depth and maps immediately")
	fun forceClearDropsNestedDepth() {
		ProcessorRoundCache.begin()
		ProcessorRoundCache.begin()
		assertThat(depth()).isEqualTo(2)
		assertThat(typeSupersMap()).isNotNull()

		ProcessorRoundCache.forceClear()
		assertThat(depth()).isEqualTo(0)
		assertThat(typeSupersMap()).isNull()
		assertThat(annotationFqcnMap()).isNull()
	}

	private fun typeSupersMap(): Any? =
		stateField("typeSupers")

	private fun annotationFqcnMap(): Any? =
		stateField("annotationFqcn")

	private fun depth(): Int =
		stateField("depth") as Int

	private fun threadState(): Any {
		val localField = ProcessorRoundCache::class.java.getDeclaredField("local")
		localField.isAccessible = true
		@Suppress("UNCHECKED_CAST")
		val local = localField.get(ProcessorRoundCache) as ThreadLocal<*>
		return checkNotNull(local.get()) { "ProcessorRoundCache ThreadLocal State missing" }
	}

	private fun stateField(name: String): Any? {
		val state = threadState()
		val field = state.javaClass.getDeclaredField(name)
		field.isAccessible = true
		return field.get(state)
	}
}
