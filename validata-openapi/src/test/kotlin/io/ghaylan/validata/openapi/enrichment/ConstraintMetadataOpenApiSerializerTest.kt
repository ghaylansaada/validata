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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.constraint.validator.string.phone.PhoneValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Guards password-metadata leak, empty-arg omission, and kind key on constraint entries.
 * 
 * @author Ghaylan Saada
 */
class ConstraintMetadataOpenApiSerializerTest {
	
	private val groups: Set<KClass<*>> = setOf(OnDefault::class)
	
	@Test
	@DisplayName("entry has _constraint kind; omits message/groups")
	fun entryHasKindOmitsMessageGroups() {
		val constraint = SizeConstraint(min = 2, max = 40, message = "hi", groups = groups)
		val entry = ConstraintMetadataOpenApiSerializer.toConstraintEntry(constraint)
		assertThat(entry[ConstraintExtensionKeys.CONSTRAINT_KIND]).isEqualTo("Size")
		assertThat(ConstraintMetadataOpenApiSerializer.typeName(constraint)).isEqualTo("Size")
		assertThat(entry["min"]).isEqualTo(2)
		assertThat(entry["max"]).isEqualTo(40)
		assertThat(entry).doesNotContainKeys("message", "groups")
	}
	
	@Test
	@DisplayName("Distinct with empty by omits the by key")
	fun omitsEmptyDistinctBy() {
		val entry = ConstraintMetadataOpenApiSerializer.toConstraintEntry(
			DistinctConstraint(by = emptySet(), message = "", groups = groups),
		)
		assertThat(entry).containsOnlyKeys(ConstraintExtensionKeys.CONSTRAINT_KIND)
		assertThat(entry[ConstraintExtensionKeys.CONSTRAINT_KIND]).isEqualTo("Distinct")
	}
	
	@Test
	@DisplayName("PasswordConstraint args are policy metadata only — no password value")
	fun passwordConstraintNeverSerializesAPasswordValue() {
		val constraint = PasswordConstraint(
			minLength = 8,
			maxLength = 64,
			requireUppercase = true,
			requireLowercase = true,
			requireDigit = true,
			requireSpecialChar = false,
			allowedSpecialChars = "!@#",
			noSequentialChars = false,
			noRepetitivePatterns = false,
			message = "secret-policy",
			groups = groups,
		)
		val map = ConstraintMetadataOpenApiSerializer.toArgsMap(constraint)
		assertThat(ConstraintMetadataOpenApiSerializer.typeName(constraint)).isEqualTo("Password")
		assertThat(map["minLength"]).isEqualTo(8)
		assertThat(map["requireUppercase"]).isEqualTo(true)
		assertThat(map).doesNotContainKeys("message", "groups", "value", "password")
		assertThat(map.values.map {
			it?.toString()
				.orEmpty()
		}).noneMatch { it.contains("secret") }
	}
	
	@Test
	@DisplayName("Compare PropertyRef rewrites to sibling externalName")
	fun compareRefRewritesToWireName() {
		val owner = ObjectSchema(
			type = Any::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "confirmSecret",
					externalName = "confirm_secret",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { null },
				),
			),
		)
		val entry = ConstraintMetadataOpenApiSerializer.toConstraintEntry(
			CompareConstraint(
				ref = "confirmSecret",
				operation = Compare.Operation.EQ,
				message = "",
				groups = groups,
			),
			siblingSchema = owner,
		)
		assertThat(entry["ref"]).isEqualTo("confirm_secret")
		assertThat(entry[ConstraintExtensionKeys.CONSTRAINT_KIND]).isEqualTo("Compare")
	}
	
	@Test
	@DisplayName("Distinct by PropertyRef rewrites to element externalName")
	fun distinctByRewritesToElementWireName() {
		val element = ObjectSchema(
			type = Any::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "code",
					externalName = "sku_code",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { null },
				),
			),
		)
		val entry = ConstraintMetadataOpenApiSerializer.toConstraintEntry(
			DistinctConstraint(by = setOf("code"), message = "", groups = groups),
			elementSchema = element,
		)
		assertThat(entry["by"]).isEqualTo(listOf("sku_code"))
	}
	
	@Test
	@DisplayName("CompositionConstraint serializes as OR with leaf members")
	fun compositionSerializesMembers() {
		val email = EmailConstraint("", groups)
		val phone = PhoneConstraint(
			allowedTypes = emptySet(),
			allowedCountries = emptySet(),
			message = "",
			groups = groups,
		)
		val composition = CompositionConstraint(
			message = "either",
			groups = groups,
			children = listOf(
				CompiledConstraint(email, ValidatorBackedRunner(EmailValidator, email), 0),
				CompiledConstraint(phone, ValidatorBackedRunner(PhoneValidator, phone), 1),
			),
		)
		val entry = ConstraintMetadataOpenApiSerializer.toConstraintEntry(composition)
		assertThat(entry[ConstraintExtensionKeys.CONSTRAINT_KIND]).isEqualTo("Composition")
		assertThat(entry["composition"]).isEqualTo("OR")
		assertThat(entry).doesNotContainKeys("message", "groups", "children")
		@Suppress("UNCHECKED_CAST")
		val members = entry["members"] as List<Map<String, Any?>>
		assertThat(members).hasSize(2)
		assertThat(members.map { it[ConstraintExtensionKeys.CONSTRAINT_KIND] }).containsExactly("Email", "Phone")
	}
}
