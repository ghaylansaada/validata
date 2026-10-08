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
package io.ghaylan.validata.samples.constraint

import io.ghaylan.validata.groups.OnDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [OddYearsValidator]: null-skip, odd pass, even / non-integral fail-closed.*
 * 
 * @author Ghaylan Saada
 */
class OddYearsValidatorTest {
	
	/** Synthetic cursor; these validators do not read siblings.	 */
	private val context = SampleValidationContext()
	
	/** Generated metadata with a blank message (engine uses [OddYearsError] default).	 */
	private fun constraint(): OddYearsConstraint = OddYearsConstraint(message = "", groups = setOf(OnDefault::class))
	
	@Nested
	@DisplayName("Null input")
	inner class NullInput {
		
		@Test
		@DisplayName("null skips (presence is Required)")
				/** null skips (presence is Required)				 */
		fun nullSkips() {
			assertThat(OddYearsValidator.runValidation(null, constraint(), context)).isNull()
		}
	}
	
	@Nested
	@DisplayName("Odd integer")
	inner class OddInteger {
		
		@Test
		@DisplayName("odd Int 2025 is accepted")
				/** odd Int 2025 is accepted				 */
		fun oddIntPasses() {
			assertThat(OddYearsValidator.runValidation(2025, constraint(), context)).isNull()
		}
		
		@Test
		@DisplayName("odd Long 3 is accepted")
				/** odd Long 3 is accepted				 */
		fun oddLongPasses() {
			assertThat(OddYearsValidator.runValidation(3L, constraint(), context)).isNull()
		}
	}
	
	@Nested
	@DisplayName("Even integer")
	inner class EvenInteger {
		
		@Test
		@DisplayName("even Int 4 fails with YEAR_NOT_ODD")
				/** even Int 4 fails with YEAR_NOT_ODD				 */
		fun evenIntFails() {
			val error = OddYearsValidator.runValidation(4, constraint(), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(OddYearsError.YEAR_NOT_ODD)
			assertThat(error.message).isEqualTo(OddYearsError.YEAR_NOT_ODD.message)
		}
		
		@Test
		@DisplayName("even year 2024 fails with YEAR_NOT_ODD")
				/** even year 2024 fails with YEAR_NOT_ODD				 */
		fun evenYearFails() {
			val error = OddYearsValidator.runValidation(2024, constraint(), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(OddYearsError.YEAR_NOT_ODD)
		}
	}
	
	@Nested
	@DisplayName("Non-integral and non-finite")
	inner class NonIntegral {
		
		@Test
		@DisplayName("fractional Double fails with YEAR_NOT_ODD (no silent truncation)")
				/** fractional Double fails with YEAR_NOT_ODD (no silent truncation)				 */
		fun fractionalFails() {
			val error = OddYearsValidator.runValidation(3.5, constraint(), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(OddYearsError.YEAR_NOT_ODD)
		}
		
		@Test
		@DisplayName("NaN fails with YEAR_NOT_ODD")
				/** NaN fails with YEAR_NOT_ODD				 */
		fun nanFails() {
			val error = OddYearsValidator.runValidation(Double.NaN, constraint(), context)
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(OddYearsError.YEAR_NOT_ODD)
		}
	}
}
