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

import io.ghaylan.validata.constraint.annotation.EmailConstraint
import io.ghaylan.validata.constraint.annotation.PhoneConstraint
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.constraint.composition.CompositionOrRunner
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.constraint.validator.string.phone.PhoneValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards composition OR error-code parity between property and endpoint collectors.
 * 
 * @author Ghaylan Saada
 */
class CompositionErrorCodeCollectorTest {
	
	private val groups = setOf(OnDefault::class)
	
	@BeforeEach
	fun reset() {
		EndpointErrorCodeCollector.resetForTests()
	}
	
	@Test
	@DisplayName("endpoint and property collectors both publish CONSTRAINT_UNSATISFIABLE for OR composition")
	fun compositionCodesAgree() {
		val email = EmailConstraint("", groups)
		val phone = PhoneConstraint(
			allowedTypes = emptySet(),
			allowedCountries = emptySet(),
			message = "",
			groups = groups,
		)
		val composition = CompositionConstraint(
			message = "",
			groups = groups,
			children = listOf(
				CompiledConstraint(email, ValidatorBackedRunner(EmailValidator, email), 0),
				CompiledConstraint(phone, ValidatorBackedRunner(PhoneValidator, phone), 1),
			),
		)
		val compositionCompiled = CompiledConstraint(
			composition,
			CompositionOrRunner(composition),
			0,
		)
		val prop = PropertySpec(
			declaredName = "contact",
			externalName = "contact",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(compositionCompiled),
		)
		val propertyCodes = PropertyErrorCodeCollector.collect(prop, groups)
			.mapNotNull { it["code"] }
		val endpoint = EndpointSchema(
			id = "composition-test",
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(prop),
			),
			groups = groups,
		)
		val endpointCodes = EndpointErrorCodeCollector.collect(endpoint)
		
		assertThat(propertyCodes).contains(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE.code)
		assertThat(endpointCodes).contains(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE.code)
		val emailCodes = EmailValidator.possibleErrorCodes(email, String::class.java).map { it.code }
		assertThat(propertyCodes).containsAll(emailCodes)
		assertThat(endpointCodes).containsAll(emailCodes)
	}
	
	@Test
	@DisplayName("inactive groups omit composition and leaf codes from both collectors")
	fun inactiveGroupsOmitCompositionCodes() {
		val createGroups = setOf(OnCreate::class)
		val email = EmailConstraint("", createGroups)
		val phone = PhoneConstraint(
			allowedTypes = emptySet(),
			allowedCountries = emptySet(),
			message = "",
			groups = createGroups,
		)
		val composition = CompositionConstraint(
			message = "",
			groups = createGroups,
			children = listOf(
				CompiledConstraint(email, ValidatorBackedRunner(EmailValidator, email), 0),
				CompiledConstraint(phone, ValidatorBackedRunner(PhoneValidator, phone), 1),
			),
		)
		val compositionCompiled = CompiledConstraint(
			composition,
			CompositionOrRunner(composition),
			0,
		)
		val prop = PropertySpec(
			declaredName = "contact",
			externalName = "contact",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(compositionCompiled),
		)
		val active = setOf(OnDefault::class)
		val propertyCodes = PropertyErrorCodeCollector.collect(prop, active)
			.mapNotNull { it["code"] }
		val endpoint = EndpointSchema(
			id = "composition-inactive",
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(prop),
			),
			groups = active,
		)
		val endpointCodes = EndpointErrorCodeCollector.collect(endpoint)
		
		assertThat(propertyCodes).doesNotContain(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE.code)
		assertThat(endpointCodes).doesNotContain(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE.code)
		val emailCodes = EmailValidator.possibleErrorCodes(email, String::class.java).map { it.code }
		assertThat(propertyCodes).doesNotContainAnyElementsOf(emailCodes)
		assertThat(endpointCodes).doesNotContainAnyElementsOf(emailCodes)
	}
}
