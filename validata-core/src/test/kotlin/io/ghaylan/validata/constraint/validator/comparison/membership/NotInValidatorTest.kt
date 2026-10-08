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
package io.ghaylan.validata.constraint.validator.comparison.membership

import io.ghaylan.validata.constraint.annotation.NotInConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [NotInValidator].
 *
 * @author Ghaylan Saada
 */
@DisplayName("NotInValidator")
class NotInValidatorTest {

	private fun c(vararg values: String) =
		NotInConstraint(values.toSet(), "", ValidatorTestSupport.defaultGroups)

	@Nested
	@DisplayName("null / non-scalar")
	inner class SkipPaths {

		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(NotInValidator, c("banned"))
		}

		@Test
		@DisplayName("non-scalar value is skipped")
		fun skipsNonScalar() {
			assertValid(NotInValidator, listOf("banned"), c("banned"))
		}
	}

	@Nested
	@DisplayName("membership")
	inner class Membership {

		@Test
		@DisplayName("unlisted string passes")
		fun allowsUnlistedValue() {
			assertValid(NotInValidator, "alice", c("root", "admin"))
		}

		@Test
		@DisplayName("listed string fails with VALUE_NOT_ALLOWED and constraint attached")
		fun rejectsListedValue() {
			val constraint = c("banned", "root")
			assertInvalid(
				NotInValidator,
				"banned",
				constraint,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
				constraint,
			)
		}

		@Test
		@DisplayName("number deny-list via toString")
		fun numberDenyList() {
			assertValid(NotInValidator, 17, c("18", "21"))
			assertInvalid(NotInValidator, 18, c("18", "21"), ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
	}
}
