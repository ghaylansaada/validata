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
package io.ghaylan.validata.samples.constraint

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.groups.OnDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [OddYearsConstraintDocumentation]: support matching and empty native facets.*
 * 
 * @author Ghaylan Saada
 */
class OddYearsConstraintDocumentationTest {
	
	private val docs = OddYearsConstraintDocumentation()
	
	private fun oddYears(): OddYearsConstraint = OddYearsConstraint(message = "", groups = setOf(OnDefault::class))
	
	@Nested
	@DisplayName("supports")
	inner class Supports {
		
		@Test
		@DisplayName("supports OddYearsConstraint")
				/** supports OddYearsConstraint				 */
		fun supportsOddYears() {
			assertThat(docs.supports(oddYears())).isTrue()
		}
		
		@Test
		@DisplayName("does not support unrelated metadata")
				/** does not support unrelated metadata				 */
		fun rejectsOtherMetadata() {
			val other = MinConstraint("1", true, "", setOf(OnDefault::class))
			assertThat(docs.supports(other)).isFalse()
		}
	}
	
	@Nested
	@DisplayName("hints")
	inner class Hints {
		
		@Test
		@DisplayName("returns EMPTY so OddYears is not x-validata-unmapped")
				/** returns EMPTY so OddYears is not x-validata-unmapped				 */
		fun emptyHints() {
			assertThat(docs.hints(oddYears(), null)).isEqualTo(ConstraintDocHints.EMPTY)
		}
	}
}
