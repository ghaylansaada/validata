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
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Locks leaf-package policy so platform types are never traversed as DTOs at runtime.
 *
 * @author Ghaylan Saada
 */
class LeafTypeRegistryTest {

	data class UserDto(val id: String)

	@Test
	@DisplayName("java.util / java.time packages are leaves; user DTOs are not")
	fun packagePolicy() {
		assertThat(LeafTypeRegistry.isLeaf(UUID::class.java)).isTrue()
		assertThat(LeafTypeRegistry.isLeaf(String::class.java)).isTrue()
		assertThat(LeafTypeRegistry.isLeaf(UserDto::class.java)).isFalse()
	}

	@Test
	@DisplayName("register() marks a host type as a leaf")
	fun registerAdditional() {
		class ThirdPartyValue {
			val bits: Long = 0
		}
		assertThat(LeafTypeRegistry.isLeaf(ThirdPartyValue::class.java)).isFalse()
		LeafTypeRegistry.register(ThirdPartyValue::class.java)
		assertThat(LeafTypeRegistry.isLeaf(ThirdPartyValue::class.java)).isTrue()
	}
}
