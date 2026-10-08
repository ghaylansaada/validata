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

import io.ghaylan.validata.ext.toConstraintNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Parse rules for `@Min` / `@Max` / `@MultipleOf` bound strings.
 * 
 * @author Ghaylan Saada
 */
class StringToConstraintNumberTest {
	
	@Test
	@DisplayName("plain decimals parse")
	fun plainDecimals() {
		assertThat("18".toConstraintNumber()).isEqualByComparingTo(BigDecimal("18"))
		assertThat("-0.5".toConstraintNumber()).isEqualByComparingTo(BigDecimal("-0.5"))
	}
	
	@Test
	@DisplayName("underscores are digit separators")
	fun underscoreSeparators() {
		assertThat("1_000".toConstraintNumber()).isEqualByComparingTo(BigDecimal("1000"))
		assertThat("1_000.5_00".toConstraintNumber()).isEqualByComparingTo(BigDecimal("1000.500"))
		assertThat("-10_000".toConstraintNumber()).isEqualByComparingTo(BigDecimal("-10000"))
	}
	
	@Test
	@DisplayName("blank and garbage reject")
	fun rejects() {
		assertThat("".toConstraintNumber()).isNull()
		assertThat("   ".toConstraintNumber()).isNull()
		assertThat("___".toConstraintNumber()).isNull()
		assertThat("abc".toConstraintNumber()).isNull()
		assertThat("1.2.3".toConstraintNumber()).isNull()
		assertThat("Infinity".toConstraintNumber()).isNull()
		assertThat("-Infinity".toConstraintNumber()).isNull()
		assertThat("NaN".toConstraintNumber()).isNull()
	}
	
	@Test
	@DisplayName("leading plus sign is accepted")
	fun leadingPlus() {
		assertThat("+18".toConstraintNumber()).isEqualByComparingTo(BigDecimal("18"))
	}
}
