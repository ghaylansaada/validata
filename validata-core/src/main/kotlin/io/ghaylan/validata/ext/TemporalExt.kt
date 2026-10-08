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
package io.ghaylan.validata.ext

import io.ghaylan.validata.runtime.ValidationContext
import java.time.*
import java.time.chrono.ChronoLocalDate
import java.time.chrono.ChronoLocalDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal
import kotlin.reflect.KClass

/**
 * Parses a month bound literal for `@Min` / `@Max` on [Month] subjects.
 *
 * Delegates to schema [io.ghaylan.validata.schema.types.ConstraintLiteralRules.parseMonth]
 * (uppercase enum name or ordinal `"1"`…`"12"`).
 * No side effects.
 *
 * @receiver Bound literal string (trimmed internally).
 * @return Parsed [Month], or `null` when the literal is invalid.
 * */
internal fun String.toConstraintMonth(): Month? =
	io.ghaylan.validata.schema.types.ConstraintLiteralRules.parseMonth(this)

/**
 * Converts a [String] into a [Temporal] instance of the specified [clazz].
 *
 * Supported types: [LocalDate], [LocalTime], [OffsetTime], [LocalDateTime], [ZonedDateTime],
 * [OffsetDateTime], [Instant], and [Year]. No side effects.
 *
 * @receiver The string representation of the temporal.
 * @param clazz The target temporal class.
 * @return A temporal instance of type [clazz].
 * @throws IllegalArgumentException If the temporal type is unsupported.
 * @throws DateTimeParseException If [clazz] is supported but this string cannot be parsed.
 * */
internal fun String.toTemporal(clazz: KClass<out Temporal>): Temporal {
	return when (clazz) {
		LocalDate::class -> LocalDate.parse(this)
		LocalTime::class -> LocalTime.parse(this)
		OffsetTime::class -> OffsetTime.parse(this)
		LocalDateTime::class -> LocalDateTime.parse(this)
		ZonedDateTime::class -> ZonedDateTime.parse(this)
		OffsetDateTime::class -> OffsetDateTime.parse(this)
		Instant::class -> Instant.parse(this)
		Year::class -> Year.parse(this)
		else -> throw IllegalArgumentException("Unsupported temporal type: ${clazz.java.name}")
	}
}

/**
 * Returns the current moment as the same concrete [Temporal] type as the receiver.
 *
 * Used by relative temporal constraints (`@Past` / `@Future`) so "now" matches the annotated
 * property's clock representation ([LocalDate] vs [Instant], etc.).
 *
 * Prefer [nowMatching] so a validation run pins "now" once per concrete temporal class.
 *
 * Covers every `java.time` type that implements [Temporal] and is ordered by [compareTemporal]:
 * ISO locals/offsets/zones, [Instant], [Year], and chronology-backed calendars via
 * [ChronoLocalDate] / [ChronoLocalDateTime] / [ChronoZonedDateTime].
 *
 * @receiver Temporal whose concrete type selects the clock representation.
 * @param clock Clock used for "now" (defaults to the system default zone).
 * @return "Now" as the same concrete [Temporal] subtype.
 * @throws IllegalArgumentException When [receiver][Temporal] has no supported "now" mapping.
 * */
internal fun Temporal.now(clock: Clock = Clock.systemDefaultZone()): Temporal {
	return when (this) {
		is LocalDate -> LocalDate.now(clock)
		is LocalTime -> LocalTime.now(clock)
		is OffsetTime -> OffsetTime.now(clock)
		is LocalDateTime -> LocalDateTime.now(clock)
		is ZonedDateTime -> ZonedDateTime.now(clock)
		is OffsetDateTime -> OffsetDateTime.now(clock)
		is Instant -> Instant.now(clock)
		is Year -> Year.now(clock)
		// Chronology calendars (JapaneseDate, HijrahDate, …) — after ISO concretes above.
		is ChronoLocalDate -> chronology.dateNow(clock)
		is ChronoLocalDateTime<*> -> chronology.localDateTime(LocalDateTime.now(clock))
		is ChronoZonedDateTime<*> -> chronology.zonedDateTime(ZonedDateTime.now(clock))
		else -> error("Unsupported Temporal for relative checks: ${this::class.java.name}")
	}
}

/**
 * Attribute-bag key for run-scoped [nowMatching] memoization (one entry per concrete Temporal class).*
 * 
 * @author Ghaylan Saada
 */
@JvmInline
private value class TemporalNowKey(val type: Class<*>)

/**
 * Returns "now" as the same concrete [Temporal] type as [sample], memoized once per validation run
 * per concrete class so list/map walks do not call the system clock per element.
 *
 * Uses [clock] and [getOrComputeAttribute].
 *
 * @receiver Active validation context for the run.
 * @param sample Subject under validation (type selector only).
 * @return Run-scoped "now" for [sample]'s concrete type.
 * */
internal fun ValidationContext.nowMatching(sample: Temporal): Temporal =
	getOrComputeAttribute(TemporalNowKey(sample.javaClass)) {
		sample.now(clock)
	}

/**
 * Returns `true` if this temporal equals [other] by chronological comparison.
 *
 * @param other Temporal of the same concrete type.
 * @return `true` when chronologically equal.
 * @throws IllegalArgumentException If the concrete types differ or are unsupported.
 * */
fun Temporal.isEqual(other: Temporal): Boolean = compareTemporal(this, other) == 0

/**
 * Returns `true` if this temporal is before or equal to [other].
 *
 * @param other Temporal of the same concrete type.
 * @return `true` when this ≤ [other] chronologically.
 * @throws IllegalArgumentException If the concrete types differ or are unsupported.
 * */
fun Temporal.isBeforeOrEqual(other: Temporal): Boolean = compareTemporal(this, other) <= 0

/**
 * Returns `true` if this temporal is strictly before [other].
 *
 * Both must be the same concrete type.
 *
 * @param other Temporal of the same concrete type.
 * @return `true` when this < [other] chronologically.
 * @throws IllegalArgumentException If the concrete types differ or are unsupported.
 * */
fun Temporal.isBefore(other: Temporal): Boolean = compareTemporal(this, other) < 0

/**
 * Returns `true` if this temporal is after or equal to [other].
 *
 * @param other Temporal of the same concrete type.
 * @return `true` when this ≥ [other] chronologically.
 * @throws IllegalArgumentException If the concrete types differ or are unsupported.
 * */
fun Temporal.isAfterOrEqual(other: Temporal): Boolean = compareTemporal(this, other) >= 0

/**
 * Returns `true` if this temporal is strictly after [other].
 *
 * Both must be the same concrete type.
 *
 * @param other Temporal of the same concrete type.
 * @return `true` when this > [other] chronologically.
 * @throws IllegalArgumentException If the concrete types differ or are unsupported.
 * */
fun Temporal.isAfter(other: Temporal): Boolean = compareTemporal(this, other) > 0

/**
 * Chronological comparison for two [Temporal] values of the same concrete type.
 *
 * Shared by the public Temporal ordering extensions so each comparison operator stays a one-liner
 * without duplicating the type switch. Package-internal: callers should use [isBefore] / [isAfter]
 * / … rather than this helper directly. No side effects.
 *
 * @param value1 Left operand.
 * @param value2 Right operand; must match [value1]'s concrete type.
 * @return Negative if [value1] < [value2], zero if equal, positive if [value1] > [value2].
 * @throws IllegalArgumentException If the concrete types differ or are unsupported.
 * */
@Suppress("UNCHECKED_CAST")
internal fun compareTemporal(value1: Temporal, value2: Temporal): Int {
	return when (value1) {
		is LocalDate if value2 is LocalDate -> value1.compareTo(value2)
		is LocalTime if value2 is LocalTime -> value1.compareTo(value2)
		is LocalDateTime if value2 is LocalDateTime -> value1.compareTo(value2)
		is ZonedDateTime if value2 is ZonedDateTime -> value1.compareTo(value2)
		is OffsetDateTime if value2 is OffsetDateTime -> value1.compareTo(value2)
		is OffsetTime if value2 is OffsetTime -> value1.compareTo(value2)
		is Instant if value2 is Instant -> value1.compareTo(value2)
		is Year if value2 is Year -> value1.compareTo(value2)
		is ChronoLocalDate if value2 is ChronoLocalDate -> value1.compareTo(value2)
		is ChronoLocalDateTime<*> if value2 is ChronoLocalDateTime<*> -> value1.compareTo(value2)
		is ChronoZonedDateTime<*> if value2 is ChronoZonedDateTime<*> -> value1.compareTo(value2)
		else -> throw IllegalArgumentException(
			"Unsupported or mismatched Temporal types: ${value1::class.simpleName} vs ${value2::class.simpleName}",
		)
	}
}

/**
 * Orders periods by [Period.toTotalMonths], then by [Period.getDays].
 *
 * Periods are not a single absolute timeline (calendar months vs days), so this matches
 * common “length of leave / subscription” comparisons rather than date arithmetic.
 * Kept package-local so Min/Max validators share one ordering policy. No side effects.
 *
 * @param a Left [Period].
 * @param b Right [Period].
 * @return Negative if [a] is shorter, zero if equal, positive if [a] is longer.
 * */
internal fun comparePeriods(a: Period, b: Period): Int {
	val months = a.toTotalMonths().compareTo(b.toTotalMonths())
	if (months != 0) return months
	return a.days.compareTo(b.days)
}
