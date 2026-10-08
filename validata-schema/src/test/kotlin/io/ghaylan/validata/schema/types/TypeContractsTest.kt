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

import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Locks the shared type / literal contracts so KSP, IntelliJ, and runtime cannot drift.
 *
 * @author Ghaylan Saada
 */
class TypeContractsTest {

	@Nested
	@DisplayName("ScalarKinds")
	inner class ScalarKindsNested {

		@Test
		@DisplayName("maps numeric / string / UUID leaves")
		fun mapsKnownLeaves() {
			assertThat(ScalarKinds.of(TypeNames.INT_KOTLIN)).isEqualTo(ScalarKind.INTEGRAL)
			assertThat(ScalarKinds.of(TypeNames.INTEGER_JAVA)).isEqualTo(ScalarKind.INTEGRAL)
			assertThat(ScalarKinds.of(TypeNames.BIG_DECIMAL)).isEqualTo(ScalarKind.DECIMAL)
			assertThat(ScalarKinds.of(TypeNames.STRING_KOTLIN)).isEqualTo(ScalarKind.STRING)
			assertThat(ScalarKinds.of(TypeNames.UUID)).isEqualTo(ScalarKind.UUID)
			assertThat(ScalarKinds.of(TypeNames.BOOLEAN_JAVA)).isEqualTo(ScalarKind.BOOLEAN)
		}

		@Test
		@DisplayName("treats java.time, Date, and Calendar as TEMPORAL")
		fun temporals() {
			assertThat(ScalarKinds.of(TypeNames.LOCAL_DATE)).isEqualTo(ScalarKind.TEMPORAL)
			assertThat(ScalarKinds.of(TypeNames.DURATION)).isEqualTo(ScalarKind.TEMPORAL)
			assertThat(ScalarKinds.of(TypeNames.MONTH)).isEqualTo(ScalarKind.TEMPORAL)
			assertThat(ScalarKinds.of(TypeNames.DATE)).isEqualTo(ScalarKind.TEMPORAL)
			assertThat(ScalarKinds.of(TypeNames.CALENDAR)).isEqualTo(ScalarKind.TEMPORAL)
		}

		@Test
		@DisplayName("isEnum overrides FQCN")
		fun enumOverride() {
			assertThat(ScalarKinds.of("com.example.Role", isEnum = true)).isEqualTo(ScalarKind.ENUM)
		}

		@Test
		@DisplayName("platform leaf excludes kotlin.Any")
		fun platformLeaf() {
			assertThat(ScalarKinds.isPlatformLeaf(null)).isTrue()
			assertThat(ScalarKinds.isPlatformLeaf(TypeNames.STRING_KOTLIN)).isTrue()
			assertThat(ScalarKinds.isPlatformLeaf(TypeNames.ANY_KOTLIN)).isFalse()
			assertThat(ScalarKinds.isPlatformLeaf("com.acme.User")).isFalse()
		}
	}

	@Nested
	@DisplayName("KnownTypes")
	inner class KnownTypesNested {

		@Test
		@DisplayName("canonicalizes java.lang boxed names")
		fun canonicalize() {
			assertThat(KnownTypes.canonicalize(TypeNames.INTEGER_JAVA)).isEqualTo(TypeNames.INT_KOTLIN)
			assertThat(KnownTypes.canonicalize(TypeNames.STRING_JAVA)).isEqualTo(TypeNames.STRING_KOTLIN)
			assertThat(KnownTypes.isNumeric(TypeNames.INTEGER_JAVA)).isTrue()
			assertThat(KnownTypes.isCharSequenceLike(TypeNames.CHAR_SEQUENCE_JAVA)).isTrue()
		}

		@Test
		@DisplayName("array / map / collection FQCN helpers")
		fun shapeHelpers() {
			assertThat(KnownTypes.isArrayFqcn(TypeNames.INT_ARRAY)).isTrue()
			assertThat(KnownTypes.isArrayFqcn("com.acme.PhotoArray")).isFalse()
			assertThat(KnownTypes.isMapFqcn(TypeNames.MAP_KOTLIN)).isTrue()
			assertThat(KnownTypes.isCollectionFqcn(TypeNames.LIST_JAVA)).isTrue()
			assertThat(KnownTypes.primitiveArrayElementFqcn(TypeNames.INT_ARRAY))
				.isEqualTo(TypeNames.INT_KOTLIN)
		}

		@Test
		@DisplayName("temporal literal hosts cover Month / Duration / Period")
		fun temporalLiteralHosts() {
			assertThat(KnownTypes.isTemporalLiteralHost(TypeNames.MONTH)).isTrue()
			assertThat(KnownTypes.isTemporalLiteralHost(TypeNames.DURATION)).isTrue()
			assertThat(KnownTypes.isTemporalLiteralHost(TypeNames.PERIOD)).isTrue()
			assertThat(KnownTypes.isTemporalLiteralHost(TypeNames.STRING_KOTLIN)).isFalse()
		}

		@Test
		@DisplayName("builtinSupertypes covers String / numeric / temporal / collections")
		fun builtinSupertypes() {
			assertThat(KnownTypes.builtinSupertypes(TypeNames.STRING_KOTLIN))
				.contains(TypeNames.CHAR_SEQUENCE_KOTLIN, TypeNames.COMPARABLE_KOTLIN)
			assertThat(KnownTypes.builtinSupertypes(TypeNames.INT_KOTLIN))
				.contains(TypeNames.NUMBER_KOTLIN, TypeNames.COMPARABLE_KOTLIN)
			assertThat(KnownTypes.builtinSupertypes(TypeNames.LOCAL_DATE))
				.contains(TypeNames.TEMPORAL, TypeNames.COMPARABLE_KOTLIN)
			assertThat(KnownTypes.builtinSupertypes(TypeNames.LIST_KOTLIN))
				.contains(TypeNames.COLLECTION_KOTLIN, TypeNames.ITERABLE_KOTLIN)
			assertThat(KnownTypes.builtinSupertypes("com.acme.User")).isEmpty()
		}

		@Test
		@DisplayName("isTemporal ranking is narrower than temporal literal hosts")
		fun temporalRankingVsLiteralHosts() {
			assertThat(KnownTypes.isTemporal(TypeNames.LOCAL_DATE)).isTrue()
			assertThat(KnownTypes.isTemporal(TypeNames.TEMPORAL)).isTrue()
			// Literal hosts that are NOT Temporal-assignable peers for validator ranking.
			assertThat(KnownTypes.isTemporal(TypeNames.MONTH)).isFalse()
			assertThat(KnownTypes.isTemporal(TypeNames.DURATION)).isFalse()
			assertThat(KnownTypes.isTemporal(TypeNames.PERIOD)).isFalse()
			assertThat(KnownTypes.isTemporalLiteralHost(TypeNames.MONTH)).isTrue()
			assertThat(KnownTypes.isTemporalLiteralHost(TypeNames.DURATION)).isTrue()
		}

		@Test
		@DisplayName("jvmErasedName distinguishes boxed vs primitive and IntArray")
		fun jvmErasedName() {
			assertThat(KnownTypes.jvmErasedName(TypeNames.INT_KOTLIN, notNull = false))
				.isEqualTo(TypeNames.INTEGER_JAVA)
			assertThat(KnownTypes.jvmErasedName(TypeNames.INT_KOTLIN, notNull = true)).isEqualTo("int")
			assertThat(KnownTypes.jvmErasedName(TypeNames.INT_ARRAY, notNull = true)).isEqualTo("int[]")
			assertThat(KnownTypes.jvmErasedName("com.acme.User", notNull = true)).isNull()
		}

		@Test
		@DisplayName("primitiveOrBoxedMatch covers full PRIMITIVE_TO_BOXED table both ways")
		fun allPrimitiveBoxedPairs() {
			for ((prim, boxed) in TypeTables.PRIMITIVE_TO_BOXED) {
				assertThat(KnownTypes.primitiveOrBoxedMatch(prim, boxed))
					.withFailMessage { "expected match $prim ↔ $boxed" }
					.isTrue()
				assertThat(KnownTypes.primitiveOrBoxedMatch(boxed, prim)).isTrue()
			}
			assertThat(KnownTypes.primitiveOrBoxedMatch(TypeNames.INT_KOTLIN, TypeNames.LONG_KOTLIN)).isFalse()
		}

		@Test
		@DisplayName("catalog integrity: arrays, supers, leaves stay closed under TypeNames")
		fun catalogIntegrity() {
			assertThat(TypeTables.PRIMITIVE_ARRAY_ELEMENT.keys)
				.containsExactlyInAnyOrderElementsOf(
					listOf(
						TypeNames.INT_ARRAY,
						TypeNames.LONG_ARRAY,
						TypeNames.SHORT_ARRAY,
						TypeNames.BYTE_ARRAY,
						TypeNames.FLOAT_ARRAY,
						TypeNames.DOUBLE_ARRAY,
						TypeNames.BOOLEAN_ARRAY,
						TypeNames.CHAR_ARRAY,
					),
				)
			assertThat(TypeTables.TEMPORAL_IMPLEMENTORS).allMatch {
				KnownTypes.builtinSupertypes(it).contains(TypeNames.TEMPORAL)
			}
			// Month / Duration / Period are literal hosts but must NOT soft-claim Temporal.
			assertThat(KnownTypes.builtinSupertypes(TypeNames.MONTH)).doesNotContain(TypeNames.TEMPORAL)
			assertThat(KnownTypes.builtinSupertypes(TypeNames.DURATION)).doesNotContain(TypeNames.TEMPORAL)
			assertThat(KnownTypes.builtinSupertypes(TypeNames.PERIOD)).doesNotContain(TypeNames.TEMPORAL)
			assertThat(KnownTypes.builtinSupertypes(TypeNames.MONTH)).contains(TypeNames.COMPARABLE_KOTLIN)
			assertThat(TypeTables.BUILTIN_ASSIGNABLE_SUPERTYPES.keys)
				.allMatch { it.startsWith(TypeNames.KOTLIN_PACKAGE_PREFIX) || it.startsWith(TypeNames.JAVA_PACKAGE_PREFIX) }
			assertThat(TypeTables.SCALAR_LEAF_FQCNS).allMatch { ScalarKinds.isScalarLeaf(it) }
			assertThat(KnownTypes.isArrayFqcn("com.acme.PhotoArray")).isFalse()
			assertThat(KnownTypes.isArrayFqcn(TypeNames.ARRAY_KOTLIN)).isTrue()
		}
	}

	@Nested
	@DisplayName("ConstraintLiteralRules")
	inner class LiteralRulesNested {

		@Test
		@DisplayName("number literals accept underscores and reject junk")
		fun numbers() {
			assertThat(ConstraintLiteralRules.isValidNumber("18")).isTrue()
			assertThat(ConstraintLiteralRules.isValidNumber("1_000")).isTrue()
			assertThat(ConstraintLiteralRules.parseNumber("1_000")).isEqualByComparingTo("1000")
			assertThat(ConstraintLiteralRules.isValidNumber("")).isFalse()
			assertThat(ConstraintLiteralRules.isValidNumber("1.2.3")).isFalse()
		}

		@ParameterizedTest
		@ValueSource(strings = ["JANUARY", "1", "12"])
		@DisplayName("Month literals accept name and ordinal")
		fun monthOk(raw: String) {
			assertThat(ConstraintLiteralRules.isValidTemporal(raw, TypeNames.MONTH)).isTrue()
			assertThat(ConstraintLiteralRules.parseMonth(raw)).isNotNull()
		}

		@Test
		@DisplayName("Month rejects out of range ordinal")
		fun monthBad() {
			assertThat(ConstraintLiteralRules.isValidTemporal("13", TypeNames.MONTH)).isFalse()
			assertThat(ConstraintLiteralRules.parseMonth("13")).isNull()
		}

		@Test
		@DisplayName("temporal parse matches declared type")
		fun temporals() {
			assertThat(ConstraintLiteralRules.isValidTemporal("2020-01-01", TypeNames.LOCAL_DATE)).isTrue()
			assertThat(ConstraintLiteralRules.isValidTemporal("2020-01-01", TypeNames.INSTANT)).isFalse()
			assertThat(ConstraintLiteralRules.isValidTemporal("PT1H", TypeNames.DURATION)).isTrue()
			assertThat(ConstraintLiteralRules.isValidTemporal("P1Y", TypeNames.PERIOD)).isTrue()
		}

		@Test
		@DisplayName("checkTypedLiteral covers blank / number / enum")
		fun typedLiteral() {
			assertThat(ConstraintLiteralRules.checkTypedLiteral("", TypeNames.STRING_KOTLIN))
				.isEqualTo(TypedLiteralResult.Ok)
			assertThat(ConstraintLiteralRules.checkTypedLiteral("  ", TypeNames.INT_KOTLIN))
				.isEqualTo(TypedLiteralResult.BlankNotAllowed)
			assertThat(ConstraintLiteralRules.checkTypedLiteral("nope", TypeNames.INT_KOTLIN))
				.isEqualTo(TypedLiteralResult.InvalidNumber)
			assertThat(
				ConstraintLiteralRules.checkTypedLiteral("ADMIN", "test.Role", setOf("ADMIN")),
			).isEqualTo(TypedLiteralResult.Ok)
			assertThat(
				ConstraintLiteralRules.checkTypedLiteral("NOPE", "test.Role", setOf("ADMIN")),
			).isInstanceOf(TypedLiteralResult.UnknownEnumConstant::class.java)
		}
	}

	@Nested
	@DisplayName("BuiltinTypeShortNames")
	inner class ShortNamesNested {

		@Test
		@DisplayName("short names resolve to TypeNames constants")
		fun alignedWithTypeNames() {
			assertThat(BuiltinTypeShortNames.ALL["Int"]).isEqualTo(TypeNames.INT_KOTLIN)
			assertThat(BuiltinTypeShortNames.SCALAR_LEAVES["Calendar"]).isEqualTo(TypeNames.CALENDAR)
			assertThat(BuiltinTypeShortNames.SCALAR_LEAVES.keys)
				.allMatch { it in BuiltinTypeShortNames.ALL }
			assertThat(BuiltinTypeShortNames.COLLECTION_OR_ARRAY_SHORT_NAMES)
				.contains("List", "Array", "ArrayList")
		}
	}
}
