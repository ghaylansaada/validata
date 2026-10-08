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
package io.ghaylan.validata.constraint.validator.comparison

import io.ghaylan.validata.constraint.annotation.Compare
import io.ghaylan.validata.constraint.annotation.CompareConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [ComparisonSupport] type guards and comparison helpers.
 *
 * @author Ghaylan Saada
 */
class ComparisonSupportTest {

	private fun constraint(ref: String = "other") = CompareConstraint(
		ref,
		Compare.Operation.EQ,
		"",
		ValidatorTestSupport.defaultGroups,
	)

	@Nested
	@DisplayName("typeMismatchOrNull")
	inner class TypeMismatchOrNull {

		@Test
		@DisplayName("returns null when runtime types match")
		fun matchingTypes() {
			assertThat(
				ComparisonSupport.typeMismatchOrNull(
					value = 10,
					other = 20,
					refName = "other",
					constraint = constraint(),
				),
			).isNull()
		}

		@Test
		@DisplayName("returns COMPARISON_NOT_ORDERABLE when types differ")
		fun mismatchedTypes() {
			val constraint = constraint()
			val error = ComparisonSupport.typeMismatchOrNull(
				value = 10,
				other = "10",
				refName = "other",
				constraint = constraint,
			)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.COMPARISON_NOT_ORDERABLE)
			assertThat(error.metadata).isEqualTo(constraint)
			assertThat(error.message).contains("'other'")
		}

		@Test
		@DisplayName("uses isInstance against the annotated value runtime class")
		fun matchingRuntimeClass() {
			val value: Number = 10
			val other: Number = 20
			assertThat(
				ComparisonSupport.typeMismatchOrNull(
					value = value,
					other = other,
					refName = "other",
					constraint = constraint(),
				),
			).isNull()
		}

		@Test
		@DisplayName("rejects sibling values that are not instances of the annotated runtime class")
		fun differentNumericRuntimeClasses() {
			val value: Number = 10
			val other: Number = 20L
			val error = ComparisonSupport.typeMismatchOrNull(
				value = value,
				other = other,
				refName = "other",
				constraint = constraint(),
			)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.COMPARISON_NOT_ORDERABLE)
		}
	}

	@Nested
	@DisplayName("compare")
	inner class CompareValues {

		@Test
		@DisplayName("delegates to Comparable.compareTo for numbers")
		fun numbers() {
			assertThat(ComparisonSupport.compare(10, 5)).isGreaterThan(0)
			assertThat(ComparisonSupport.compare(5, 10)).isLessThan(0)
			assertThat(ComparisonSupport.compare(7, 7)).isZero()
		}

		@Test
		@DisplayName("delegates to Comparable.compareTo for temporals")
		fun temporals() {
			val earlier = LocalDate.of(2024, 1, 1)
			val later = LocalDate.of(2024, 6, 1)
			assertThat(ComparisonSupport.compare(later, earlier)).isGreaterThan(0)
			assertThat(ComparisonSupport.compare(earlier, later)).isLessThan(0)
		}
	}

	@Nested
	@DisplayName("greaterThanFailureCode")
	inner class GreaterThanFailureCode {

		@Test
		@DisplayName("inclusive uses COMPARISON_UNSATISFIED_LESS_THAN")
		fun inclusive() {
			assertThat(ComparisonSupport.greaterThanFailureCode(inclusive = true))
				.isEqualTo(ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN)
		}

		@Test
		@DisplayName("exclusive uses COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL")
		fun exclusive() {
			assertThat(ComparisonSupport.greaterThanFailureCode(inclusive = false))
				.isEqualTo(ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL)
		}
	}

	@Nested
	@DisplayName("lessThanFailureCode")
	inner class LessThanFailureCode {

		@Test
		@DisplayName("inclusive uses COMPARISON_UNSATISFIED_GREATER_THAN")
		fun inclusive() {
			assertThat(ComparisonSupport.lessThanFailureCode(inclusive = true))
				.isEqualTo(ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN)
		}

		@Test
		@DisplayName("exclusive uses COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL")
		fun exclusive() {
			assertThat(ComparisonSupport.lessThanFailureCode(inclusive = false))
				.isEqualTo(ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL)
		}
	}
}
