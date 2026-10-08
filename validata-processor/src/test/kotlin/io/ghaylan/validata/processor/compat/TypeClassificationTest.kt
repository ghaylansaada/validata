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
package io.ghaylan.validata.processor.compat

import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Unit tests for [TypeClassification] leaf mapping and platform-leaf guards.
 *
 * ## Why this suite exists
 *
 * Generated code embeds `ScalarKind.<NAME>` from these helpers. Drift breaks schema
 * emission and property-ref scalar matrices. Platform-leaf recognition must stay broad enough
 * that the processor never demands `@Validatable` on `String` / `List`.
 * 
 * @author Ghaylan Saada

 */
class TypeClassificationTest {

	@Test
	@DisplayName("maps common Kotlin / Java leaves to ScalarKind")
	fun scalarKinds() {
		assertThat(TypeClassification.scalarKind(TypeNames.STRING_KOTLIN)).isEqualTo(ScalarKind.STRING)
		assertThat(TypeClassification.scalarKind(TypeNames.INT_KOTLIN)).isEqualTo(ScalarKind.INTEGRAL)
		assertThat(TypeClassification.scalarKind(TypeNames.INTEGER_JAVA)).isEqualTo(ScalarKind.INTEGRAL)
		assertThat(TypeClassification.scalarKind(TypeNames.BIG_DECIMAL)).isEqualTo(ScalarKind.DECIMAL)
		assertThat(TypeClassification.scalarKind(TypeNames.INSTANT)).isEqualTo(ScalarKind.TEMPORAL)
		assertThat(TypeClassification.scalarKind(TypeNames.UUID)).isEqualTo(ScalarKind.UUID)
		assertThat(TypeClassification.scalarKind(TypeNames.BOOLEAN_KOTLIN)).isEqualTo(ScalarKind.BOOLEAN)
		assertThat(TypeClassification.scalarKind("com.acme.Color")).isEqualTo(ScalarKind.OTHER)
	}

	@Test
	@DisplayName("isScalar covers java.time.* without listing every type")
	fun temporalIsScalar() {
		assertThat(TypeClassification.isScalar(TypeNames.LOCAL_DATE)).isTrue()
		assertThat(TypeClassification.isScalar("com.acme.User")).isFalse()
	}

	@Test
	@DisplayName("platform leaves include JDK collections and strings")
	fun platformLeaves() {
		assertThat(TypeClassification.isPlatformLeaf(TypeNames.STRING_KOTLIN)).isTrue()
		assertThat(TypeClassification.isPlatformLeaf(TypeNames.STRING_JAVA)).isTrue()
		assertThat(TypeClassification.isPlatformLeaf(TypeNames.LIST_KOTLIN)).isTrue()
		assertThat(TypeClassification.isPlatformLeaf(TypeNames.MAP_JAVA)).isTrue()
		assertThat(TypeClassification.isPlatformLeaf("com.acme.User")).isFalse()
		// Unresolved types must not become cascade targets.
		assertThat(TypeClassification.isPlatformLeaf(null)).isTrue()
		assertThat(TypeClassification.isPlatformLeaf(TypeNames.ANY_KOTLIN)).isFalse()
	}

	@Test
	@DisplayName("isArrayFqcn matches kotlin.Array and primitive arrays only")
	fun arrayFqcn() {
		assertThat(TypeClassification.isArrayFqcn(TypeNames.ARRAY_KOTLIN)).isTrue()
		assertThat(TypeClassification.isArrayFqcn(TypeNames.INT_ARRAY)).isTrue()
		assertThat(TypeClassification.isArrayFqcn(TypeNames.LONG_ARRAY)).isTrue()
		assertThat(TypeClassification.isArrayFqcn("com.acme.PhotoArray")).isFalse()
		assertThat(TypeClassification.isArrayFqcn("java.lang.reflect.Array")).isFalse()
		assertThat(TypeClassification.isArrayFqcn(null)).isFalse()
	}
}
