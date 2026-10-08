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
package io.ghaylan.validata.constraint.validator.size

import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CharSequenceSizeValidator].
 * 
 * @author Ghaylan Saada
 */
class CharSequenceSizeValidatorTest {
	
	private fun constraint(
		min: Int = 1,
		max: Int = 2
	): SizeConstraint = SizeConstraint(min, max, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("Null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value skips validation")
		fun nullValueSkipsValidation() {
			assertSkipsNull(CharSequenceSizeValidator, constraint())
		}
	}
	
	@Nested
	@DisplayName("Within bounds")
	inner class WithinBounds {
		
		@Test
		@DisplayName("length at minimum passes")
		fun lengthAtMinimumPasses() {
			assertValid(CharSequenceSizeValidator, "a", constraint(min = 1, max = 3))
		}
		
		@Test
		@DisplayName("length at maximum passes")
		fun lengthAtMaximumPasses() {
			assertValid(CharSequenceSizeValidator, "abc", constraint(min = 1, max = 3))
		}
		
		@Test
		@DisplayName("length between bounds passes")
		fun lengthBetweenBoundsPasses() {
			assertValid(CharSequenceSizeValidator, "ab", constraint(min = 1, max = 3))
		}
	}
	
	@Nested
	@DisplayName("Out of bounds")
	inner class OutOfBounds {
		
		@Test
		@DisplayName("empty string fails with TEXT_TOO_SHORT")
		fun emptyStringFailsTooShort() {
			val c = constraint()
			assertInvalid(
				CharSequenceSizeValidator,
				"",
				c,
				ConstraintErrorCode.TEXT_TOO_SHORT,
				c,
			)
		}

		@Test
		@DisplayName("string shorter than minimum fails with TEXT_TOO_SHORT")
		fun belowMinimumFailsTooShort() {
			val c = constraint(min = 2, max = 4)
			assertInvalid(
				CharSequenceSizeValidator,
				"a",
				c,
				ConstraintErrorCode.TEXT_TOO_SHORT,
				c,
			)
		}

		@Test
		@DisplayName("string longer than maximum fails with TEXT_TOO_LONG")
		fun aboveMaximumFailsTooLong() {
			val c = constraint(min = 1, max = 2)
			assertInvalid(
				CharSequenceSizeValidator,
				"abc",
				c,
				ConstraintErrorCode.TEXT_TOO_LONG,
				c,
			)
		}
	}
}
