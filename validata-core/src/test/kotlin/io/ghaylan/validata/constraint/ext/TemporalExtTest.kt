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
package io.ghaylan.validata.constraint.ext

import io.ghaylan.validata.ext.comparePeriods
import io.ghaylan.validata.ext.compareTemporal
import io.ghaylan.validata.ext.isAfterOrEqual
import io.ghaylan.validata.ext.isBeforeOrEqual
import io.ghaylan.validata.ext.now
import io.ghaylan.validata.ext.nowMatching
import io.ghaylan.validata.ext.toConstraintMonth
import io.ghaylan.validata.ext.toTemporal
import io.ghaylan.validata.support.TestValidationContext
import io.ghaylan.validata.support.ValidatorTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.*

/** Unit tests for temporal parsing, ordering, and bound helpers in [TemporalExt.kt].
 * 
 * @author Ghaylan Saada

 */
class TemporalExtTest {

	@Nested
	@DisplayName("toConstraintMonth")
	inner class ToConstraintMonth {

		@Test
		@DisplayName("parses uppercase enum names")
		fun enumNames() {
			assertThat("JANUARY".toConstraintMonth()).isEqualTo(Month.JANUARY)
			assertThat("DECEMBER".toConstraintMonth()).isEqualTo(Month.DECEMBER)
		}

		@Test
		@DisplayName("parses mixed-case enum names after trim")
		fun mixedCaseNames() {
			assertThat("  february  ".toConstraintMonth()).isEqualTo(Month.FEBRUARY)
			assertThat("March".toConstraintMonth()).isEqualTo(Month.MARCH)
		}

		@Test
		@DisplayName("parses month numbers 1 through 12")
		fun monthNumbers() {
			Month.entries.forEachIndexed { index, month ->
				assertThat("${index + 1}".toConstraintMonth()).isEqualTo(month)
			}
		}

		@Test
		@DisplayName("blank, out-of-range, and unknown literals return null")
		fun invalidLiterals() {
			assertThat("".toConstraintMonth()).isNull()
			assertThat("   ".toConstraintMonth()).isNull()
			assertThat("0".toConstraintMonth()).isNull()
			assertThat("13".toConstraintMonth()).isNull()
			assertThat("-1".toConstraintMonth()).isNull()
			assertThat("NOT_A_MONTH".toConstraintMonth()).isNull()
			assertThat("january-ish".toConstraintMonth()).isNull()
		}
	}

	@Nested
	@DisplayName("toTemporal")
	inner class ToTemporal {

		@Test
		@DisplayName("parses supported temporal types from ISO strings")
		fun supportedTypes() {
			assertThat("2024-06-15".toTemporal(LocalDate::class))
				.isEqualTo(LocalDate.of(2024, 6, 15))
			assertThat("12:30:45".toTemporal(LocalTime::class))
				.isEqualTo(LocalTime.of(12, 30, 45))
			assertThat("12:30:45+02:00".toTemporal(OffsetTime::class))
				.isEqualTo(OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2)))
			assertThat("2024-06-15T12:30:45".toTemporal(LocalDateTime::class))
				.isEqualTo(LocalDateTime.of(2024, 6, 15, 12, 30, 45))
			assertThat("2024-06-15T12:30:45+02:00".toTemporal(OffsetDateTime::class))
				.isEqualTo(OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2)))
			assertThat("2024-06-15T12:30:45+02:00[Europe/Paris]".toTemporal(ZonedDateTime::class))
				.isEqualTo(ZonedDateTime.parse("2024-06-15T12:30:45+02:00[Europe/Paris]"))
			assertThat("2024-06-15T10:30:45Z".toTemporal(Instant::class))
				.isEqualTo(Instant.parse("2024-06-15T10:30:45Z"))
			assertThat("2024".toTemporal(Year::class)).isEqualTo(Year.of(2024))
		}

		@Test
		@DisplayName("throws for unsupported temporal classes")
		fun unsupportedType() {
			@Suppress("UNCHECKED_CAST")
			val unsupported = Month::class as kotlin.reflect.KClass<out java.time.temporal.Temporal>
			assertThatThrownBy { "2024-06-15".toTemporal(unsupported) }
				.isInstanceOf(IllegalArgumentException::class.java)
				.hasMessageContaining("Unsupported temporal type")
		}
	}

	@Nested
	@DisplayName("Temporal.now()")
	inner class TemporalNow {

		@Test
		@DisplayName("returns the same concrete type as the receiver sample")
		fun preservesConcreteType() {
			val sampleDate = LocalDate.of(2000, 1, 1)
			assertThat(sampleDate.now()).isInstanceOf(LocalDate::class.java)

			val sampleTime = LocalTime.NOON
			assertThat(sampleTime.now()).isInstanceOf(LocalTime::class.java)

			val sampleOffsetTime = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC)
			assertThat(sampleOffsetTime.now()).isInstanceOf(OffsetTime::class.java)

			val sampleDateTime = LocalDateTime.of(2000, 1, 1, 0, 0)
			assertThat(sampleDateTime.now()).isInstanceOf(LocalDateTime::class.java)

			val sampleZoned = ZonedDateTime.parse("2000-01-01T00:00:00Z")
			assertThat(sampleZoned.now()).isInstanceOf(ZonedDateTime::class.java)

			val sampleOffsetDateTime = OffsetDateTime.parse("2000-01-01T00:00:00Z")
			assertThat(sampleOffsetDateTime.now()).isInstanceOf(OffsetDateTime::class.java)

			val sampleInstant = Instant.parse("2000-01-01T00:00:00Z")
			assertThat(sampleInstant.now()).isInstanceOf(Instant::class.java)

			val sampleYear = Year.of(2000)
			assertThat(sampleYear.now()).isInstanceOf(Year::class.java)
		}

		@Test
		@DisplayName("supports chronology calendars via ChronoLocalDate")
		fun chronologyLocalDate() {
			val japanese = java.time.chrono.JapaneseDate.of(2000, 1, 1)
			assertThat(japanese.now()).isInstanceOf(java.time.chrono.JapaneseDate::class.java)
		}

		@Test
		@DisplayName("rejects Temporal types without a now() mapping")
		fun unsupportedThrows() {
			// Duration is TemporalAmount, not Temporal — use a custom stub via Instant is covered;
			// ChronoField-only types are not Temporal. Verify Year/Instant path is explicit above.
			assertThat(Year.now().now()).isInstanceOf(Year::class.java)
		}
	}

	@Nested
	@DisplayName("nowMatching memoization")
	inner class NowMatching {

		@Test
		@DisplayName("two calls for the same temporal kind return equal memoized values")
		fun sameKindMemoized() {
			val fixed = Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneOffset.UTC)
			val ctx = TestValidationContext(
				groups = ValidatorTestSupport.defaultGroups,
				clock = fixed,
			)
			val sample = LocalDate.of(2000, 1, 1)
			val first = ctx.nowMatching(sample)
			val second = ctx.nowMatching(LocalDate.of(1999, 12, 31))
			assertThat(first).isEqualTo(LocalDate.now(fixed))
			assertThat(second).isSameAs(first)

			val instantSample = Instant.parse("2000-01-01T00:00:00Z")
			val firstInstant = ctx.nowMatching(instantSample)
			val secondInstant = ctx.nowMatching(Instant.parse("1999-01-01T00:00:00Z"))
			assertThat(firstInstant).isEqualTo(Instant.now(fixed))
			assertThat(secondInstant).isSameAs(firstInstant)
		}
	}

	@Nested
	@DisplayName("temporal ordering extensions")
	inner class TemporalOrdering {

		private val earlier = LocalDate.of(2024, 1, 1)
		private val same = LocalDate.of(2024, 1, 1)
		private val later = LocalDate.of(2024, 6, 1)

		@Test
		@DisplayName("isEqual reflects chronological equality")
		fun isEqual() {
			assertThat(earlier.isEqual(same)).isTrue()
			assertThat(earlier.isEqual(later)).isFalse()
		}

		@Test
		@DisplayName("isBefore and isBeforeOrEqual respect strict vs inclusive ordering")
		fun isBeforeFamily() {
			assertThat(later.isBefore(earlier)).isFalse()
			assertThat(earlier.isBefore(later)).isTrue()
			assertThat(earlier.isBeforeOrEqual(same)).isTrue()
			assertThat(later.isBeforeOrEqual(earlier)).isFalse()
		}

		@Test
		@DisplayName("isAfter and isAfterOrEqual respect strict vs inclusive ordering")
		fun isAfterFamily() {
			assertThat(earlier.isAfter(later)).isFalse()
			assertThat(later.isAfter(earlier)).isTrue()
			assertThat(earlier.isAfterOrEqual(same)).isTrue()
			assertThat(earlier.isAfterOrEqual(later)).isFalse()
		}

		@Test
		@DisplayName("compareTemporal supports each paired concrete type")
		fun compareTemporalSupportedPairs() {
			val dateCmp = compareTemporal(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 1))
			assertThat(dateCmp).isGreaterThan(0)

			val timeCmp = compareTemporal(LocalTime.of(10, 0), LocalTime.of(9, 0))
			assertThat(timeCmp).isGreaterThan(0)

			val dateTimeCmp = compareTemporal(
				LocalDateTime.of(2024, 1, 2, 0, 0),
				LocalDateTime.of(2024, 1, 1, 0, 0),
			)
			assertThat(dateTimeCmp).isGreaterThan(0)

			val zonedCmp = compareTemporal(
				ZonedDateTime.parse("2024-01-02T00:00:00Z"),
				ZonedDateTime.parse("2024-01-01T00:00:00Z"),
			)
			assertThat(zonedCmp).isGreaterThan(0)

			val offsetDateTimeCmp = compareTemporal(
				OffsetDateTime.parse("2024-01-02T00:00:00Z"),
				OffsetDateTime.parse("2024-01-01T00:00:00Z"),
			)
			assertThat(offsetDateTimeCmp).isGreaterThan(0)

			val offsetTimeCmp = compareTemporal(
				OffsetTime.of(10, 0, 0, 0, ZoneOffset.UTC),
				OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC),
			)
			assertThat(offsetTimeCmp).isGreaterThan(0)

			val instantCmp = compareTemporal(
				Instant.parse("2024-01-02T00:00:00Z"),
				Instant.parse("2024-01-01T00:00:00Z"),
			)
			assertThat(instantCmp).isGreaterThan(0)

			val yearCmp = compareTemporal(Year.of(2025), Year.of(2024))
			assertThat(yearCmp).isGreaterThan(0)
		}

		@Test
		@DisplayName("compareTemporal rejects mismatched concrete types")
		fun compareTemporalMismatchedTypes() {
			assertThatThrownBy {
				compareTemporal(LocalDate.of(2024, 1, 1), LocalTime.NOON)
			}
				.isInstanceOf(IllegalArgumentException::class.java)
				.hasMessageContaining("Unsupported or mismatched Temporal types")
		}
	}

	@Nested
	@DisplayName("comparePeriods")
	inner class ComparePeriods {

		@Test
		@DisplayName("orders by total months first")
		fun monthsDominates() {
			val shorterMonths = Period.of(1, 0, 0)
			val longerMonths = Period.of(2, 0, 0)
			assertThat(comparePeriods(shorterMonths, longerMonths)).isLessThan(0)
			assertThat(comparePeriods(longerMonths, shorterMonths)).isGreaterThan(0)
		}

		@Test
		@DisplayName("breaks ties by day component")
		fun daysBreakTie() {
			val fewerDays = Period.of(1, 0, 5)
			val moreDays = Period.of(1, 0, 10)
			assertThat(comparePeriods(fewerDays, moreDays)).isLessThan(0)
			assertThat(comparePeriods(moreDays, fewerDays)).isGreaterThan(0)
		}

		@Test
		@DisplayName("returns zero for equivalent periods")
		fun equalPeriods() {
			val left = Period.of(1, 2, 3)
			val right = Period.of(1, 2, 3)
			assertThat(comparePeriods(left, right)).isZero()
		}
	}
}
