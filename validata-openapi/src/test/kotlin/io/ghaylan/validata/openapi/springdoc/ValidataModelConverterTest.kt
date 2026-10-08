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
package io.ghaylan.validata.openapi.springdoc

import io.ghaylan.validata.openapi.enrichment.OpenApiEnrichmentCache
import io.ghaylan.validata.openapi.support.OpenApiProbeDto
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Guards components-schema enrichment and identity-cache skip (Jakarta simple-name collision is
 * covered by [JakartaValidationAnnotationFilterTest]).
 * 
 * @author Ghaylan Saada
 */
class ValidataModelConverterTest {
	
	@BeforeEach
	fun reset() {
		OpenApiEnrichmentCache.resetForTests()
		GeneratedSchemas.resetForTests()
	}
	
	@Test
	@DisplayName("unknown types are left unchanged")
	fun unknownTypePassesThrough() {
		val base = Schema<Any>().apply { minLength = 9 }
		val converter = ValidataModelConverter(mappers = emptyList())
		val resolved = converter.resolve(
			AnnotatedType(String::class.java),
			mockContext(emptyMap()),
			chainOf(base),
		)
		assertThat(resolved).isSameAs(base)
		assertThat(resolved?.minLength).isEqualTo(9)
	}
	
	@Test
	@DisplayName("enriches the components model for a generated DTO and skips a second pass")
	fun enrichesDefinedModelOnce() {
		assertThat(GeneratedSchemas.get(OpenApiProbeDto::class.java)).isNotNull
		val defined = Schema<Any>().apply {
			name = "OpenApiProbeDto"
			properties = linkedMapOf("name" to Schema<Any>())
		}
		val ref = Schema<Any>().apply {
			name = "OpenApiProbeDto"
			`$ref` = "#/components/schemas/OpenApiProbeDto"
		}
		val context = mockContext(mapOf("OpenApiProbeDto" to defined))
		val converter = ValidataModelConverter(mappers = emptyList())
		
		converter.resolve(AnnotatedType(OpenApiProbeDto::class.java).name("OpenApiProbeDto"), context, chainOf(ref))
		assertThat(defined.properties["name"]?.minLength).isEqualTo(2)
		assertThat(defined.properties["name"]?.maxLength).isEqualTo(40)
		
		defined.properties["name"]?.minLength = 99
		converter.resolve(AnnotatedType(OpenApiProbeDto::class.java).name("OpenApiProbeDto"), context, chainOf(ref))
		assertThat(defined.properties["name"]?.minLength).isEqualTo(99)
	}
	
	private fun mockContext(defined: Map<String, Schema<*>>): ModelConverterContext {
		val context = mock(ModelConverterContext::class.java)
		`when`(context.definedModels).thenReturn(defined)
		return context
	}
	
	private fun chainOf(schema: Schema<*>): MutableIterator<ModelConverter> {
		val inner = mock(ModelConverter::class.java)
		`when`(inner.resolve(any(), any(), any())).thenReturn(schema)
		return mutableListOf(inner).iterator()
	}
}
