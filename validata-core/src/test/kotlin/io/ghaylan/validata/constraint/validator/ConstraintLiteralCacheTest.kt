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
package io.ghaylan.validata.constraint.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/** Unit tests for [ConstraintLiteralCache] memoization semantics.
 * 
 * @author Ghaylan Saada
 */
class ConstraintLiteralCacheTest {
	
	@AfterEach
	fun tearDown() {
		ConstraintLiteralCache.clearForTests()
	}
	
	@Nested
	@DisplayName("getOrParse")
	inner class GetOrParse {
		
		@Test
		@DisplayName("returns cached value on second call with same key")
		fun cacheHit() {
			val key = CacheKey("parse-hit")
			var parseCount = 0
			val first = ConstraintLiteralCache.getOrParse(key) {
				parseCount++
				"parsed"
			}
			val second = ConstraintLiteralCache.getOrParse(key) {
				parseCount++
				"should-not-run"
			}
			
			assertThat(first).isEqualTo("parsed")
			assertThat(second).isEqualTo("parsed")
			assertThat(parseCount).isEqualTo(1)
		}
		
		@Test
		@DisplayName("does not cache null parse results")
		fun nullNotCached() {
			val key = CacheKey("parse-null")
			val attempts = AtomicInteger(0)
			
			repeat(3) {
				val result: String? = ConstraintLiteralCache.getOrParse(key) {
					attempts.incrementAndGet()
					null
				}
				assertThat(result).isNull()
			}
			
			assertThat(attempts.get()).isEqualTo(3)
		}
		
		@Test
		@DisplayName("uses distinct entries per key")
		fun cacheMissPerKey() {
			var keyOneParses = 0
			var keyTwoParses = 0
			val first = ConstraintLiteralCache.getOrParse(CacheKey("one")) {
				keyOneParses++
				1
			}
			val second = ConstraintLiteralCache.getOrParse(CacheKey("two")) {
				keyTwoParses++
				2
			}
			
			assertThat(first).isEqualTo(1)
			assertThat(second).isEqualTo(2)
			assertThat(keyOneParses).isEqualTo(1)
			assertThat(keyTwoParses).isEqualTo(1)
		}
	}
	
	@Nested
	@DisplayName("getOrCompute")
	inner class GetOrCompute {
		
		@Test
		@DisplayName("returns cached value on second call with same key")
		fun cacheHit() {
			val key = CacheKey("compute-hit")
			var computeCount = 0
			val first = ConstraintLiteralCache.getOrCompute(key) {
				computeCount++
				Policy("ready")
			}
			val second = ConstraintLiteralCache.getOrCompute(key) {
				computeCount++
				Policy("should-not-run")
			}
			
			assertThat(first).isEqualTo(Policy("ready"))
			assertThat(second).isEqualTo(Policy("ready"))
			assertThat(computeCount).isEqualTo(1)
		}
		
		@Test
		@DisplayName("recomputes after clearForTests")
		fun clearForTests() {
			val key = CacheKey("clear")
			var computeCount = 0
			
			ConstraintLiteralCache.getOrCompute(key) {
				computeCount++
				"first"
			}
			ConstraintLiteralCache.clearForTests()
			val afterClear = ConstraintLiteralCache.getOrCompute(key) {
				computeCount++
				"second"
			}
			
			assertThat(afterClear).isEqualTo("second")
			assertThat(computeCount).isEqualTo(2)
		}
	}
	
	private data class CacheKey(val id: String)
	
	private data class Policy(val state: String)
}
