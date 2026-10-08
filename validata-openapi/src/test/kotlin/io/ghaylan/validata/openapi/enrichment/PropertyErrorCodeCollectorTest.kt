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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards `@ApiError` override of validator codes, always-on `VALUE_TYPE_MISMATCH`,
 * `VALUE_MISSING` for `@Required`, and group filtering.
 * 
 * @author Ghaylan Saada
 */
class PropertyErrorCodeCollectorTest {
	
	@Test
	@DisplayName("@ApiError wins on code collision; Required adds VALUE_MISSING; VALUE_TYPE_MISMATCH always present")
	fun errorDocOverridesValidatorCodeAndRequiredAddsValueMissing() {
		val required = RequiredConstraint(Required.Mode.STRICT, "", setOf(OnDefault::class))
		val size = SizeConstraint(2, 40, "", setOf(OnDefault::class))
		val prop = PropertySpec(
			declaredName = "email",
			externalName = "email",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(
				CompiledConstraint(required, ValidatorBackedRunner(RequiredValidator, required), 0),
				CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 1),
			),
			errorDocs = listOf(
				SchemaErrorDoc(code = "TEXT_TOO_SHORT", message = "Email is too short for this API"),
				SchemaErrorDoc(code = "EMAIL_TAKEN", message = "already registered"),
			),
		)
		val errors = PropertyErrorCodeCollector.collect(prop, setOf(OnDefault::class))
		val byCode = errors.associate { it.getValue("code") to it.getValue("message") }
		assertThat(byCode["TEXT_TOO_SHORT"]).isEqualTo("Email is too short for this API")
		assertThat(byCode["EMAIL_TAKEN"]).isEqualTo("already registered")
		assertThat(byCode[ConstraintErrorCode.VALUE_TYPE_MISMATCH.name]).isEqualTo(ConstraintErrorCode.VALUE_TYPE_MISMATCH.message)
		assertThat(byCode[ConstraintErrorCode.VALUE_MISSING.name]).isEqualTo(ConstraintErrorCode.VALUE_MISSING.message)
		assertThat(errors.map { it.getValue("code") }).isSorted
	}
	
	@Test
	@DisplayName("inactive group constraints do not contribute codes; VALUE_TYPE_MISMATCH remains")
	fun inactiveGroupsAreOmitted() {
		val size = SizeConstraint(2, 40, "", setOf(OnCreate::class))
		val prop = PropertySpec(
			declaredName = "name",
			externalName = "name",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(
				CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 0),
			),
		)
		val errors = PropertyErrorCodeCollector.collect(prop, setOf(OnDefault::class))
		assertThat(errors.map { it["code"] }).containsExactly(ConstraintErrorCode.VALUE_TYPE_MISMATCH.name)
	}
}
