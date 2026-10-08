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
package io.ghaylan.validata.processor.verify

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Unit coverage for [ConstraintLiteralVerifier] parse helpers.
 * 
 * @author Ghaylan Saada
 */
class ConstraintLiteralVerifierTest {
	
	@Test
	@DisplayName("number literals accept decimals and reject garbage")
	fun numberLiterals() {
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("18")).isTrue()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("0.5")).isTrue()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("-3")).isTrue()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("1_000")).isTrue()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("1_000.5")).isTrue()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("")).isFalse()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("abc")).isFalse()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("1.2.3")).isFalse()
	}
	
	@Test
	@DisplayName("temporal literals match subject type")
	fun temporalLiterals() {
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral("2020-01-01", TypeNames.LOCAL_DATE),
		).isTrue()
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral("12:30:00", TypeNames.LOCAL_TIME),
		).isTrue()
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral(
				"2020-01-01T00:00:00Z",
				TypeNames.INSTANT,
			),
		).isTrue()
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral("2020-01-01", TypeNames.INSTANT),
		).isFalse()
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral("not-a-date", TypeNames.LOCAL_DATE),
		).isFalse()
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral("JANUARY", TypeNames.MONTH),
		).isTrue()
		assertThat(
			ConstraintLiteralVerifier.isValidTemporalLiteral("january", TypeNames.MONTH),
		).isTrue()
	}
	
	@Test
	@DisplayName("number literals reject blank and whitespace-only")
	fun numberLiteralsRejectBlank() {
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("   ")).isFalse()
		assertThat(ConstraintLiteralVerifier.isValidNumberLiteral("\t")).isFalse()
	}
	
	@Test
	@DisplayName("temporal literals reject blank for every supported type")
	fun temporalLiteralsRejectBlank() {
		for (typeQ in listOf(
			TypeNames.LOCAL_DATE,
			TypeNames.LOCAL_TIME,
			TypeNames.INSTANT,
			TypeNames.LOCAL_DATE_TIME,
			TypeNames.YEAR_MONTH,
			TypeNames.MONTH,
		)) {
			assertThat(ConstraintLiteralVerifier.isValidTemporalLiteral("", typeQ)).withFailMessage("blank should fail for $typeQ")
				.isFalse()
			assertThat(ConstraintLiteralVerifier.isValidTemporalLiteral("  ", typeQ)).withFailMessage("whitespace should fail for $typeQ")
				.isFalse()
		}
	}
}
