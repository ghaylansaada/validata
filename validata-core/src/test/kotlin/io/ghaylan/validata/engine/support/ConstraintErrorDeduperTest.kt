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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ConstraintErrorDeduper] — path+code uniqueness and natural-path ordering.
 * 
 * @author Ghaylan Saada
 */
class ConstraintErrorDeduperTest {
	
	private fun err(
		path: String?,
		code: ConstraintErrorCode = ConstraintErrorCode.VALUE_MISSING,
	): ConstraintError<*> = ConstraintError(path = path, code = code)
	
	@Nested
	@DisplayName("fast path")
	inner class FastPath {
		
		@Test
		@DisplayName("empty list is returned by identity")
		fun emptyIdentity() {
			val empty = emptyList<ConstraintError<*>>()
			assertThat(ConstraintErrorDeduper.deduplicate(empty)).isSameAs(empty)
		}
		
		@Test
		@DisplayName("single-element list is returned by identity")
		fun singleIdentity() {
			val single = listOf(err("email"))
			assertThat(ConstraintErrorDeduper.deduplicate(single)).isSameAs(single)
		}
	}
	
	@Nested
	@DisplayName("deduplication")
	inner class Deduplication {
		
		@Test
		@DisplayName("duplicate path+code collapses to one error")
		fun duplicatePathAndCode() {
			val a = err("email", ConstraintErrorCode.TEXT_BLANK)
			val b = err("email", ConstraintErrorCode.TEXT_BLANK)
			val result = ConstraintErrorDeduper.deduplicate(listOf(a, b))
			assertThat(result).hasSize(1)
			assertThat(result.single().path).isEqualTo("email")
			assertThat(result.single().code).isEqualTo(ConstraintErrorCode.TEXT_BLANK)
		}
		
		@Test
		@DisplayName("same path with different codes keeps both, ordered by code name")
		fun samePathDifferentCodes() {
			val late = err("email", ConstraintErrorCode.VALUE_FORMAT_INVALID)
			val early = err("email", ConstraintErrorCode.TEXT_BLANK)
			val result = ConstraintErrorDeduper.deduplicate(listOf(late, early))
			assertThat(result).hasSize(2)
			assertThat(result.map { (it.code as Enum<*>).name }).containsExactly("TEXT_BLANK", "VALUE_FORMAT_INVALID")
		}
	}
	
	@Nested
	@DisplayName("ordering")
	inner class Ordering {
		
		@Test
		@DisplayName("distinct paths sort with natural path order")
		fun naturalPathOrder() {
			val result = ConstraintErrorDeduper.deduplicate(
				listOf(
					err("users[10].name"),
					err("users[2].name"),
					err("email"),
				),
			)
			assertThat(result.map { it.path }).containsExactly(
				"email",
				"users[2].name",
				"users[10].name",
			)
		}
	}
}
