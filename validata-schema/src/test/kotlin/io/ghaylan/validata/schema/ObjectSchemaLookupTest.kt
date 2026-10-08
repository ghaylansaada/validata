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

import io.ghaylan.validata.schema.shape.ObjectRefShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.schema.support.dto.Address
import io.ghaylan.validata.schema.support.dto.Payload
import io.ghaylan.validata.schema.support.dto.ProfilePayload
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks [ObjectSchema] indexes, polymorphism hooks, and duplicate-name fail-loud rules the
 * engine / path layer depend on.
 *
 * Complements [ObjectSchemaSmokeTest] (construct / cycle / ValueReader). Does not assert
 * engine walk outcomes.
 * 
 * @author Ghaylan Saada
 */
class ObjectSchemaLookupTest {
	
	@Test
	@DisplayName("byDeclaredName and byExternalName index the same property under different keys")
	fun declaredVsExternalIndexes() {
		val schema = ObjectSchema(
			type = Address::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "city",
					externalName = "town",
					shape = ScalarShape(ScalarKind.STRING),
					read = { (it as Address).city },
				),
			),
		)
		
		assertThat(schema.byDeclaredName["city"]?.externalName).isEqualTo("town")
		assertThat(schema.byExternalName["town"]?.declaredName).isEqualTo("city")
		assertThat(schema.byDeclaredName["town"]).isNull()
		assertThat(schema.byExternalName["city"]).isNull()
	}
	
	@Test
	@DisplayName("selfRef is a stable ObjectRefShape pointing at the same schema instance")
	fun selfRefIsStable() {
		val schema = ObjectSchema(
			type = Address::class.java,
			properties = emptyList(),
		)
		val first = schema.selfRef
		val second = schema.selfRef
		assertThat(first).isSameAs(second)
		assertThat(first).isInstanceOf(ObjectRefShape::class.java)
		assertThat(first.ref.value).isSameAs(schema)
	}
	
	@Test
	@DisplayName("subtypes map retains concrete schemas keyed by runtime class")
	fun subtypesRetained() {
		val profile = ObjectSchema(
			type = ProfilePayload::class.java,
			properties = emptyList(),
		)
		val root = ObjectSchema(
			type = Payload::class.java,
			properties = emptyList(),
			subtypes = mapOf(ProfilePayload::class.java to profile),
		)
		
		assertThat(root.subtypes).containsOnlyKeys(ProfilePayload::class.java)
		assertThat(root.subtypes.getValue(ProfilePayload::class.java)).isSameAs(profile)
	}
	
	@Test
	@DisplayName("properties list order is preserved for deterministic walks")
	fun propertiesOrderPreserved() {
		val first = PropertySpec(
			declaredName = "a",
			externalName = "a",
			shape = ScalarShape(ScalarKind.STRING),
			read = { null },
		)
		val second = PropertySpec(
			declaredName = "b",
			externalName = "b",
			shape = ScalarShape(ScalarKind.STRING),
			read = { null },
		)
		val schema = ObjectSchema(
			type = Address::class.java,
			properties = listOf(first, second),
		)
		
		assertThat(schema.properties).containsExactly(first, second)
	}
	
	@Test
	@DisplayName("duplicate declaredName fails with actionable diagnostics naming both indices")
	fun duplicateDeclaredNameFails() {
		val first = PropertySpec(
			declaredName = "city",
			externalName = "city_a",
			shape = ScalarShape(ScalarKind.STRING),
			read = { (it as Address).city },
		)
		val second = PropertySpec(
			declaredName = "city",
			externalName = "city_b",
			shape = ScalarShape(ScalarKind.STRING),
			read = { (it as Address).city },
		)
		
		assertThatIllegalStateException().isThrownBy {
			ObjectSchema(
				type = Address::class.java,
				properties = listOf(first, second),
			)
		}.withMessageContaining("Duplicate declaredName 'city'").withMessageContaining(Address::class.java.name).withMessageContaining("properties[0]").withMessageContaining("properties[1]").withMessageContaining("ObjectSchemaModule")
	}
	
	@Test
	@DisplayName("constructor snapshots properties — mutating the source list does not affect the schema")
	fun propertiesAreFrozenAgainstCallerMutation() {
		val mutable = mutableListOf(
			PropertySpec(
				declaredName = "city",
				externalName = "city",
				shape = ScalarShape(ScalarKind.STRING),
				read = { (it as Address).city },
			),
		)
		val schema = ObjectSchema(
			type = Address::class.java,
			properties = mutable,
		)
		
		mutable.add(
			PropertySpec(
				declaredName = "extra",
				externalName = "extra",
				shape = ScalarShape(ScalarKind.STRING),
				read = { null },
			),
		)
		
		assertThat(schema.properties).hasSize(1)
		assertThat(schema.byDeclaredName).containsOnlyKeys("city")
		org.assertj.core.api.Assertions.assertThatThrownBy {
			(schema.properties as MutableList).add(
				PropertySpec(
					declaredName = "viaCast",
					externalName = "viaCast",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
				),
			)
		}.isInstanceOf(UnsupportedOperationException::class.java)
	}
	
	@Test
	@DisplayName("constructor snapshots subtypes — mutating the source map does not affect the schema")
	fun subtypesAreFrozenAgainstCallerMutation() {
		val profile = ObjectSchema(
			type = ProfilePayload::class.java,
			properties = emptyList(),
		)
		val mutable = mutableMapOf<Class<*>, ObjectSchema>(ProfilePayload::class.java to profile)
		val root = ObjectSchema(
			type = Payload::class.java,
			properties = emptyList(),
			subtypes = mutable,
		)
		
		mutable[Address::class.java] = ObjectSchema(type = Address::class.java, properties = emptyList())
		
		assertThat(root.subtypes).containsOnlyKeys(ProfilePayload::class.java)
		org.assertj.core.api.Assertions.assertThatThrownBy {
			(root.subtypes as MutableMap)[Address::class.java] = ObjectSchema(type = Address::class.java, properties = emptyList())
		}.isInstanceOf(UnsupportedOperationException::class.java)
	}
	
	@Test
	@DisplayName("toString reports type name with property and subtype counts")
	fun toStringDiagnostic() {
		val profile = ObjectSchema(
			type = ProfilePayload::class.java,
			properties = emptyList(),
		)
		val schema = ObjectSchema(
			type = Payload::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "id",
					externalName = "id",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
				),
			),
			subtypes = mapOf(ProfilePayload::class.java to profile),
		)
		
		assertThat(schema.toString()).isEqualTo(
			"ObjectSchema(${Payload::class.java.name}, props=1, subtypes=1)",
		)
	}
	
	@Test
	@DisplayName("duplicate externalName fails with actionable diagnostics naming both indices")
	fun duplicateExternalNameFails() {
		val first = PropertySpec(
			declaredName = "city_a",
			externalName = "city",
			shape = ScalarShape(ScalarKind.STRING),
			read = { (it as Address).city },
		)
		val second = PropertySpec(
			declaredName = "city_b",
			externalName = "city",
			shape = ScalarShape(ScalarKind.STRING),
			read = { (it as Address).city },
		)
		
		assertThatIllegalStateException().isThrownBy {
			ObjectSchema(
				type = Address::class.java,
				properties = listOf(first, second),
			)
		}.withMessageContaining("Duplicate externalName 'city'").withMessageContaining(Address::class.java.name).withMessageContaining("properties[0]").withMessageContaining("properties[1]")
	}
}
