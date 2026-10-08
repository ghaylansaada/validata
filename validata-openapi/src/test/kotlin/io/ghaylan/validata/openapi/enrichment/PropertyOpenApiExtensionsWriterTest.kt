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
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.required.RequiredWhenValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Guards `x-validata-constraints` as an array of all constraints, empty-arg omission, and errors.
 * 
 * @author Ghaylan Saada
 */
class PropertyOpenApiExtensionsWriterTest {
	
	private val groups: Set<KClass<*>> = setOf(OnDefault::class)
	
	@Test
	@DisplayName("x-validata-constraints lists every constraint; omits empty RequiredWhen.values")
	fun writesAllConstraintsAsArray() {
		val requiredMeta = RequiredConstraint(Required.Mode.STRICT, "", groups)
		val sizeMeta = SizeConstraint(2, 40, "", groups)
		val equalMeta = CompareConstraint(
			ref = "confirm",
			operation = Compare.Operation.EQ,
			message = "",
			groups = groups,
		)
		val whenMeta = RequiredWhenConstraint(
			ref = "contactType",
			condition = RequiredWhen.Condition.EQ,
			value = "PHONE",
			values = emptySet(),
			mode = Required.Mode.STRICT,
			message = "",
			groups = groups,
		)
		val prop = PropertySpec(
			declaredName = "name",
			externalName = "first_name",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(
				CompiledConstraint(requiredMeta, ValidatorBackedRunner(RequiredValidator, requiredMeta), 0),
				CompiledConstraint(sizeMeta, ValidatorBackedRunner(CharSequenceSizeValidator, sizeMeta), 1),
				CompiledConstraint(equalMeta,
					ValidatorBackedRunner(
						io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator,
						equalMeta,
					),
					2),
				CompiledConstraint(whenMeta, ValidatorBackedRunner(RequiredWhenValidator, whenMeta), 3),
			),
			errorDocs = listOf(
				SchemaErrorDoc(code = "NAME_INVALID", message = "bad name"),
			),
		)
		val schema = Schema<Any>().apply { description = "keep me" }
		
		PropertyOpenApiExtensionsWriter.write(schema, prop, groups)
		
		assertThat(schema.description).isEqualTo("keep me")
		@Suppress("UNCHECKED_CAST")
		val constraints = schema.extensions[ConstraintExtensionKeys.CONSTRAINTS] as List<Map<String, Any?>>
		assertThat(constraints.map { it[ConstraintExtensionKeys.CONSTRAINT_KIND] }).containsExactly("Required", "Size", "Compare", "RequiredWhen")
		val size = constraints.single { it[ConstraintExtensionKeys.CONSTRAINT_KIND] == "Size" }
		assertThat(size["min"]).isEqualTo(2)
		assertThat(size["max"]).isEqualTo(40)
		val requiredWhen = constraints.single { it[ConstraintExtensionKeys.CONSTRAINT_KIND] == "RequiredWhen" }
		assertThat(requiredWhen["ref"]).isEqualTo("contactType")
		assertThat(requiredWhen["condition"]).isEqualTo("EQ")
		assertThat(requiredWhen["value"]).isEqualTo("PHONE")
		assertThat(requiredWhen).doesNotContainKeys("values", "message", "groups", "errorCodes")
		val compare = constraints.single { it[ConstraintExtensionKeys.CONSTRAINT_KIND] == "Compare" }
		assertThat(compare["ref"]).isEqualTo("confirm")
		assertThat(compare["operation"]).isEqualTo("EQ")
		@Suppress("UNCHECKED_CAST")
		val errors = schema.extensions[ConstraintExtensionKeys.ERRORS] as List<Map<String, String>>
		assertThat(errors.map { it["code"] }).contains(
			ConstraintErrorCode.VALUE_TYPE_MISMATCH.name,
			ConstraintErrorCode.VALUE_MISSING.name,
			"NAME_INVALID",
			"TEXT_TOO_SHORT",
			"TEXT_TOO_LONG",
			ConstraintErrorCode.COMPARISON_NOT_ORDERABLE.name,
			ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL.name,
		)
		val nameInvalid = errors.first { it["code"] == "NAME_INVALID" }
		assertThat(nameInvalid["message"]).isEqualTo("bad name")
		val valueNull = errors.first { it["code"] == ConstraintErrorCode.VALUE_MISSING.name }
		assertThat(valueNull["message"]).isEqualTo(ConstraintErrorCode.VALUE_MISSING.message)
	}
	
	@Test
	@DisplayName("duplicate constraint kinds are separate array entries")
	fun duplicateKindsAreSeparateEntries() {
		val a = CompareConstraint(ref = "left", operation = Compare.Operation.EQ, message = "", groups = groups)
		val b = CompareConstraint(ref = "right", operation = Compare.Operation.EQ, message = "", groups = groups)
		val prop = PropertySpec(
			declaredName = "name",
			externalName = "name",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(
				CompiledConstraint(a,
					ValidatorBackedRunner(
						io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator,
						a,
					),
					0),
				CompiledConstraint(b,
					ValidatorBackedRunner(
						io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator,
						b,
					),
					1),
			),
		)
		val schema = Schema<Any>()
		PropertyOpenApiExtensionsWriter.write(schema, prop, groups)
		@Suppress("UNCHECKED_CAST")
		val constraints = schema.extensions[ConstraintExtensionKeys.CONSTRAINTS] as List<Map<String, Any?>>
		assertThat(constraints).hasSize(2)
		assertThat(constraints.map { it[ConstraintExtensionKeys.CONSTRAINT_KIND] }).containsExactly("Compare", "Compare")
		assertThat(constraints.map { it["ref"] }).containsExactly("left", "right")
	}
	
	@Test
	@DisplayName("numeric and temporal Min both appear; Distinct omits empty by")
	fun allMinsAndEmptyDistinctBy() {
		val temporalMin = MinConstraint(value = "2020-02-29", inclusive = true, message = "", groups = groups)
		val numericMin = MinConstraint(value = "5", inclusive = true, message = "", groups = groups)
		val distinct = DistinctConstraint(by = emptySet(), message = "", groups = groups)
		val prop = PropertySpec(
			declaredName = "window",
			externalName = "window",
			shape = ScalarShape(ScalarKind.STRING),
			read = ValueReader { null },
			constraints = listOf(
				CompiledConstraint(
					temporalMin,
					ValidatorBackedRunner(
						io.ghaylan.validata.constraint.validator.temporal.min.TemporalMinValidator,
						temporalMin,
					),
					0,
				),
				CompiledConstraint(
					numericMin,
					ValidatorBackedRunner(
						io.ghaylan.validata.constraint.validator.number.min.NumberMinValidator,
						numericMin,
					),
					1,
				),
				CompiledConstraint(distinct, ValidatorBackedRunner(DistinctValidator, distinct), 2),
			),
		)
		val schema = Schema<Any>()
		PropertyOpenApiExtensionsWriter.write(schema, prop, groups)
		@Suppress("UNCHECKED_CAST")
		val constraints = schema.extensions[ConstraintExtensionKeys.CONSTRAINTS] as List<Map<String, Any?>>
		assertThat(constraints.map { it[ConstraintExtensionKeys.CONSTRAINT_KIND] }).containsExactly("Min", "Min", "Distinct")
		assertThat(constraints[0]["value"]).isEqualTo("2020-02-29")
		assertThat(constraints[1]["value"]).isEqualTo("5")
		assertThat(constraints[2]).containsOnlyKeys(ConstraintExtensionKeys.CONSTRAINT_KIND)
	}
	
	@Test
	@DisplayName("Compare ref becomes confirm_secret via ownerSchema externalName")
	fun compareRefUsesJsonPropertyWireName() {
		val equalMeta = CompareConstraint(
			ref = "confirmSecret",
			operation = Compare.Operation.EQ,
			message = "",
			groups = groups,
		)
		val owner = ObjectSchema(
			type = Any::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "secret",
					externalName = "secret",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { null },
					constraints = listOf(
						CompiledConstraint(
							equalMeta,
							ValidatorBackedRunner(
								io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator,
								equalMeta,
							),
							0,
						),
					),
				),
				PropertySpec(
					declaredName = "confirmSecret",
					externalName = "confirm_secret",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { null },
				),
			),
		)
		val secret = owner.properties[0]
		val schema = Schema<Any>()
		PropertyOpenApiExtensionsWriter.write(schema, secret, groups, owner)
		@Suppress("UNCHECKED_CAST")
		val constraints = schema.extensions[ConstraintExtensionKeys.CONSTRAINTS] as List<Map<String, Any?>>
		val compare = constraints.single { it[ConstraintExtensionKeys.CONSTRAINT_KIND] == "Compare" }
		assertThat(compare["ref"]).isEqualTo("confirm_secret")
	}
}
