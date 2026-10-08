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

import io.ghaylan.validata.schema.Validatable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

/**
 * T-26 — structure and leaf kind must not share one enum axis.
 * 
 * @author Ghaylan Saada
 */
class TypeStructureTest {
	
	@Validatable
	data class Address(val city: String?)
	
	@Test
	@DisplayName("List<String> is ARRAY whose element is SCALAR STRING — not a STRING_ARRAY kind")
	fun listOfStringSeparatesShapeAndLeaf() {
		val info = ReflectionUtils.infoFromType(object: Any() {
			@Suppress("unused")
			val field: List<String>? = null
		}.javaClass.getDeclaredField("field").genericType)
		
		assertThat(info.structure).isEqualTo(TypeStructure.ARRAY)
		assertThat(info.scalarKind).isNull()
		assertThat(info.isArrayOfScalars).isTrue()
		assertThat(info.typeArguments.single().structure).isEqualTo(TypeStructure.SCALAR)
		assertThat(info.typeArguments.single().scalarKind).isEqualTo("STRING")
	}
	
	@Test
	@DisplayName("Map is MAP regardless of value leaf kind")
	fun mapIsAlwaysMapStructure() {
		val info = ReflectionUtils.infoFromType(object: Any() {
			@Suppress("unused")
			val field: Map<String, Int>? = null
		}.javaClass.getDeclaredField("field").genericType)
		
		assertThat(info.structure).isEqualTo(TypeStructure.MAP)
		assertThat(info.isMap).isTrue()
		assertThat(info.isArray).isFalse()
		assertThat(info.isObject).isFalse()
		assertThat(info.typeArguments[0].scalarKind).isEqualTo("STRING")
		assertThat(info.typeArguments[1].scalarKind).isEqualTo("INTEGRAL")
	}
	
	@Test
	@DisplayName("List of DTOs is ARRAY of OBJECT — element kind lives on typeArguments")
	fun listOfObjects() {
		val info = ReflectionUtils.infoFromType(object: Any() {
			@Suppress("unused")
			val field: List<Address>? = null
		}.javaClass.getDeclaredField("field").genericType)
		
		assertThat(info.structure).isEqualTo(TypeStructure.ARRAY)
		assertThat(info.isArrayOfObjects).isTrue()
		assertThat(info.isArrayOfNonScalar).isTrue()
		assertThat(info.typeArguments.single().structure).isEqualTo(TypeStructure.OBJECT)
	}
	
	@Test
	@DisplayName("UUID and platform types are leaf scalars, not traversable objects")
	fun uuidIsScalar() {
		val info = ReflectionUtils.infoFromClass(UUID::class.java)
		assertThat(info.structure).isEqualTo(TypeStructure.SCALAR)
		assertThat(info.scalarKind).isEqualTo("UUID")
		assertThat(info.isObject).isFalse()
		assertThat(ReflectionUtils.isLeafType(UUID::class.java)).isTrue()
	}
	
	@Test
	@DisplayName("registerLeafType marks third-party wrappers as terminal")
	fun registerLeafType() {
		class Money
		assertThat(ReflectionUtils.isLeafType(Money::class.java)).isFalse()
		ReflectionUtils.registerLeafType(Money::class.java)
		assertThat(ReflectionUtils.isLeafType(Money::class.java)).isTrue()
	}
}
