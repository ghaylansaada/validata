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

import io.ghaylan.validata.runtime.PathSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Characterization of [PathSegment] rope materialization (error-path strings only).
 * 
 * @author Ghaylan Saada
 */
class PathSegmentTest {
	
	@Test
	@DisplayName("Root materializes to empty string")
	fun rootIsEmpty() {
		assertThat(PathSegment.Root.toPathString()).isEmpty()
	}
	
	@Test
	@DisplayName("dotted names and indices preserve legacy syntax")
	fun nestedPath() {
		val root = PathSegment.Root
		val user = PathSegment.Name(root, "user")
		val address = PathSegment.Name(user, "address")
		val idx = PathSegment.Index(address, 0)
		val city = PathSegment.Name(idx, "city")
		assertThat(city.toPathString()).isEqualTo("user.address[0].city")
	}
	
	@Test
	@DisplayName("map key and value segments match legacy labels")
	fun mapSegments() {
		val meta = PathSegment.Name(PathSegment.Root, "meta")
		assertThat(PathSegment.MapKeys(meta, "k")
			.toPathString()).isEqualTo("meta.keys[k]")
		assertThat(PathSegment.MapValue(meta, "k")
			.toPathString()).isEqualTo("meta[k]")
	}
	
	@Test
	@DisplayName("beforeLastIndex returns the path before the nearest index")
	fun beforeLastIndex() {
		val city = PathSegment.Name(
			PathSegment.Index(PathSegment.Name(PathSegment.Root, "items"), 2),
			"name",
		)
		assertThat(city.beforeLastIndex()
			.toPathString()).isEqualTo("items")
	}
	
	@Test
	@DisplayName("beforeLastIndex on a path without indices returns the original segment")
	fun beforeLastIndexWithoutIndexReturnsOriginal() {
		val path = PathSegment.Name(PathSegment.Name(PathSegment.Root, "user"), "email")
		assertThat(path.beforeLastIndex()).isSameAs(path)
	}
	
	@Test
	@DisplayName("Root beforeLastIndex returns Root")
	fun beforeLastIndexOnRoot() {
		assertThat(PathSegment.Root.beforeLastIndex()).isSameAs(PathSegment.Root)
	}
	
	@Test
	@DisplayName("nested map value under index materializes with bracket syntax")
	fun mapValueUnderIndex() {
		val path = PathSegment.MapValue(
			PathSegment.Index(PathSegment.Name(PathSegment.Root, "rows"), 1),
			"id",
		)
		assertThat(path.toPathString()).isEqualTo("rows[1][id]")
	}
}
