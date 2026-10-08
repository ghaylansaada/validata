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
package io.ghaylan.validata.constraint.spi

import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Built-in [ConstraintCatalog] SPI must load closed-world from this module's KSP output.
 * 
 * @author Ghaylan Saada
 */
class GeneratedConstraintCatalogsTest {
	
	@Test
	@DisplayName("built-in catalog includes Required → RequiredValidator")
	fun loadsRequiredBinding() {
		GeneratedConstraintCatalogs.resetForTests()
		val entries = GeneratedConstraintCatalogs.all()
		assertThat(entries).isNotEmpty
		val required = entries.filter { it.metadataType == RequiredConstraint::class.java }
		assertThat(required).isNotEmpty
		assertThat(required.map { it.validatorType }).contains(RequiredValidator::class.java)
		assertThat(required.map { it.defaultInstanceFactory() }).contains(RequiredValidator)
	}
	
	@Test
	@DisplayName("all() is cached until resetForTests")
	fun caching() {
		GeneratedConstraintCatalogs.resetForTests()
		val first = GeneratedConstraintCatalogs.all()
		val second = GeneratedConstraintCatalogs.all()
		assertThat(second).isSameAs(first)
	}
}
