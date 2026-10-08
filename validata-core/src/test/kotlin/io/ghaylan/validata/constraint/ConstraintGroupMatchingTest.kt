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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.support.TestValidationContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Direct unit tests for [ConstraintGroupMatching].
 * 
 * @author Ghaylan Saada
 */
class ConstraintGroupMatchingTest {
	
	@Nested
	@DisplayName("shouldRun")
	inner class ShouldRun {
		
		@Test
		@DisplayName("skipGroupChecks always runs")
		fun skipGroupChecks() {
			val ctx = TestValidationContext(groups = emptySet(), skipGroupChecks = true)
			assertThat(ConstraintGroupMatching.shouldRun(setOf(OnCreate::class), ctx)).isTrue()
		}
		
		@Test
		@DisplayName("empty constraint groups always run")
		fun emptyConstraintGroups() {
			val ctx = TestValidationContext(groups = setOf(OnDefault::class))
			assertThat(ConstraintGroupMatching.shouldRun(emptySet(), ctx)).isTrue()
		}
		
		@Test
		@DisplayName("empty context groups never match a non-empty constraint set")
		fun emptyContextGroups() {
			val ctx = TestValidationContext(groups = emptySet())
			assertThat(ConstraintGroupMatching.shouldRun(setOf(OnDefault::class), ctx)).isFalse()
		}
		
		@Test
		@DisplayName("intersecting groups run")
		fun intersecting() {
			val ctx = TestValidationContext(groups = setOf(OnDefault::class, OnCreate::class))
			assertThat(ConstraintGroupMatching.shouldRun(setOf(OnCreate::class), ctx)).isTrue()
		}
		
		@Test
		@DisplayName("non-intersecting groups skip")
		fun nonIntersecting() {
			val ctx = TestValidationContext(groups = setOf(OnDefault::class))
			assertThat(ConstraintGroupMatching.shouldRun(setOf(OnCreate::class), ctx)).isFalse()
		}
	}
}
