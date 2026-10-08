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
package io.ghaylan.validata.constraint.validator.temporal.relativetonow

import io.ghaylan.validata.constraint.annotation.RelativeToNow
import io.ghaylan.validata.constraint.annotation.RelativeToNowConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.TestValidationContext
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Unit tests for [RelativeToNowValidator] — relation matching and the optional `within` window.
 * 
 * @author Ghaylan Saada
 */
@DisplayName("RelativeToNowValidator")
class RelativeToNowValidatorTest {
	
	private val now = LocalDate.of(2026, 6, 15)
	
	private val fixed = TestValidationContext(
		clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC),
	)
	
	private fun constraint(
		relation: RelativeToNow.Relation,
		within: Int = Int.MAX_VALUE,
		unit: ChronoUnit = ChronoUnit.DAYS,
	) = RelativeToNowConstraint(relation, within, unit, "", ValidatorTestSupport.defaultGroups)
	
	@Test
	@DisplayName("null value is skipped")
	fun skipsNull() {
		assertSkipsNull(RelativeToNowValidator, constraint(RelativeToNow.Relation.LT))
	}
	
	@Nested
	@DisplayName("relations")
	inner class Relations {
		
		@Test
		@DisplayName("LT accepts the past and rejects today")
		fun beforeNow() {
			val c = constraint(RelativeToNow.Relation.LT)
			assertValid(RelativeToNowValidator, now.minusDays(1), c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now,
				c,
				ConstraintErrorCode.TEMPORAL_NOT_IN_PAST,
				fixed,
			)
		}
		
		@Test
		@DisplayName("LTE accepts the past and today")
		fun beforeOrNow() {
			val c = constraint(RelativeToNow.Relation.LTE)
			assertValid(RelativeToNowValidator, now.minusDays(1), c, fixed)
			assertValid(RelativeToNowValidator, now, c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now.plusDays(1),
				c,
				ConstraintErrorCode.TEMPORAL_NOT_IN_PAST,
				fixed,
			)
		}
		
		@Test
		@DisplayName("GT accepts the future and rejects today")
		fun afterNow() {
			val c = constraint(RelativeToNow.Relation.GT)
			assertValid(RelativeToNowValidator, now.plusDays(1), c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now,
				c,
				ConstraintErrorCode.TEMPORAL_NOT_IN_FUTURE,
				fixed,
			)
		}
		
		@Test
		@DisplayName("GTE accepts the future and today")
		fun afterOrNow() {
			val c = constraint(RelativeToNow.Relation.GTE)
			assertValid(RelativeToNowValidator, now.plusDays(1), c, fixed)
			assertValid(RelativeToNowValidator, now, c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now.minusDays(1),
				c,
				ConstraintErrorCode.TEMPORAL_NOT_IN_FUTURE,
				fixed,
			)
		}
		
		@Test
		@DisplayName("EQ accepts only today")
		fun equalToNow() {
			val c = constraint(RelativeToNow.Relation.EQ)
			assertValid(RelativeToNowValidator, now, c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now.plusDays(1),
				c,
				ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL,
				fixed,
			)
		}
	}
	
	/**
	 * The window applies only for `within in 1 until Int.MAX_VALUE`; the default and any
	 * non-positive value leave the relation unbounded.
	 */
	@Nested
	@DisplayName("within window")
	inner class WithinWindow {
		
		@Test
		@DisplayName("finite window bounds how far ahead a future date may be")
		fun futureWindow() {
			val c = constraint(RelativeToNow.Relation.GT, within = 7)
			assertValid(RelativeToNowValidator, now.plusDays(7), c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now.plusDays(8),
				c,
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
				fixed,
			)
		}
		
		@Test
		@DisplayName("finite window bounds how far back a past date may be")
		fun pastWindow() {
			val c = constraint(RelativeToNow.Relation.LT, within = 7)
			assertValid(RelativeToNowValidator, now.minusDays(7), c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now.minusDays(8),
				c,
				ConstraintErrorCode.TEMPORAL_TOO_EARLY,
				fixed,
			)
		}
		
		@Test
		@DisplayName("Int.MAX_VALUE leaves the relation unbounded")
		fun maxValueIsUnbounded() {
			val c = constraint(RelativeToNow.Relation.GT, within = Int.MAX_VALUE)
			assertValid(RelativeToNowValidator, now.plusYears(500), c, fixed)
		}
		
		@Test
		@DisplayName("non-positive within leaves the relation unbounded")
		fun nonPositiveIsUnbounded() {
			assertValid(
				RelativeToNowValidator,
				now.plusYears(500),
				constraint(RelativeToNow.Relation.GT, within = 0),
				fixed,
			)
			assertValid(
				RelativeToNowValidator,
				now.minusYears(500),
				constraint(RelativeToNow.Relation.LT, within = -1),
				fixed,
			)
		}
		
		@Test
		@DisplayName("window honours the configured unit")
		fun honoursUnit() {
			val c = constraint(RelativeToNow.Relation.GT, within = 1, unit = ChronoUnit.MONTHS)
			assertValid(RelativeToNowValidator, now.plusDays(20), c, fixed)
			assertInvalid(
				RelativeToNowValidator,
				now.plusDays(40),
				c,
				ConstraintErrorCode.TEMPORAL_TOO_LATE,
				fixed,
			)
		}
	}
}
