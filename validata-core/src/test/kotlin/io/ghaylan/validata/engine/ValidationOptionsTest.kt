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
package io.ghaylan.validata.engine

import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [ValidationOptions] equality (array-backed groups).
 * 
 * @author Ghaylan Saada
 */
class ValidationOptionsTest {
	
	@Test
	@DisplayName("equal flags and groups contentEquals")
	fun equalInstances() {
		val a = ValidationOptions(oneErrorPerParam = false, failFast = true, groups = arrayOf(OnDefault::class))
		val b = ValidationOptions(oneErrorPerParam = false, failFast = true, groups = arrayOf(OnDefault::class))
		assertThat(a).isEqualTo(b)
		assertThat(a.hashCode()).isEqualTo(b.hashCode())
	}
	
	@Test
	@DisplayName("different groups content are not equal")
	fun differentGroupsNotEqual() {
		val a = ValidationOptions(groups = arrayOf(OnDefault::class))
		val b = ValidationOptions(groups = arrayOf(OnCreate::class))
		assertThat(a).isNotEqualTo(b)
	}
	
	@Test
	@DisplayName("different failFast flags are not equal")
	fun differentFailFastNotEqual() {
		val a = ValidationOptions(failFast = true)
		val b = ValidationOptions(failFast = false)
		assertThat(a).isNotEqualTo(b)
	}
}
