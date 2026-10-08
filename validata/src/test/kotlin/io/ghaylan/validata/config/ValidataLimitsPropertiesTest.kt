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
package io.ghaylan.validata.config

import io.ghaylan.validata.engine.ValidationLimits
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [ValidataLimitsProperties.toLimits].
 * 
 * @author Ghaylan Saada
 */
class ValidataLimitsPropertiesTest {
	
	@Test
	@DisplayName("copies bound values onto ValidationLimits")
	fun copiesValues() {
		val props = ValidataLimitsProperties(
			maxDepth = 7,
			maxElementsPerContainer = 11,
			maxErrors = 13,
		)
		assertThat(props.toLimits()).isEqualTo(ValidationLimits(maxDepth = 7, maxElementsPerContainer = 11, maxErrors = 13))
	}
	
	@Test
	@DisplayName("rejects non-positive values via ValidationLimits init")
	fun rejectsNonPositive() {
		assertThatThrownBy {
			ValidataLimitsProperties(maxDepth = 0).toLimits()
		}.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("max-depth")
	}
	
	@Test
	@DisplayName("defaults match ValidationLimits companion defaults")
	fun defaultsMatchLimits() {
		assertThat(ValidataLimitsProperties().toLimits()).isEqualTo(ValidationLimits())
	}
}
