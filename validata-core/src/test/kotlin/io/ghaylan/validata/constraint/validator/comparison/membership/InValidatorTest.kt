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

import io.ghaylan.validata.constraint.annotation.InConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [InValidator].
 *
 * @author Ghaylan Saada
 */
@DisplayName("InValidator")
class InValidatorTest {

	private fun c(vararg values: String) =
		InConstraint(values.toSet(), "", ValidatorTestSupport.defaultGroups)

	@Nested
	@DisplayName("null / non-scalar")
	inner class SkipPaths {

		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(InValidator, c("OPEN"))
		}

		@Test
		@DisplayName("non-scalar value is skipped")
		fun skipsNonScalar() {
			assertValid(InValidator, listOf("OPEN"), c("OPEN"))
		}
	}

	@Nested
	@DisplayName("membership")
	inner class Membership {

		@Test
		@DisplayName("listed string passes")
		fun allowsListedValue() {
			assertValid(InValidator, "OPEN", c("OPEN", "CLOSED"))
		}

		@Test
		@DisplayName("unknown string fails with VALUE_NOT_ALLOWED and constraint attached")
		fun rejectsUnknownValue() {
			val constraint = c("OPEN", "CLOSED")
			assertInvalid(
				InValidator,
				"DRAFT",
				constraint,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
				constraint,
			)
		}

		@Test
		@DisplayName("number matches via toString")
		fun numberMembership() {
			assertValid(InValidator, 18, c("18", "21"))
			assertInvalid(InValidator, 17, c("18", "21"), ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}

		@Test
		@DisplayName("enum matches via name")
		fun enumMembership() {
			assertValid(InValidator, SampleStatus.OPEN, c("OPEN", "CLOSED"))
			assertInvalid(
				InValidator,
				SampleStatus.DRAFT,
				c("OPEN", "CLOSED"),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
	}

	private enum class SampleStatus { OPEN, CLOSED, DRAFT }
}
