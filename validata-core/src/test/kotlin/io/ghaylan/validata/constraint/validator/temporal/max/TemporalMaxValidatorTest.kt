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
package io.ghaylan.validata.constraint.validator.temporal.max

import io.ghaylan.validata.constraint.annotation.MaxConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Unit tests for [TemporalMaxValidator].
 * 
 * @author Ghaylan Saada
 */
class TemporalMaxValidatorTest {
	
	private fun c(
		value: String,
		inclusive: Boolean = true
	) = MaxConstraint(value, inclusive, "", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("skips null temporal")
		fun skipsNull() {
			assertSkipsNull(TemporalMaxValidator, c("2030-12-31"))
		}
	}
	
	@Nested
	@DisplayName("inclusive vs exclusive")
	inner class InclusiveExclusive {
		
		@Test
		@DisplayName("inclusive Max allows equality")
		fun inclusiveAllowsEquality() {
			assertValid(TemporalMaxValidator, LocalDate.of(2030, 12, 31), c("2030-12-31", inclusive = true))
		}
		
		@Test
		@DisplayName("exclusive Max rejects equality with TEMPORAL_TOO_LATE")
		fun exclusiveRejectsEquality() {
			assertInvalid(
				TemporalMaxValidator,
				LocalDate.of(2030, 12, 31),
				c("2030-12-31", inclusive = false),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
		}
	}
	
	@Nested
	@DisplayName("valid values")
	inner class ValidValues {
		
		@Test
		@DisplayName("date before maximum passes")
		fun beforeMaximumPasses() {
			assertValid(TemporalMaxValidator, LocalDate.of(2030, 6, 1), c("2030-12-31"))
		}
	}
	
	@Nested
	@DisplayName("violations")
	inner class Violations {
		
		@Test
		@DisplayName("date after maximum fails with TEMPORAL_TOO_LATE")
		fun afterMaximumFails() {
			assertInvalid(
				TemporalMaxValidator,
				LocalDate.of(2031, 1, 1),
				c("2030-12-31"),
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
			)
		}
	}
}
