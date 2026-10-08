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

import io.ghaylan.validata.processor.naming.EndpointIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames

/**
 * JVM type-name contract used by [EndpointIdentifier] for array parameters.
 *
 * Full KSP round-trips live in endpoint integration tests; this suite locks
 * `Array<T>` vs primitive-array encoding against [KnownTypes.jvmErasedName] /
 * [Class.getTypeName].
 * 
 * @author Ghaylan Saada

 */
class KnownTypesArrayEncodingTest {

	@Test
	@DisplayName("Array<Int> must erase to Integer[] (boxed), not int[]")
	fun arrayOfIntIsBoxed() {
		// EndpointIdentifier forces notNull=false for kotlin.Array components.
		assertThat(KnownTypes.jvmErasedName(TypeNames.INT_KOTLIN, notNull = false))
			.isEqualTo(TypeNames.INTEGER_JAVA)
		assertThat(KnownTypes.jvmErasedName(TypeNames.INT_KOTLIN, notNull = true))
			.isEqualTo("int")
		assertThat(Array<Int>::class.java.typeName).isEqualTo("${TypeNames.INTEGER_JAVA}[]")
	}

	@Test
	@DisplayName("IntArray erases to int[] matching Class.getTypeName")
	fun intArrayIsPrimitive() {
		assertThat(KnownTypes.jvmErasedName(TypeNames.INT_ARRAY, notNull = true))
			.isEqualTo("int[]")
		assertThat(IntArray::class.java.typeName).isEqualTo("int[]")
	}

	@Test
	@DisplayName("user types ending in Array are not treated as arrays by FQCN helper")
	fun photoArrayNotAnArray() {
		assertThat(TypeClassification.isArrayFqcn("com.acme.PhotoArray")).isFalse()
	}

	@Test
	@DisplayName("CONTINUATION_KOTLIN constant matches kotlinx Continuation FQCN")
	fun continuationFqcnLocked() {
		assertThat(TypeNames.CONTINUATION_KOTLIN)
			.isEqualTo("kotlin.coroutines.Continuation")
		// EndpointIdentifier.of drops this synthetic param for suspend handlers.
		assertThat(EndpointIdentifier::class.java.name).contains("EndpointIdentifier")
	}
}
