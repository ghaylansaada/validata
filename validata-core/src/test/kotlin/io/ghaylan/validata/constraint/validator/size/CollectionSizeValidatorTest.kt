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
 * Unit tests for [CollectionSizeValidator].
 * 
 * @author Ghaylan Saada
 */
class CollectionSizeValidatorTest {
	
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
			assertSkipsNull(CollectionSizeValidator, constraint())
		}
	}
	
	@Nested
	@DisplayName("Within bounds")
	inner class WithinBounds {
		
		@Test
		@DisplayName("size at minimum passes")
		fun sizeAtMinimumPasses() {
			assertValid(CollectionSizeValidator, listOf(1), constraint(min = 1, max = 3))
		}
		
		@Test
		@DisplayName("size at maximum passes")
		fun sizeAtMaximumPasses() {
			assertValid(CollectionSizeValidator, listOf(1, 2, 3), constraint(min = 1, max = 3))
		}
		
		@Test
		@DisplayName("size between bounds passes")
		fun sizeBetweenBoundsPasses() {
			assertValid(CollectionSizeValidator, listOf(1, 2), constraint(min = 1, max = 3))
		}
	}
	
	@Nested
	@DisplayName("Out of bounds")
	inner class OutOfBounds {
		
		@Test
		@DisplayName("empty collection fails with COLLECTION_TOO_SMALL")
		fun emptyCollectionFailsTooSmall() {
			val c = constraint()
			assertInvalid(
				CollectionSizeValidator,
				emptyList<Any>(),
				c,
				ConstraintErrorCode.COLLECTION_TOO_SMALL,
				c,
			)
		}

		@Test
		@DisplayName("collection smaller than minimum fails with COLLECTION_TOO_SMALL")
		fun belowMinimumFailsTooSmall() {
			val c = constraint(min = 2, max = 4)
			assertInvalid(
				CollectionSizeValidator,
				listOf(1),
				c,
				ConstraintErrorCode.COLLECTION_TOO_SMALL,
				c,
			)
		}

		@Test
		@DisplayName("collection larger than maximum fails with COLLECTION_TOO_LARGE")
		fun aboveMaximumFailsTooLarge() {
			val c = constraint(min = 1, max = 2)
			assertInvalid(
				CollectionSizeValidator,
				listOf(1, 2, 3),
				c,
				ConstraintErrorCode.COLLECTION_TOO_LARGE,
				c,
			)
		}
	}
}
