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

package io.ghaylan.validata.intellij.contract

import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks schema `PropertyRefCompatibilityKind` / `PropertyRefScope` entry names used by discovery.
 *
 * Discovery matches on **enum entry names** read from the user classpath — renaming breaks
 * both KSP and this plugin.
 * 
 * @author Ghaylan Saada
 */
class PropertyRefContractTest {
	
	@Test
	@DisplayName("PropertyRefCompatibilityKind entry names stay stable for discovery")
	fun compatibilityKindEntryNamesAreStable() {
		assertThat(PropertyRefCompatibilityKind.entries.map { it.name }).containsExactly(
			"NONE",
			"SAME_SCALAR_KIND",
			"COMPARABLE_FAMILY",
		)
	}
	
	@Test
	@DisplayName("PropertyRefScope entry names stay stable for discovery")
	fun hostScopeEntryNamesAreStable() {
		assertThat(PropertyRefScope.entries.map { it.name }).containsExactly(
			"SIBLING",
			"ELEMENT",
		)
	}
}
