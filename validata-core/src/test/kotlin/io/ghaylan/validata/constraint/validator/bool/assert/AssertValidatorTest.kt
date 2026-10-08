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
package io.ghaylan.validata.constraint.validator.bool.assert

import io.ghaylan.validata.constraint.annotation.AssertConstraint
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import io.ghaylan.validata.model.ConstraintErrorCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AssertValidator")
class AssertValidatorTest {

	private fun c(expected: Boolean) =
		AssertConstraint(expected, "", ValidatorTestSupport.defaultGroups)

	@Nested
	@DisplayName("null")
	inner class Null {
		@Test
		fun skipsNull() {
			assertSkipsNull(AssertValidator, c(true))
		}
	}

	@Nested
	@DisplayName("value = true")
	inner class MustBeTrue {
		@Test
		fun passesWhenTrue() {
			assertValid(AssertValidator, true, c(true))
		}

		@Test
		fun failsWhenFalse() {
			assertInvalid(AssertValidator, false, c(true), ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
	}

	@Nested
	@DisplayName("value = false")
	inner class MustBeFalse {
		@Test
		fun passesWhenFalse() {
			assertValid(AssertValidator, false, c(false))
		}

		@Test
		fun failsWhenTrue() {
			assertInvalid(AssertValidator, true, c(false), ConstraintErrorCode.VALUE_NOT_ALLOWED)
		}
	}
}
