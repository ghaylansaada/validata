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
package io.ghaylan.validata.schema.ref

import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

/**
 * Locks the shared cross-field scalar matrix used by KSP and the IntelliJ plugin.
 *
 * Changing a cell here is an intentional public contract change — update processor / IDE
 * mirrors together.
 *
 * Nested groups organize scenarios by [PropertyRefCompatibilityKind] and shared contract checks.
 * 
 * @author Ghaylan Saada
 */
class PropertyRefScalarCompatibilityTest {
	
	/**
	 * Pairwise and subject-gate rules for COMPARABLE_FAMILY.
	 */
	@Nested
	@DisplayName("Given COMPARABLE_FAMILY")
	inner class ComparableFamily {
		
		@Test
		@DisplayName("COMPARABLE_FAMILY: numeric↔numeric, temporal↔temporal, and same-kind leaves OK; mixes fail")
		fun comparableFamily() {
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"INTEGRAL",
					"DECIMAL",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"DECIMAL",
					"DECIMAL",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"TEMPORAL",
					"TEMPORAL",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"INTEGRAL",
					"TEMPORAL",
				),
			).isFalse()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"STRING",
					"INTEGRAL",
				),
			).isFalse()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"STRING",
					"STRING",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"CHAR",
					"CHAR",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"ENUM",
					"ENUM",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"UUID",
					"UUID",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"STRING",
					"ENUM",
				),
			).isFalse()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"BOOLEAN",
					"BOOLEAN",
				),
			).isFalse()
		}
		
		@ParameterizedTest(name = "COMPARABLE_FAMILY subject {0}")
		@CsvSource(
			"INTEGRAL, true",
			"DECIMAL, true",
			"TEMPORAL, true",
			"CHAR, true",
			"STRING, true",
			"ENUM, true",
			"UUID, true",
			"BOOLEAN, false",
			"OTHER, false",
		)
		fun comparableFamilySubjectGate(
			kind: String,
			expected: Boolean
		) {
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					kind,
				),
			).isEqualTo(expected)
		}
	}
	
	/**
	 * Pairwise and subject-gate rules for SAME_SCALAR_KIND.
	 */
	@Nested
	@DisplayName("Given SAME_SCALAR_KIND")
	inner class SameScalarKind {
		
		@Test
		@DisplayName("SAME_SCALAR_KIND: identical kinds, both numeric, and common leaf pairs")
		fun sameScalarKind() {
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"STRING",
					"STRING",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"INTEGRAL",
					"DECIMAL",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"UUID",
					"UUID",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"ENUM",
					"ENUM",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"BOOLEAN",
					"BOOLEAN",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"STRING",
					"INTEGRAL",
				),
			).isFalse()
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"UUID",
					"STRING",
				),
			).isFalse()
		}
		
		@ParameterizedTest(name = "SAME_SCALAR_KIND subject {0} accepted")
		@EnumSource(value = ScalarKind::class, names = ["OTHER"], mode = EnumSource.Mode.EXCLUDE)
		fun sameScalarKindSubjectAcceptsAllExceptOther(kind: ScalarKind) {
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					kind,
				),
			).isTrue()
		}
		
		@Test
		@DisplayName("SAME_SCALAR_KIND subject gate rejects OTHER")
		fun sameScalarKindSubjectRejectsOther() {
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"OTHER",
				),
			).isFalse()
			assertThat(
				PropertyRefScalarCompatibility.subjectMismatchMessage(
					"equalTo",
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"OTHER",
				),
			).contains("@equalTo").contains("OTHER")
		}
	}
	
	/**
	 * NONE no-op, mismatch copy, typed overloads, OTHER asymmetry, and enum name stability.
	 */
	@Nested
	@DisplayName("Given NONE / messages / contract")
	inner class NoneMessagesAndContract {
		
		@Test
		@DisplayName("OTHER↔OTHER isCompatible true but isSubjectCompatible false for SAME_SCALAR_KIND")
		fun otherAsymmetry() {
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"OTHER",
					"OTHER",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"OTHER",
				),
			).isFalse()
		}
		
		@Test
		@DisplayName("NONE always compatible; subject gate always true; mismatch messages empty")
		fun none() {
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.NONE,
					"STRING",
					"TEMPORAL",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.NONE,
					"STRING",
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.NONE,
					ScalarKind.OTHER,
				),
			).isTrue()
			assertThat(
				PropertyRefScalarCompatibility.mismatchMessage(
					PropertyRefCompatibilityKind.NONE,
					"STRING",
					"TEMPORAL",
				),
			).isEmpty()
			assertThat(
				PropertyRefScalarCompatibility.subjectMismatchMessage(
					"RequiredWhen",
					PropertyRefCompatibilityKind.NONE,
					"STRING",
				),
			).isEmpty()
		}
		
		@Test
		@DisplayName("typed ScalarKind overloads match string-name overloads")
		fun typedOverloadsMatchStringNames() {
			assertThat(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					ScalarKind.INTEGRAL,
					ScalarKind.DECIMAL,
				),
			).isEqualTo(
				PropertyRefScalarCompatibility.isCompatible(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"INTEGRAL",
					"DECIMAL",
				),
			)
			assertThat(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					ScalarKind.TEMPORAL,
				),
			).isEqualTo(
				PropertyRefScalarCompatibility.isSubjectCompatible(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"TEMPORAL",
				),
			)
		}
		
		@Test
		@DisplayName("mismatchMessage describes COMPARABLE_FAMILY and SAME_SCALAR_KIND failures")
		fun mismatchMessages() {
			assertThat(
				PropertyRefScalarCompatibility.mismatchMessage(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					ScalarKind.INTEGRAL,
					ScalarKind.TEMPORAL,
				),
			).isEqualTo(
				PropertyRefScalarCompatibility.mismatchMessage(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"INTEGRAL",
					"TEMPORAL",
				),
			)
			assertThat(
				PropertyRefScalarCompatibility.subjectMismatchMessage(
					"Compare",
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					ScalarKind.STRING,
				),
			).isEqualTo(
				PropertyRefScalarCompatibility.subjectMismatchMessage(
					"Compare",
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"STRING",
				),
			)
			assertThat(
				PropertyRefScalarCompatibility.mismatchMessage(
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"INTEGRAL",
					"TEMPORAL",
				),
			).contains("cannot compare").contains("INTEGRAL").contains("TEMPORAL")
			assertThat(
				PropertyRefScalarCompatibility.mismatchMessage(
					PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
					"STRING",
					"INTEGRAL",
				),
			).contains("cannot equal").contains("STRING").contains("INTEGRAL")
			assertThat(
				PropertyRefScalarCompatibility.subjectMismatchMessage(
					"Compare",
					PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
					"BOOLEAN",
				),
			).contains("@Compare").contains("BOOLEAN").contains("orderable scalar")
		}
	}
}
