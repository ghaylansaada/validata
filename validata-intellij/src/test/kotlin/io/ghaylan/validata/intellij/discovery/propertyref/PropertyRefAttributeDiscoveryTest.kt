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

package io.ghaylan.validata.intellij.discovery.propertyref

import io.ghaylan.validata.intellij.model.PropertyRefMetadataHost
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [PropertyRefAttributeDiscovery] helpers that do not need a running IDE.
 * 
 * @author Ghaylan Saada
 */
class PropertyRefAttributeDiscoveryTest {
	
	@Test
	@DisplayName("primarySiblingCompatibility ignores NONE and ELEMENT hosts")
	fun primarySiblingCompatibilityIgnoresNoneAndElementHosts() {
		val hosts = listOf(
			PropertyRefMetadataHost("by", PropertyRefScope.ELEMENT, PropertyRefCompatibilityKind.NONE),
			PropertyRefMetadataHost(
				"property",
				PropertyRefScope.SIBLING,
				PropertyRefCompatibilityKind.NONE,
			),
		)
		assertThat(PropertyRefAttributeDiscovery.primarySiblingCompatibility(hosts)).isNull()
	}
	
	@Test
	@DisplayName("primarySiblingCompatibility prefers COMPARABLE_FAMILY")
	fun primarySiblingCompatibilityPrefersComparableFamily() {
		val hosts = listOf(
			PropertyRefMetadataHost(
				"other",
				PropertyRefScope.SIBLING,
				PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
			),
			PropertyRefMetadataHost(
				"min",
				PropertyRefScope.SIBLING,
				PropertyRefCompatibilityKind.COMPARABLE_FAMILY,
			),
		)
		assertThat(PropertyRefAttributeDiscovery.primarySiblingCompatibility(hosts)).isEqualTo(PropertyRefCompatibilityKind.COMPARABLE_FAMILY)
	}
	
	@Test
	@DisplayName("primarySiblingCompatibility returns first non-NONE when no COMPARABLE_FAMILY")
	fun primarySiblingCompatibilityReturnsFirstNonNoneWhenNoComparable() {
		val hosts = listOf(
			PropertyRefMetadataHost(
				"a",
				PropertyRefScope.SIBLING,
				PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
			),
			PropertyRefMetadataHost(
				"b",
				PropertyRefScope.SIBLING,
				PropertyRefCompatibilityKind.SAME_SCALAR_KIND,
			),
		)
		assertThat(PropertyRefAttributeDiscovery.primarySiblingCompatibility(hosts)).isEqualTo(PropertyRefCompatibilityKind.SAME_SCALAR_KIND)
	}
	
	@Test
	@DisplayName("primarySiblingCompatibility is null for empty hosts")
	fun primarySiblingCompatibilityEmptyHostsIsNull() {
		assertThat(PropertyRefAttributeDiscovery.primarySiblingCompatibility(emptyList())).isNull()
	}
}
