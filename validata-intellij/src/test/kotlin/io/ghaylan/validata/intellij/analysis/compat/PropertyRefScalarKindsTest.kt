/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.analysis.compat

import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PropertyRefScalarKindsTest {
	
	@Test
	@DisplayName("maps integral and decimal FQCNs")
	fun integralAndDecimal() {
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.Int")).isEqualTo(ScalarKind.INTEGRAL)
		assertThat(PropertyRefScalarKinds.scalarKind("java.lang.Integer")).isEqualTo(ScalarKind.INTEGRAL)
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.Long")).isEqualTo(ScalarKind.INTEGRAL)
		assertThat(PropertyRefScalarKinds.scalarKind("java.math.BigInteger")).isEqualTo(ScalarKind.INTEGRAL)
		assertThat(PropertyRefScalarKinds.scalarKind("java.math.BigDecimal")).isEqualTo(ScalarKind.DECIMAL)
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.Double")).isEqualTo(ScalarKind.DECIMAL)
	}
	
	@Test
	@DisplayName("maps string / boolean / char / uuid FQCNs")
	fun leafScalars() {
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.String")).isEqualTo(ScalarKind.STRING)
		assertThat(PropertyRefScalarKinds.scalarKind("java.lang.String")).isEqualTo(ScalarKind.STRING)
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.Boolean")).isEqualTo(ScalarKind.BOOLEAN)
		assertThat(PropertyRefScalarKinds.scalarKind("java.lang.Boolean")).isEqualTo(ScalarKind.BOOLEAN)
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.Char")).isEqualTo(ScalarKind.CHAR)
		assertThat(PropertyRefScalarKinds.scalarKind("java.util.UUID")).isEqualTo(ScalarKind.UUID)
	}
	
	@Test
	@DisplayName("maps temporal FQCNs")
	fun temporal() {
		assertThat(PropertyRefScalarKinds.scalarKind("java.time.LocalDate")).isEqualTo(ScalarKind.TEMPORAL)
		assertThat(PropertyRefScalarKinds.scalarKind("java.time.Instant")).isEqualTo(ScalarKind.TEMPORAL)
		assertThat(PropertyRefScalarKinds.scalarKind("java.util.Date")).isEqualTo(ScalarKind.TEMPORAL)
		assertThat(PropertyRefScalarKinds.scalarKind("java.util.Calendar")).isEqualTo(ScalarKind.TEMPORAL)
	}
	
	@Test
	@DisplayName("unknown leaves are OTHER")
	fun other() {
		assertThat(PropertyRefScalarKinds.scalarKind("com.example.UserDto")).isEqualTo(ScalarKind.OTHER)
		assertThat(PropertyRefScalarKinds.scalarKind("kotlin.collections.List")).isEqualTo(ScalarKind.OTHER)
	}
}
