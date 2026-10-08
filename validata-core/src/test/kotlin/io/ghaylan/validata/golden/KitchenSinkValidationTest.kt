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
package io.ghaylan.validata.golden

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.annotation.Compare
import io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator
import io.ghaylan.validata.constraint.validator.number.min.NumberMinValidator
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.engine.support.NaturalPathOrder
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * L4 golden: multi-constraint object walked by [ValidatorEngine].
 *
 * Uses [EngineTestSupport.bodyRequestSchema] so the walk goes through the real request path
 * without requiring KSP-generated schemas for the fixture type.
 * 
 * @author Ghaylan Saada
 */
class KitchenSinkValidationTest {
	
	data class Signup(
		val email: String?,
		val password: String?,
		val confirm: String?,
		val age: Int?,
	)
	
	private val groups = setOf(OnDefault::class)
	
	private fun <M: ConstraintMetadata> compiled(
		meta: M,
		validator: ConstraintValidator<*, M>,
		order: Int,
	) = CompiledConstraint(
		metadata = meta,
		runner = ValidatorBackedRunner(validator, meta),
		order = order,
	)
	
	private fun signupSchema(): ObjectSchema {
		val required = compiled(RequiredConstraint(Required.Mode.STRICT, "", groups), RequiredValidator, 0)
		val email = compiled(EmailConstraint("", groups), EmailValidator, 1)
		val size = compiled(SizeConstraint(8, 64, "", groups), CharSequenceSizeValidator, 1)
		val equalTo = compiled(
			CompareConstraint("password", Compare.Operation.EQ, "", groups),
			CompareValidator,
			2,
		)
		val minAge = compiled(MinConstraint("18", true, "", groups), NumberMinValidator, 1)
		
		return ObjectSchema(
			type = Signup::class.java,
			properties = listOf(
				PropertySpec(
					"email",
					"email",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as Signup).email },
					constraints = listOf(required, email),
				),
				PropertySpec(
					"password",
					"password",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as Signup).password },
					constraints = listOf(required, size),
				),
				PropertySpec(
					"confirm",
					"confirm",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as Signup).confirm },
					constraints = listOf(required, equalTo),
				),
				PropertySpec(
					"age",
					"age",
					ScalarShape(ScalarKind.INTEGRAL),
					ValueReader { (it as Signup).age },
					constraints = listOf(required, minAge),
				),
			),
		)
	}
	
	@Test
	@DisplayName("valid signup yields no errors")
	fun validSignup() {
		val engine = EngineTestSupport.engine()
		val endpoint = EngineTestSupport.bodyRequestSchema(
			body = signupSchema(),
			failFast = false,
			oneErrorPerParam = false,
			id = "golden-signup-valid",
		)
		val errors = engine.validateRequest(
			schema = endpoint,
			body = Signup("a@b.com", "password1", "password1", 21),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).isEmpty()
	}
	
	@Test
	@DisplayName("invalid signup reports expected codes and paths")
	fun invalidSignup() {
		val engine = EngineTestSupport.engine()
		val endpoint = EngineTestSupport.bodyRequestSchema(
			body = signupSchema(),
			failFast = false,
			oneErrorPerParam = false,
			id = "golden-signup-invalid",
		)
		val errors = engine.validateRequest(
			schema = endpoint,
			body = Signup("bad", "short", "other", 15),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).isNotEmpty()
		assertThat(errors.map { it.path }).contains("email", "password", "confirm", "age")
		assertThat(errors.map { it.code }).contains(
			ConstraintErrorCode.VALUE_FORMAT_INVALID,
			ConstraintErrorCode.TEXT_TOO_SHORT,
			ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL,
			ConstraintErrorCode.NUMBER_TOO_SMALL,
		)
		val paths = errors.map { it.path.orEmpty() }
		assertThat(paths).isSortedAccordingTo(Comparator(NaturalPathOrder::compare))
	}
}
