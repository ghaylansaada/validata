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
package io.ghaylan.validata.constraint.validator.collection.contains

import io.ghaylan.validata.constraint.annotation.Contains
import io.ghaylan.validata.constraint.annotation.ContainsConstraint
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import io.ghaylan.validata.model.ConstraintErrorCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ContainsValidator")
class ContainsValidatorTest {

	private fun c(vararg values: String, mode: Contains.Mode = Contains.Mode.ANY) =
		ContainsConstraint(values.toSet(), mode, "", ValidatorTestSupport.defaultGroups)

	@Test
	@DisplayName("ANY — at least one element matches")
	fun anyMode() {
		val constraint = c("A", "B")
		assertValid(CollectionContainsValidator, listOf("A"), constraint)
		assertInvalid(
			CollectionContainsValidator,
			listOf("C"),
			constraint,
			ConstraintErrorCode.COLLECTION_ITEM_MISSING,
			constraint,
		)
	}

	@Test
	@DisplayName("ALL — every required literal appears")
	fun allMode() {
		val constraint = c("A", "B", mode = Contains.Mode.ALL)
		assertValid(CollectionContainsValidator, listOf("A", "B", "C"), constraint)
		assertInvalid(
			CollectionContainsValidator,
			listOf("A"),
			constraint,
			ConstraintErrorCode.COLLECTION_ITEM_MISSING,
			constraint,
		)
	}

	@Test
	@DisplayName("NONE — forbidden literals absent")
	fun noneMode() {
		val constraint = c("A", mode = Contains.Mode.NONE)
		assertValid(CollectionContainsValidator, listOf("B"), constraint)
		assertInvalid(
			CollectionContainsValidator,
			listOf("A"),
			constraint,
			ConstraintErrorCode.VALUE_NOT_ALLOWED,
			constraint,
		)
	}

	@Test
	@DisplayName("arrays use ArrayContainsValidator")
	fun arraySubject() {
		val constraint = c("1", "2")
		assertValid(ArrayContainsValidator, arrayOf("1"), constraint)
		assertInvalid(
			ArrayContainsValidator,
			arrayOf("9"),
			constraint,
			ConstraintErrorCode.COLLECTION_ITEM_MISSING,
			constraint,
		)
	}

	@Test
	@DisplayName("non-array Cloneable skips")
	fun nonArrayCloneableSkips() {
		assertValid(ArrayContainsValidator, object : Cloneable {}, c("x"))
	}
}
