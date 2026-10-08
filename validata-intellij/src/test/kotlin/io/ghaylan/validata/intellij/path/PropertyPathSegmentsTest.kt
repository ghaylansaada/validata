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

package io.ghaylan.validata.intellij.path

import io.ghaylan.validata.schema.PropertyPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks [PropertyPathSegments] to [PropertyPath] split rules plus IDE soft-segment ranges.
 * 
 * @author Ghaylan Saada
 */
class PropertyPathSegmentsTest {
	
	@Test
	@DisplayName("split delegates to PropertyPath.split")
	fun splitTrimsAndDropsBlankSegments() {
		assertThat(PropertyPathSegments.split("address.city")).containsExactly("address", "city")
		assertThat(PropertyPathSegments.split("  password  ")).containsExactly("password")
		assertThat(PropertyPathSegments.split("")).isEmpty()
		assertThat(PropertyPathSegments.split(" address . city ")).containsExactly("address", "city")
		assertThat(PropertyPathSegments.split("...")).isEmpty()
		assertThat(PropertyPathSegments.split("address.city")).isEqualTo(PropertyPath.split("address.city"))
	}
	
	@Test
	@DisplayName("MAX_REFERENCE_PATH_DEPTH matches schema PropertyPath cap")
	fun maxDepthConstantMatchesSchema() {
		assertThat(PropertyPathSegments.MAX_REFERENCE_PATH_DEPTH).isEqualTo(PropertyPath.MAX_REFERENCE_PATH_DEPTH)
		assertThat(PropertyPathSegments.MAX_REFERENCE_PATH_DEPTH).isEqualTo(6)
	}
	
	@Test
	@DisplayName("segmentsWithRanges covers nested path ranges")
	fun segmentsWithRangesCoverNestedPath() {
		val segs = PropertyPathSegments.segmentsWithRanges("address.city")
		assertThat(segs).hasSize(2)
		assertThat(segs[0].name).isEqualTo("address")
		assertThat(segs[0].rangeInValue.startOffset).isEqualTo(0)
		assertThat(segs[0].rangeInValue.endOffset).isEqualTo(7)
		assertThat(segs[0].soft).isFalse()
		assertThat(segs[1].name).isEqualTo("city")
		assertThat(segs[1].rangeInValue.startOffset).isEqualTo(8)
		assertThat(segs[1].rangeInValue.endOffset).isEqualTo(12)
	}
	
	@Test
	@DisplayName("trailing dot appends a soft empty segment for completion")
	fun trailingDotAddsSoftEmptySegment() {
		val segs = PropertyPathSegments.segmentsWithRanges("address.")
		assertThat(segs).hasSize(2)
		assertThat(segs[0].name).isEqualTo("address")
		assertThat(segs[1].name).isEmpty()
		assertThat(segs[1].soft).isTrue()
		assertThat(segs[1].rangeInValue.startOffset).isEqualTo(8)
	}
	
	@Test
	@DisplayName("empty string yields no segments")
	fun emptyStringYieldsNoSegments() {
		assertThat(PropertyPathSegments.segmentsWithRanges("")).isEmpty()
	}
	
	@Test
	@DisplayName("depth cap omits the seventh segment")
	fun depthCapOmitsSeventhSegment() {
		val path = (1..7).joinToString(".") { "s$it" }
		val segs = PropertyPathSegments.segmentsWithRanges(path)
		assertThat(segs).hasSize(PropertyPathSegments.MAX_REFERENCE_PATH_DEPTH)
		assertThat(segs.last().name).isEqualTo("s6")
	}
	
	@Test
	@DisplayName("single segment spans the full value range")
	fun singleSegmentHasFullRange() {
		val segs = PropertyPathSegments.segmentsWithRanges("password")
		assertThat(segs).hasSize(1)
		assertThat(segs[0].name).isEqualTo("password")
		assertThat(segs[0].rangeInValue.startOffset).isEqualTo(0)
		assertThat(segs[0].rangeInValue.endOffset).isEqualTo(8)
	}
}
