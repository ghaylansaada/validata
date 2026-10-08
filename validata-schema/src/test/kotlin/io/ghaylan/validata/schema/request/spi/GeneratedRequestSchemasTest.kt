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
package io.ghaylan.validata.schema.request.spi

import io.ghaylan.validata.schema.spi.SchemaSpiDiagnostics
import io.ghaylan.validata.schema.support.request.SampleRequestSchemas
import io.ghaylan.validata.schema.support.spi.RequestSchemaModuleMerge
import io.ghaylan.validata.schema.support.spi.SampleRequestSchemaModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for [GeneratedRequestSchemas]: real [java.util.ServiceLoader] load, cache, and duplicate rules.
 *
 * ## Why this suite exists
 *
 * Endpoint IR is keyed by stable method identifiers. Duplicate contributions must fail loudly so
 * two controllers cannot silently overwrite each other. Uses
 * [SampleRequestSchemaModule] for ServiceLoader discovery and [RequestSchemaModuleMerge] for conflict rules.
 * 
 * @author Ghaylan Saada
 */
class GeneratedRequestSchemasTest {
	
	@AfterEach
	fun resetCache() {
		GeneratedRequestSchemas.resetForTests()
	}
	
	@Test
	@DisplayName("ServiceLoader discovers SampleRequestSchemaModule from META-INF/services")
	fun loadsRegisteredRequestSchemaModule() {
		val schema = GeneratedRequestSchemas.get(SampleRequestSchemaModule.ENDPOINT_ID)
		assertThat(schema).isNotNull
		assertThat(schema!!.id).isEqualTo(SampleRequestSchemaModule.ENDPOINT_ID)
		assertThat(GeneratedRequestSchemas.all()).containsKey(SampleRequestSchemaModule.ENDPOINT_ID)
	}
	
	@Test
	@DisplayName("endpoint ids not contributed by any RequestSchemaModule resolve to null")
	fun unknownEndpointIsNull() {
		assertThat(GeneratedRequestSchemas.get("never.contributed#id()")).isNull()
	}
	
	@Test
	@DisplayName("all() caches the ServiceLoader snapshot until resetForTests")
	fun cachesUntilReset() {
		val first = GeneratedRequestSchemas.all()
		val second = GeneratedRequestSchemas.all()
		assertThat(second).isSameAs(first)
		
		GeneratedRequestSchemas.resetForTests()
		val third = GeneratedRequestSchemas.all()
		assertThat(third).isNotSameAs(first)
		assertThat(third.keys).isEqualTo(first.keys)
		assertThat(third[SampleRequestSchemaModule.ENDPOINT_ID]?.id).isEqualTo(SampleRequestSchemaModule.ENDPOINT_ID)
	}
	
	@Test
	@DisplayName("merging two RequestSchemaModules for the same endpoint id fails with production diagnostic text")
	fun duplicateSchemasFail() {
		val id = "com.acme.Api#create()"
		val schema = SampleRequestSchemas.of(id)
		val first = RequestSchemaModule { mapOf(id to schema) }
		val second = RequestSchemaModule { mapOf(id to schema) }
		
		assertThatThrownBy {
			RequestSchemaModuleMerge.merge(first, second)
		}.hasMessage(
			SchemaSpiDiagnostics.requestSchemaDuplicate(
				id,
				first.javaClass.name,
				second.javaClass.name,
			),
		)
	}
	
	@Test
	@DisplayName("merging two RequestSchemaModules with different endpoint ids keeps both entries")
	fun distinctKeysMergeSuccessfully() {
		val create = SampleRequestSchemas.of("com.acme.Api#create()")
		val update = SampleRequestSchemas.of("com.acme.Api#update()")
		val first = RequestSchemaModule { mapOf(create.id to create) }
		val second = RequestSchemaModule { mapOf(update.id to update) }
		val merged = RequestSchemaModuleMerge.merge(first, second)
		
		assertThat(merged).containsOnlyKeys(create.id, update.id)
		assertThat(merged[create.id]).isSameAs(create)
		assertThat(merged[update.id]).isSameAs(update)
		assertThatThrownBy { (merged as MutableMap<String, *>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
	}
	
	@Test
	@DisplayName("all() returns an unmodifiable map so callers cannot corrupt the cache")
	fun allIsUnmodifiable() {
		val all = GeneratedRequestSchemas.all()
		assertThatThrownBy { (all as MutableMap<String, *>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
	}
}
