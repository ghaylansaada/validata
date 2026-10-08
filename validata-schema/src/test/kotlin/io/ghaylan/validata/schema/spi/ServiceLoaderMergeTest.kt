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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ServiceLoaderMerge.merge] — the conflict policy shared by [GeneratedSchemas]
 * and [GeneratedRequestSchemas].
 *
 * ## Bugs this suite would catch
 * - Silent overwrite when two modules claim the same key (wrong schema at runtime).
 * - Mutable merged maps that callers could corrupt after cache load.
 * - Empty contribution lists accidentally returning a mutable map or null.
 * 
 * @author Ghaylan Saada
 */
class ServiceLoaderMergeTest {
	
	@Nested
	@DisplayName("Given an empty contribution list")
	inner class EmptyContributions {
		
		@Test
		@DisplayName("merge returns an empty unmodifiable map")
		fun emptyMergeIsUnmodifiable() {
			val merged = ServiceLoaderMerge.merge<String, Int>(emptyList()) { key, first, second ->
				"Duplicate '$key' from $first and $second"
			}
			
			assertThat(merged).isEmpty()
			assertThatThrownBy { (merged as MutableMap<String, Int>)["x"] = 1 }.isInstanceOf(UnsupportedOperationException::class.java)
		}
	}
	
	@Nested
	@DisplayName("Given a single owner")
	inner class SingleOwner {
		
		@Test
		@DisplayName("merge keeps every entry from that owner")
		fun singleOwnerPreservesEntries() {
			val merged = ServiceLoaderMerge.merge(
				listOf("owner.A" to mapOf("a" to 1, "b" to 2)),
			) { key, first, second ->
				"Duplicate '$key' from $first and $second"
			}
			
			assertThat(merged).containsExactlyEntriesOf(mapOf("a" to 1, "b" to 2))
		}
	}
	
	@Nested
	@DisplayName("Given two owners with distinct keys")
	inner class DistinctKeys {
		
		@Test
		@DisplayName("merge unions both maps in contribution order")
		fun distinctKeysUnion() {
			val merged = ServiceLoaderMerge.merge(
				listOf(
					"owner.A" to mapOf("a" to 1),
					"owner.B" to mapOf("b" to 2),
				),
			) { key, first, second ->
				"Duplicate '$key' from $first and $second"
			}
			
			assertThat(merged).containsExactlyEntriesOf(mapOf("a" to 1, "b" to 2))
		}
	}
	
	@Nested
	@DisplayName("Given two owners claiming the same key")
	inner class DuplicateKeys {
		
		@Test
		@DisplayName("merge fails with the duplicateMessage naming both owners")
		fun duplicateKeyFailsLoudly() {
			assertThatThrownBy {
				ServiceLoaderMerge.merge(
					listOf(
						"owner.A" to mapOf("shared" to 1),
						"owner.B" to mapOf("shared" to 2),
					),
				) { key, first, second ->
					"Duplicate '$key' from $first and $second"
				}
			}.hasMessage("Duplicate 'shared' from owner.A and owner.B")
		}
	}
	
	@Nested
	@DisplayName("Given mapCapacity sizing")
	inner class MapCapacity {
		
		@Test
		@DisplayName("mapCapacity uses expectedSize+1 below 3, then load-factor sizing")
		fun mapCapacityFormula() {
			assertThat(ServiceLoaderMerge.mapCapacity(0)).isEqualTo(1)
			assertThat(ServiceLoaderMerge.mapCapacity(1)).isEqualTo(2)
			assertThat(ServiceLoaderMerge.mapCapacity(2)).isEqualTo(3)
			assertThat(ServiceLoaderMerge.mapCapacity(3)).isEqualTo(5)
			assertThat(ServiceLoaderMerge.mapCapacity(12)).isEqualTo(17)
		}
	}
}
