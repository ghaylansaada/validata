/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.discovery.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Documents single-flight semantics used by [ConstraintDiscoveryCache] (ConcurrentHashMap.compute).
 * 
 * @author Ghaylan Saada
 */
class ConstraintDiscoveryCacheConcurrencyTest {
	
	@Test
	@DisplayName("ConcurrentHashMap.compute runs the miss loader once under contention")
	fun computeIsSingleFlightUnderContention() {
		val map = ConcurrentHashMap<String, String>()
		val loads = AtomicInteger(0)
		val start = CountDownLatch(1)
		val pool = Executors.newFixedThreadPool(8)
		try {
			val futures = (1..8).map {
				pool.submit {
					start.await()
					map.compute("equalTo") { _, existing ->
						existing
							?: run {
								loads.incrementAndGet()
								Thread.sleep(20)
								"hosts"
							}
					}
				}
			}
			start.countDown()
			futures.forEach { it.get() }
		}
		finally {
			pool.shutdownNow()
		}
		assertThat(loads.get()).isEqualTo(1)
		assertThat(map["equalTo"]).isEqualTo("hosts")
	}
	
	@Test
	@DisplayName("Library FQCN prefix matches validata packages only")
	fun libraryFqcnPrefixMatchesValidataOnly() {
		assertThat(ConstraintDiscoveryCache.isLibraryFqcn("io.ghaylan.validata.constraint.annotation.Size")).isTrue()
		assertThat(ConstraintDiscoveryCache.isLibraryFqcn("com.example.MyConstraint")).isFalse()
		assertThat(ConstraintDiscoveryCache.isLibraryFqcn("io.ghaylan.other.X")).isFalse()
	}
}
