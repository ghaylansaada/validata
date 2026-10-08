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
package io.ghaylan.validata.schema

import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.internal.ReflectionUtils
import io.ghaylan.validata.schema.runtime.SchemaNotFoundException
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * Lookup, cache, and polymorphism behaviour for [ValidationRegistry].
 * 
 * @author Ghaylan Saada

 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidationRegistryTest {

	private lateinit var registry: ValidationRegistry

	@BeforeAll
	fun installFixtureModule() {
		GeneratedSchemas.resetForTests()
		registry = ValidationRegistry()
	}

	@AfterAll
	fun resetGeneratedSchemas() {
		GeneratedSchemas.resetForTests()
	}

	@Test
	@DisplayName("registerStaticSchemas / getSchemaByRequest round-trip")
	fun staticSchemas() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			id = "registry-static",
		)
		registry.registerStaticSchemas(mapOf(schema.id to schema))
		assertThat(registry.getSchemaByRequest("registry-static")).isSameAs(schema)
		assertThat(registry.getSchemaByRequest("missing")).isNull()
	}

	@Test
	@DisplayName("resolveObjectSchemaByClass throws SchemaNotFoundException for unknown types")
	fun missingGeneratedSchema() {
		val snf = assertThrows<SchemaNotFoundException> {
			registry.resolveObjectSchemaByClass(NeverSeen::class.java)
		}
		assertThat(snf.type).isEqualTo(NeverSeen::class.java)
		assertThat(snf.message).contains("validata-processor")
	}

	@Test
	@DisplayName("resolveObjectSchemaByClass returns SPI schema and caches by raw Class identity")
	fun resolvesGeneratedAndCaches() {
		val clazz = RegistryFixtureDto::class.java
		val first = registry.resolveObjectSchemaByClass(clazz)
		val second = registry.resolveObjectSchemaByClass(clazz)
		val third = registry.resolveObjectSchemaByClass(clazz)
		assertThat(first.type).isEqualTo(clazz)
		assertThat(second).isSameAs(first)
		assertThat(third).isSameAs(first)
	}

	@Test
	@DisplayName("missing schema is not negatively cached — still throws every time")
	fun missingSchemaNotNegativelyCached() {
		repeat(3) {
			assertThrows<SchemaNotFoundException> {
				registry.resolveObjectSchemaByClass(NeverSeen::class.java)
			}
		}
		assertThat(registry.tryResolveObjectSchema(NeverSeen::class.java)).isNull()
	}

	@Test
	@DisplayName("schemaForValue returns declared schema for null or exact declared type")
	fun schemaForValueNullAndExact() {
		val child = ObjectSchema(type = ChildDto::class.java, properties = emptyList())
		val parent = ObjectSchema(
			type = ParentDto::class.java,
			properties = emptyList(),
			subtypes = mapOf(ChildDto::class.java to child),
		)
		assertThat(registry.schemaForValue(parent, null)).isSameAs(parent)
		assertThat(registry.schemaForValue(parent, ParentDto())).isSameAs(parent)
		assertThat(registry.schemaForValue(parent, ChildDto())).isSameAs(child)
	}

	@Test
	@DisplayName("schemaForValue picks the closest subtype among multiple assignable entries")
	fun schemaForValueClosestAmongMultiple() {
		val grandchild = ObjectSchema(type = GrandchildDto::class.java, properties = emptyList())
		val child = ObjectSchema(type = ChildDto::class.java, properties = emptyList())
		val parent = ObjectSchema(
			type = ParentDto::class.java,
			properties = emptyList(),
			subtypes = mapOf(
				ChildDto::class.java to child,
				GrandchildDto::class.java to grandchild,
			),
		)
		assertThat(registry.schemaForValue(parent, GrandchildDto())).isSameAs(grandchild)
		assertThat(registry.schemaForValue(parent, ChildDto())).isSameAs(child)
	}

	@Test
	@DisplayName("tryResolveObjectSchema returns null without throwing for unknown types")
	fun tryResolveReturnsNull() {
		assertThat(registry.tryResolveObjectSchema(NeverSeen::class.java)).isNull()
	}

	@Test
	@DisplayName("freeze rejects further registration and deep-freezes catalogs")
	fun freezeRejectsFurtherRegistration() {
		val local = ValidationRegistry()
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			id = "freeze-static",
		)
		local.registerStaticSchemas(mapOf(schema.id to schema))
		assertThat(local.isFrozen()).isFalse()
		local.freeze()
		assertThat(local.isFrozen()).isTrue()
		assertThat(local.getSchemaByRequest("freeze-static")).isSameAs(schema)
		assertThat(local.staticSchemas()).containsEntry("freeze-static", schema)

		assertThrows<IllegalStateException> {
			local.registerStaticSchemas(mapOf("again" to schema))
		}
		assertThrows<IllegalStateException> {
			local.registerValidators(emptyMap())
		}
		// Idempotent
		local.freeze()
		assertThat(local.isFrozen()).isTrue()
	}

	@Test
	@DisplayName("registerValidators populates validatorCatalog and remains readable after freeze")
	fun registerValidatorsHappyPath() {
		val local = ValidationRegistry()
		val typeInfo = ReflectionUtils.infoFromClass(String::class.java)
		local.registerValidators(
			mapOf(
				RequiredConstraint::class to mapOf(typeInfo to RequiredValidator),
			),
		)
		assertThat(local.validatorCatalog()).containsKey(RequiredConstraint::class)
		assertThat(local.validatorCatalog()[RequiredConstraint::class]).containsKey(typeInfo)
		local.freeze()
		assertThat(local.validatorCatalog()).containsKey(RequiredConstraint::class)
		assertThrows<UnsupportedOperationException> {
			(local.validatorCatalog() as MutableMap<*, *>).clear()
		}
	}

	@Test
	@DisplayName("schemaForValue falls back to generated schema when runtime class is absent from subtypes")
	fun schemaForValueSpiFallback() {
		val parent = ObjectSchema(
			type = ParentDto::class.java,
			properties = emptyList(),
			subtypes = mapOf(ChildDto::class.java to ObjectSchema(type = ChildDto::class.java, properties = emptyList())),
		)
		// RegistryFixtureDto is not a subtype of ParentDto but has an SPI schema.
		val resolved = registry.schemaForValue(parent, RegistryFixtureDto(value = "ok"))
		assertThat(resolved.type).isEqualTo(RegistryFixtureDto::class.java)
		assertThat(resolved.properties.map { it.externalName }).contains("value")
	}

	private class NeverSeen

	open class ParentDto
	open class ChildDto : ParentDto()
	class GrandchildDto : ChildDto()
}
