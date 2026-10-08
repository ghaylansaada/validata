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
package io.ghaylan.validata.ext

import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [MethodUniqueIdentifiers].
 * 
 * @author Ghaylan Saada
 */
class MethodUniqueIdentifiersTest {
	
	@Suppress("unused")
	class Sample {
		
		fun create(name: String) {}
		fun create(
			name: String,
			age: Int
		) {
		}
		
		fun nested(box: NestedType) {}
	}
	
	class NestedType
	
	@Suppress("unused")
	class SuspendSample {
		
		suspend fun susp(name: String) {}
	}
	
	@Nested
	@DisplayName("format")
	inner class Format {
		
		@Test
		@DisplayName("identifier uses Class#method(paramTypeName) with FQCN types")
		fun basicFormat() {
			val method = Sample::class.java.getDeclaredMethod("create", String::class.java)
			assertThat(method.getUniqueIdentifier()).isEqualTo(
				"${Sample::class.java.name}#create(${String::class.java.typeName})",
			)
		}
		
		@Test
		@DisplayName("overloads with different arity produce distinct identifiers")
		fun overloadsDistinct() {
			val one = Sample::class.java.getDeclaredMethod("create", String::class.java)
			val two = Sample::class.java.getDeclaredMethod("create", String::class.java, Int::class.javaPrimitiveType)
			assertThat(one.getUniqueIdentifier()).isNotEqualTo(two.getUniqueIdentifier())
		}
		
		@Test
		@DisplayName($$"nested class parameters use JVM binary names (Outer$Inner)")
		fun nestedJvmName() {
			val method = Sample::class.java.getDeclaredMethod("nested", NestedType::class.java)
			assertThat(method.getUniqueIdentifier()).contains(NestedType::class.java.typeName)
			assertThat(method.getUniqueIdentifier()).contains("$")
		}
		
		@Test
		@DisplayName("suspend method omits Continuation from the identifier")
		fun suspendOmitsContinuation() {
			val methods = SuspendSample::class.java.declaredMethods.filter { it.name == "susp" }
			assertThat(methods).isNotEmpty
			val method = methods.single()
			assertThat(method.parameterTypes.map { it.name }).contains("kotlin.coroutines.Continuation")
			val id = method.getUniqueIdentifier()
			assertThat(id).doesNotContain("Continuation")
			assertThat(id).isEqualTo(
				"${SuspendSample::class.java.name}#susp(${String::class.java.typeName})",
			)
		}
	}
	
	@Nested
	@DisplayName("caching")
	inner class Caching {
		
		@Test
		@DisplayName("same Method signature returns the identical cached string instance")
		fun cachesByMethod() {
			val method = Sample::class.java.getDeclaredMethod("create", String::class.java)
			assertThat(method.getUniqueIdentifier()).isSameAs(method.getUniqueIdentifier())
			val again = Sample::class.java.getDeclaredMethod("create", String::class.java)
			assertThat(again.getUniqueIdentifier()).isSameAs(method.getUniqueIdentifier())
		}
	}
}
