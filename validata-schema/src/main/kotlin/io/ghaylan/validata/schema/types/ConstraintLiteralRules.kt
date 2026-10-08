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
package io.ghaylan.validata.schema.types

import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Pure parse rules for constraint string literals (`@Min("18")`, temporal bounds, …).
 *
 * Shared by KSP, IntelliJ, and runtime (`toConstraintNumber` / month bounds).
 * Does not resolve types — callers pass FQCNs and optional enum constant names.
 *
 * @author Ghaylan Saada
 */
object ConstraintLiteralRules {

	/**
	 * Parses a constraint bound / factor string into a [BigDecimal].
	 *
	 * Underscores are digit separators (Kotlin-style), e.g. `"1_000"`.
	 *
	 * @param raw Raw bound / factor literal.
	 * @return Finite decimal, or `null` when blank or not a number.
	 */
	fun parseNumber(raw: String): BigDecimal? {
		val normalized = raw.trim().replace("_", "")
		if (normalized.isEmpty()) return null
		return try {
			BigDecimal(normalized)
		} catch (_: NumberFormatException) {
			null
		}
	}

	/**
	 * Whether [raw] parses as a decimal number after trim and underscore removal.
	 */
	fun isValidNumber(raw: String): Boolean =
		parseNumber(raw) != null

	/**
	 * Parses a [Month] literal: uppercase enum name (`"JANUARY"`) or ordinal `"1"`…`"12"`.
	 *
	 * @param raw Bound literal string.
	 * @return Parsed [Month], or `null` when invalid.
	 */
	fun parseMonth(raw: String): Month? {
		val trimmed = raw.trim()
		if (trimmed.isEmpty()) return null
		return try {
			trimmed.toIntOrNull()?.let { Month.of(it) }
				?: Month.valueOf(trimmed.uppercase(Locale.ROOT))
		} catch (_: IllegalArgumentException) {
			null
		} catch (_: DateTimeException) {
			null
		}
	}

	/**
	 * Whether [raw] parses as `java.time` type named by [typeQualifiedName].
	 */
	fun isValidTemporal(raw: String, typeQualifiedName: String): Boolean {
		val trimmed = raw.trim()
		if (trimmed.isEmpty()) return false
		return try {
			when (typeQualifiedName) {
				TypeNames.LOCAL_DATE -> LocalDate.parse(trimmed)
				TypeNames.LOCAL_TIME -> LocalTime.parse(trimmed)
				TypeNames.OFFSET_TIME -> OffsetTime.parse(trimmed)
				TypeNames.LOCAL_DATE_TIME -> LocalDateTime.parse(trimmed)
				TypeNames.ZONED_DATE_TIME -> ZonedDateTime.parse(trimmed)
				TypeNames.OFFSET_DATE_TIME -> OffsetDateTime.parse(trimmed)
				TypeNames.INSTANT -> Instant.parse(trimmed)
				TypeNames.YEAR -> Year.parse(trimmed)
				TypeNames.DURATION -> Duration.parse(trimmed)
				TypeNames.PERIOD -> Period.parse(trimmed)
				TypeNames.YEAR_MONTH -> YearMonth.parse(trimmed)
				TypeNames.MONTH_DAY -> MonthDay.parse(trimmed)
				TypeNames.MONTH -> parseMonth(trimmed) ?: return false
				else -> return false
			}
			true
		} catch (_: DateTimeParseException) {
			false
		} catch (_: IllegalArgumentException) {
			false
		} catch (_: DateTimeException) {
			false
		}
	}

	/**
	 * Typed-literal check shared by `@ConstraintArg(TYPED_LITERAL)` tooling.
	 *
	 * @param raw Literal text without quotes.
	 * @param typeQualifiedName Subject / gate FQCN, or `null` when unresolved → [TypedLiteralResult.Ok].
	 * @param enumConstantNames When the subject is an enum, its constant names; otherwise `null`.
	 */
	fun checkTypedLiteral(
		raw: String,
		typeQualifiedName: String?,
		enumConstantNames: Set<String>? = null,
	): TypedLiteralResult {
		val typeQ = typeQualifiedName ?: return TypedLiteralResult.Ok
		if (raw.isBlank()) {
			return if (KnownTypes.isCharSequenceLike(typeQ)) TypedLiteralResult.Ok
			else TypedLiteralResult.BlankNotAllowed
		}
		return when {
			KnownTypes.isNumeric(typeQ) -> {
				if (isValidNumber(raw)) TypedLiteralResult.Ok
				else TypedLiteralResult.InvalidNumber
			}
			KnownTypes.isTemporalLiteralHost(typeQ) -> {
				if (isValidTemporal(raw, typeQ)) TypedLiteralResult.Ok
				else TypedLiteralResult.InvalidTemporal(typeQ)
			}
			KnownTypes.isCharSequenceLike(typeQ) -> TypedLiteralResult.Ok
			enumConstantNames != null -> {
				if (raw in enumConstantNames) TypedLiteralResult.Ok
				else TypedLiteralResult.UnknownEnumConstant(raw, typeQ, enumConstantNames)
			}
			else -> TypedLiteralResult.Ok
		}
	}
}
