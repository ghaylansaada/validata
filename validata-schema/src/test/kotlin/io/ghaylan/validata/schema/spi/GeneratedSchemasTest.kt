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
package io.ghaylan.validata.schema.spi

import io.ghaylan.validata.schema.support.SampleObjectSchemas
import io.ghaylan.validata.schema.support.dto.Address
import io.ghaylan.validata.schema.support.spi.ObjectSchemaModuleMerge
import io.ghaylan.validata.schema.support.spi.SampleObjectSchemaModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for [GeneratedSchemas]: real [java.util.ServiceLoader] load, cache, and duplicate-merge rules.
 *
 * ## Why this suite exists
 *
 * Apps and the Spring host discover object IR only through this aggregator. A silent overwrite or
 * a broken `META-INF/services` registration would validate the wrong graph. Uses
 * [SampleObjectSchemaModule] on the test classpath for the happy path, and [ObjectSchemaModuleMerge]
 * for conflict rules.
 * 
 * @author Ghaylan Saada
 */
class GeneratedSchemasTest {
	
	@AfterEach
	fun resetCache() {
		GeneratedSchemas.resetForTests()
	}
	
	@Test
	@DisplayName("ServiceLoader discovers SampleObjectSchemaModule from META-INF/services")
	fun loadsRegisteredObjectSchemaModule() {
		val schema = GeneratedSchemas.get(Address::class.java)
		assertThat(schema).isNotNull
		assertThat(schema!!.type).isEqualTo(Address::class.java)
		assertThat(GeneratedSchemas.all()).containsKey(Address::class.java)
	}
	
	@Test
	@DisplayName("types not contributed by any ObjectSchemaModule resolve to null")
	fun unknownTypeIsNull() {
		assertThat(GeneratedSchemas.get(NeverContributed::class.java)).isNull()
	}
	
	@Test
	@DisplayName("all() caches the ServiceLoader snapshot until resetForTests")
	fun cachesUntilReset() {
		val first = GeneratedSchemas.all()
		val second = GeneratedSchemas.all()
		assertThat(second).isSameAs(first)
		
		GeneratedSchemas.resetForTests()
		val third = GeneratedSchemas.all()
		assertThat(third).isNotSameAs(first)
		assertThat(third.keys).isEqualTo(first.keys)
		assertThat(third[Address::class.java]?.type).isEqualTo(Address::class.java)
	}
	
	@Test
	@DisplayName("merging two ObjectSchemaModules for the same Class fails with production diagnostic text")
	fun duplicateSchemasFail() {
		val schema = SampleObjectSchemas.of()
		val first = ObjectSchemaModule { mapOf(String::class.java to schema) }
		val second = ObjectSchemaModule { mapOf(String::class.java to schema) }
		
		assertThatThrownBy {
			ObjectSchemaModuleMerge.merge(first, second)
		}.hasMessage(
			SchemaSpiDiagnostics.objectSchemaDuplicate(
				String::class.java,
				first.javaClass.name,
				second.javaClass.name,
			),
		)
	}
	
	@Test
	@DisplayName("merging two ObjectSchemaModules with different keys keeps both entries")
	fun distinctKeysMergeSuccessfully() {
		val stringSchema = SampleObjectSchemas.of(String::class.java)
		val intSchema = SampleObjectSchemas.of(Int::class.java)
		val first = ObjectSchemaModule { mapOf(String::class.java to stringSchema) }
		val second = ObjectSchemaModule { mapOf(Int::class.java to intSchema) }
		val merged = ObjectSchemaModuleMerge.merge(first, second)
		
		assertThat(merged).containsOnlyKeys(String::class.java, Int::class.java)
		assertThat(merged[String::class.java]).isSameAs(stringSchema)
		assertThat(merged[Int::class.java]).isSameAs(intSchema)
		assertThatThrownBy { (merged as MutableMap<Class<*>, *>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
	}
	
	@Test
	@DisplayName("all() returns an unmodifiable map so callers cannot corrupt the cache")
	fun allIsUnmodifiable() {
		val all = GeneratedSchemas.all()
		assertThatThrownBy { (all as MutableMap<Class<*>, *>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
	}
	
	/**
	 * Marker type never contributed by the test ServiceLoader module.
	 */
	private class NeverContributed
}
