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
package io.ghaylan.validata.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.util.Calendar
import java.util.UUID

/**
 * Structural / scalar-kind classification owned by [StructureClassifier].
 *
 * Guards production failure modes: platform leaves must not become OBJECT (schema walk /
 * InaccessibleObjectException), arrays must stay ARRAY, Month must stay TEMPORAL not ENUM.
 *
 * @author Ghaylan Saada
 */
class StructureClassifierTest {

	data class SampleDto(val name: String?)

	enum class Color { RED, GREEN }

	interface Marker

	abstract class AbstractHolder {
		abstract val id: String
	}

	@Nested
	@DisplayName("determineStructure")
	inner class DetermineStructure {

		@Test
		@DisplayName("Map / List / String / data class / Nothing / Void")
		fun knownShapes() {
			assertThat(StructureClassifier.determineStructure(Map::class)).isEqualTo(TypeStructure.MAP)
			assertThat(StructureClassifier.determineStructure(List::class)).isEqualTo(TypeStructure.ARRAY)
			assertThat(StructureClassifier.determineStructure(String::class)).isEqualTo(TypeStructure.SCALAR)
			assertThat(StructureClassifier.determineStructure(SampleDto::class)).isEqualTo(TypeStructure.OBJECT)
			assertThat(StructureClassifier.determineStructure(Nothing::class)).isEqualTo(TypeStructure.NOTHING)
			assertThat(StructureClassifier.determineStructure(Void::class)).isEqualTo(TypeStructure.NOTHING)
		}

		@Test
		@DisplayName("primitive arrays and kotlin.Array are ARRAY (not OBJECT)")
		fun arraysAreArray() {
			assertThat(StructureClassifier.determineStructure(IntArray::class)).isEqualTo(TypeStructure.ARRAY)
			assertThat(StructureClassifier.determineStructure(ByteArray::class)).isEqualTo(TypeStructure.ARRAY)
			assertThat(StructureClassifier.determineStructure(Array<String>::class)).isEqualTo(TypeStructure.ARRAY)
		}

		@Test
		@DisplayName("platform leaves stay SCALAR (never OBJECT)")
		fun platformLeavesAreScalar() {
			assertThat(StructureClassifier.determineStructure(UUID::class)).isEqualTo(TypeStructure.SCALAR)
			assertThat(StructureClassifier.determineStructure(LocalDate::class)).isEqualTo(TypeStructure.SCALAR)
			assertThat(StructureClassifier.determineStructure(BigDecimal::class)).isEqualTo(TypeStructure.SCALAR)
			assertThat(StructureClassifier.determineStructure(BigInteger::class)).isEqualTo(TypeStructure.SCALAR)
		}
	}

	@Nested
	@DisplayName("isKnownScalar")
	inner class KnownScalar {

		@Test
		@DisplayName("true for enum, UUID, Boolean, boxed Integer, and temporal leaves")
		fun enumUuidBooleanTemporal() {
			assertThat(StructureClassifier.isKnownScalar(Color::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(UUID::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(Boolean::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(Integer::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(LocalDate::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(Month::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(Duration::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(Period::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(Calendar::class)).isTrue()
			assertThat(StructureClassifier.isKnownScalar(SampleDto::class)).isFalse()
			assertThat(StructureClassifier.isKnownScalar(List::class)).isFalse()
		}
	}

	@Nested
	@DisplayName("determineScalarKind")
	inner class ScalarKind {

		@Test
		@DisplayName("maps concrete types; Month is TEMPORAL not ENUM")
		fun kindCodes() {
			assertThat(StructureClassifier.determineScalarKind(String::class)).isEqualTo("STRING")
			assertThat(StructureClassifier.determineScalarKind(Int::class)).isEqualTo("INTEGRAL")
			assertThat(StructureClassifier.determineScalarKind(Integer::class)).isEqualTo("INTEGRAL")
			assertThat(StructureClassifier.determineScalarKind(BigDecimal::class)).isEqualTo("DECIMAL")
			assertThat(StructureClassifier.determineScalarKind(LocalDate::class)).isEqualTo("TEMPORAL")
			assertThat(StructureClassifier.determineScalarKind(Month::class)).isEqualTo("TEMPORAL")
			assertThat(StructureClassifier.determineScalarKind(Duration::class)).isEqualTo("TEMPORAL")
			assertThat(StructureClassifier.determineScalarKind(Calendar::class)).isEqualTo("TEMPORAL")
			assertThat(StructureClassifier.determineScalarKind(Color::class)).isEqualTo("ENUM")
			assertThat(StructureClassifier.determineScalarKind(Boolean::class)).isEqualTo("BOOLEAN")
			assertThat(StructureClassifier.determineScalarKind(UUID::class)).isEqualTo("UUID")
		}
	}

	@Nested
	@DisplayName("primitiveOrBoxedMatch")
	inner class PrimitiveBoxed {

		@Test
		@DisplayName("matches Int↔Integer, Char↔Character; rejects cross-family pairs")
		fun boxedPairs() {
			assertThat(StructureClassifier.primitiveOrBoxedMatch(Int::class, Integer::class)).isTrue()
			assertThat(StructureClassifier.primitiveOrBoxedMatch(Boolean::class, java.lang.Boolean::class)).isTrue()
			assertThat(StructureClassifier.primitiveOrBoxedMatch(Char::class, Character::class)).isTrue()
			assertThat(StructureClassifier.primitiveOrBoxedMatch(Int::class, Double::class)).isFalse()
			assertThat(StructureClassifier.primitiveOrBoxedMatch(String::class, String::class)).isTrue()
		}
	}

	@Nested
	@DisplayName("isObjectLike")
	inner class ObjectLike {

		@Test
		@DisplayName("accepts data class; rejects abstract / interface / enum / platform leaves")
		fun acceptAndReject() {
			assertThat(StructureClassifier.isObjectLike(SampleDto::class.java)).isTrue()
			assertThat(StructureClassifier.isObjectLike(AbstractHolder::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(Marker::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(Color::class.java)).isFalse()
			// Production guard: UUID has fields but must never be walked as a DTO.
			assertThat(StructureClassifier.isObjectLike(UUID::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(String::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(List::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(Map::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(LocalDate::class.java)).isFalse()
			assertThat(StructureClassifier.isObjectLike(Int::class.javaPrimitiveType!!)).isFalse()
		}
	}
}
