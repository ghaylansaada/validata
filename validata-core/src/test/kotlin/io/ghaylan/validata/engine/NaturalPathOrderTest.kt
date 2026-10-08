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

import io.ghaylan.validata.engine.support.NaturalPathOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Direct coverage for [NaturalPathOrder] (used when sorting multi-error responses).
 * 
 * @author Ghaylan Saada
 */
class NaturalPathOrderTest {
	
	@Test
	@DisplayName("numeric segments compare as integers")
	fun numericSegments() {
		assertThat(NaturalPathOrder.compare("items[2].name", "items[10].name")).isLessThan(0)
		assertThat(NaturalPathOrder.compare("items[10].name", "items[2].name")).isGreaterThan(0)
	}
	
	@Test
	@DisplayName("equal paths compare equal")
	fun equalPaths() {
		assertThat(NaturalPathOrder.compare("a.b", "a.b")).isEqualTo(0)
	}
	
	@Test
	@DisplayName("non-digit characters fall back to character order")
	fun characterOrder() {
		assertThat(NaturalPathOrder.compare("alpha", "beta")).isLessThan(0)
	}
	
	@Test
	@DisplayName("leading zeros in numeric segments do not change integer order")
	fun leadingZeros() {
		assertThat(NaturalPathOrder.compare("items[007].x", "items[8].x")).isLessThan(0)
		assertThat(NaturalPathOrder.compare("items[08].x", "items[8].x")).isEqualTo(0)
	}
	
	@Test
	@DisplayName("empty strings and prefixes sort stably")
	fun emptyAndPrefix() {
		assertThat(NaturalPathOrder.compare("", "")).isEqualTo(0)
		assertThat(NaturalPathOrder.compare("a", "ab")).isLessThan(0)
		assertThat(NaturalPathOrder.compare("ab", "a")).isGreaterThan(0)
	}
}
