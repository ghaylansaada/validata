/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.analysis.literal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Parse rules for constraint string literals — IntelliJ adapter over schema
 * `ConstraintLiteralRules` (shared with KSP and runtime).
 *
 * Locks host message wording; parse / classification parity lives in schema `TypeContractsTest`.
 *
 * @author Ghaylan Saada
 */
class ConstraintLiteralFormatsTest {
	
	@Test
	@DisplayName("number literals accept underscores and reject junk")
	fun numberLiteralsAcceptUnderscoresAndRejectJunk() {
		assertThat(ConstraintLiteralFormats.isValidNumber("18")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidNumber("-0.5")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidNumber("1_000")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidNumber("1_000.5")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidNumber("")).isFalse()
		assertThat(ConstraintLiteralFormats.isValidNumber("nope")).isFalse()
		assertThat(ConstraintLiteralFormats.isValidNumber("1.2.3")).isFalse()
	}
	
	@Test
	@DisplayName("isNumericType classifies numeric FQCNs")
	fun numericTypeClassification() {
		assertThat(ConstraintLiteralFormats.isNumericType("kotlin.Int")).isTrue()
		assertThat(ConstraintLiteralFormats.isNumericType("java.math.BigDecimal")).isTrue()
		assertThat(ConstraintLiteralFormats.isNumericType("kotlin.String")).isFalse()
	}
	
	@Test
	@DisplayName("isStringType classifies String / CharSequence FQCNs")
	fun stringTypeClassification() {
		assertThat(ConstraintLiteralFormats.isStringType("kotlin.String")).isTrue()
		assertThat(ConstraintLiteralFormats.isStringType("java.lang.CharSequence")).isTrue()
		assertThat(ConstraintLiteralFormats.isStringType("kotlin.Int")).isFalse()
	}
	
	@Test
	@DisplayName("temporal literals must match the declared java.time type")
	fun temporalLiteralsMatchDeclaredType() {
		assertThat(ConstraintLiteralFormats.isTemporalType("java.time.Instant")).isTrue()
		assertThat(ConstraintLiteralFormats.isTemporalType("kotlin.String")).isFalse()
		
		assertThat(ConstraintLiteralFormats.isValidTemporal("2020-01-01", "java.time.LocalDate")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("12:30:00", "java.time.LocalTime")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("2020-01-01T12:30:00", "java.time.LocalDateTime")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("2020-01-01T12:30:00Z", "java.time.Instant")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("2026", "java.time.Year")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("2020-01", "java.time.YearMonth")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("--01-15", "java.time.MonthDay")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("PT1H", "java.time.Duration")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("P1Y", "java.time.Period")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("JANUARY", "java.time.Month")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("1", "java.time.Month")).isTrue()
		assertThat(ConstraintLiteralFormats.isValidTemporal("13", "java.time.Month")).isFalse()
		assertThat(ConstraintLiteralFormats.isValidTemporal("2020-01-01", "java.time.Instant")).isFalse()
		assertThat(ConstraintLiteralFormats.isValidTemporal("", "java.time.LocalDate")).isFalse()
		assertThat(ConstraintLiteralFormats.isValidTemporal("nope", "java.time.Unknown")).isFalse()
	}
	
	@Test
	@DisplayName("typedLiteralError reports unknown enum constants and accepts valid ones")
	fun typedLiteralEnumErrors() {
		assertThat(
			ConstraintLiteralFormats.typedLiteralError("NOPE", "test.Role", setOf("ADMIN", "USER")),
		).contains("NOPE")
		assertThat(
			ConstraintLiteralFormats.typedLiteralError("ADMIN", "test.Role", setOf("ADMIN")),
		).isNull()
		assertThat(
			ConstraintLiteralFormats.typedLiteralError("18", "kotlin.Int", null),
		).isNull()
	}
	
	@Test
	@DisplayName("typedLiteralError rejects blank non-string and bad number/temporal")
	fun typedLiteralErrorBlankAndTypedFailures() {
		assertThat(ConstraintLiteralFormats.typedLiteralError("", "kotlin.String", null)).isNull()
		assertThat(ConstraintLiteralFormats.typedLiteralError("  ", "kotlin.Int", null)).isEqualTo("must not be blank")
		assertThat(ConstraintLiteralFormats.typedLiteralError("nope", "kotlin.Int", null)).contains("decimal number")
		assertThat(
			ConstraintLiteralFormats.typedLiteralError("nope", "java.time.LocalDate", null),
		).contains("invalid literal")
		assertThat(ConstraintLiteralFormats.typedLiteralError("x", null, null)).isNull()
		assertThat(ConstraintLiteralFormats.typedLiteralError("x", "com.example.Dto", null)).isNull()
	}
}
