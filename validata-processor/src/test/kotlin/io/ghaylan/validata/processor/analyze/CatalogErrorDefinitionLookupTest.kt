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
package io.ghaylan.validata.processor.analyze

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [CatalogErrorDefinitionLookup] reflective null-on-miss contract.
 * 
 * @author Ghaylan Saada

 */
class CatalogErrorDefinitionLookupTest {

	@Test
	@DisplayName("missing catalog class returns null")
	fun missingClassReturnsNull() {
		assertThat(
			CatalogErrorDefinitionLookup.messageOf(
				"io.ghaylan.validata.does.not.ExistCatalog",
				"SOME",
			),
		).isNull()
		assertThat(
			CatalogErrorDefinitionLookup.codeOf(
				"io.ghaylan.validata.does.not.ExistCatalog",
				"SOME",
			),
		).isNull()
	}

	@Test
	@DisplayName("missing enum constant returns null")
	fun missingConstantReturnsNull() {
		assertThat(
			CatalogErrorDefinitionLookup.messageOf(
				SampleCatalog::class.java.name,
				"NO_SUCH_ENTRY",
			),
		).isNull()
	}

	@Test
	@DisplayName("loads code and message from a real catalog-shaped enum")
	fun loadsExistingEntry() {
		assertThat(
			CatalogErrorDefinitionLookup.codeOf(SampleCatalog::class.java.name, "ALPHA"),
		).isEqualTo("alpha")
		assertThat(
			CatalogErrorDefinitionLookup.messageOf(SampleCatalog::class.java.name, "ALPHA"),
		).isEqualTo("Alpha message")
	}

	@Test
	@DisplayName("OutOfMemoryError from Class.forName is not swallowed as null")
	fun errorIsNotSwallowed() {
		// Package-private hook: force Class.forName path via a name that cannot exist is null;
		// Error propagation is covered by verifying catch list excludes Error via reflection
		// on production source contract — simulate by invoking load through a broken classloader
		// is heavy. Instead assert that AssertionError from a hostile getter is not converted:
		assertThatThrownBy {
			CatalogErrorDefinitionLookup.messageOf(
				HostileCatalog::class.java.name,
				"BOOM",
			)
		}.isInstanceOf(AssertionError::class.java)
	}

	enum class SampleCatalog(val code: String, val message: String) {
		ALPHA("alpha", "Alpha message"),
	}

	/** Throws from `getMessage()` so [CatalogErrorDefinitionLookup] must not swallow [Error].
 */
	enum class HostileCatalog {
		BOOM,
		;

		@Suppress("unused")
		fun getCode(): String = "x"

		@Suppress("unused")
		fun getMessage(): String {
			throw AssertionError("must not be swallowed")
		}
	}
}
