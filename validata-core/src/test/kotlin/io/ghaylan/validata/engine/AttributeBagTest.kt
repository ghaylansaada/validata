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

import io.ghaylan.validata.runtime.AttributeBag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Lazy allocation behaviour of [AttributeBag].
 * 
 * @author Ghaylan Saada
 */
class AttributeBagTest {
	
	@Test
	@DisplayName("starts empty and unallocated")
	fun startsEmpty() {
		val bag = AttributeBag()
		assertThat(bag.isEmpty()).isTrue()
		assertThat(bag.isAllocated).isFalse()
	}
	
	@Test
	@DisplayName("getOrCompute allocates once and reuses the value")
	fun computesOnce() {
		val bag = AttributeBag()
		var calls = 0
		val first = bag.getOrCompute("k") {
			calls++
			"v"
		}
		val second = bag.getOrCompute("k") {
			calls++
			"other"
		}
		assertThat(first).isEqualTo("v")
		assertThat(second).isEqualTo("v")
		assertThat(calls).isEqualTo(1)
		assertThat(bag.isAllocated).isTrue()
		assertThat(bag.isEmpty()).isFalse()
	}
	
	@Test
	@DisplayName("probe lookup with storeKey allocates the stored key only on miss")
	fun probeStoreKey() {
		val bag = AttributeBag()
		
		data class PairKey(
			val a: Int,
			val b: Int
		)
		
		val probe = arrayOf(PairKey(1, 2)) // mutable holder for demo; Distinct uses a mutable class
		var stores = 0
		var computes = 0
		
		fun load(): String = bag.getOrCompute(
			lookupKey = probe[0],
			storeKey = {
				stores++
				probe[0].copy()
			},
		) {
			computes++
			"plan"
		}
		
		assertThat(load()).isEqualTo("plan")
		assertThat(load()).isEqualTo("plan")
		assertThat(computes).isEqualTo(1)
		assertThat(stores).isEqualTo(1)
	}
}
