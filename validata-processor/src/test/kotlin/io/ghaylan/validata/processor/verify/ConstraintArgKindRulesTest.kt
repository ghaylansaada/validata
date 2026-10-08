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

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure kind-map coverage for [ConstraintArgKindValueRules] / [ConstraintArgKindElementRules] (T4.3).
 * 
 * @author Ghaylan Saada
 */
class ConstraintArgKindRulesTest {
	
	@Nested
	@DisplayName("VALUE kinds")
	inner class ValueKinds {
		
		@Test
		@DisplayName("NOT_BLANK rejects whitespace-only strings")
		fun notBlank() {
			val rule = ConstraintArgKindValueRules.byKind.getValue(ConstraintArgKind.NOT_BLANK)
			assertThat(rule("  ", "min")).contains("must not be blank")
			assertThat(rule("ok", "min")).isNull()
		}
		
		@Test
		@DisplayName("NON_EMPTY rejects empty collections and strings")
		fun nonEmpty() {
			val rule = ConstraintArgKindValueRules.byKind.getValue(ConstraintArgKind.NON_EMPTY)
			assertThat(rule(emptyList<String>(), "values")).contains("must not be empty")
			assertThat(rule(listOf("A"), "values")).isNull()
		}
		
		@Test
		@DisplayName("REGEX rejects invalid patterns")
		fun regex() {
			val rule = ConstraintArgKindValueRules.byKind.getValue(ConstraintArgKind.REGEX)
			assertThat(rule("(", "pattern")).contains("not a valid Java regex")
			assertThat(rule("[a-z]+", "pattern")).isNull()
		}
		
		@Test
		@DisplayName("NON_NEGATIVE rejects negatives")
		fun nonNegative() {
			val rule = ConstraintArgKindValueRules.byKind.getValue(ConstraintArgKind.NON_NEGATIVE)
			assertThat(rule(-1, "min")).contains("must be >= 0")
			assertThat(rule(0, "min")).isNull()
		}
		
		@Test
		@DisplayName("POSITIVE rejects zero and non-numeric strings")
		fun positive() {
			val rule = ConstraintArgKindValueRules.byKind.getValue(ConstraintArgKind.POSITIVE)
			assertThat(rule(0, "factor")).contains("must be > 0")
			assertThat(rule("abc", "factor")).contains("positive decimal")
			assertThat(rule("2.5", "factor")).isNull()
		}
	}
	
	@Nested
	@DisplayName("ELEMENT kinds")
	inner class ElementKinds {
		
		@Test
		@DisplayName("NOT_BLANK rejects blank elements")
		fun notBlankElements() {
			val rule = ConstraintArgKindElementRules.byKind.getValue(ConstraintArgKind.NOT_BLANK)
			assertThat(rule(listOf(" "), listOf(" "), "values", "element")).contains("blank")
		}
		
		@Test
		@DisplayName("NON_NEGATIVE is illegal on ELEMENT")
		fun nonNegativeIllegal() {
			val rule = ConstraintArgKindElementRules.byKind.getValue(ConstraintArgKind.NON_NEGATIVE)
			assertThat(rule(emptyList(), emptyList<Any>(), "min", "element")).contains("not valid with target=ELEMENT")
		}
		
		@Test
		@DisplayName("empty collection does not fail ELEMENT NON_EMPTY")
		fun emptyCollectionElementNonEmpty() {
			val rule = ConstraintArgKindElementRules.byKind.getValue(ConstraintArgKind.NON_EMPTY)
			assertThat(rule(emptyList(), emptyList<String>(), "values", "element")).isNull()
		}
	}
}
