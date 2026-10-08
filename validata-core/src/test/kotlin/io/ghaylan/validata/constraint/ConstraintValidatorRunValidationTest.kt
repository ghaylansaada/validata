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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.constraint.annotation.AssertConstraint
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.bool.assert.AssertValidator
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.TestValidationContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Group filtering and path enrichment on [ConstraintValidator.runValidation].
 * 
 * @author Ghaylan Saada
 */
class ConstraintValidatorRunValidationTest {
	
	@Test
	@DisplayName("skipGroupChecks bypasses group intersection")
	fun skipGroupChecks() {
		val constraint = required(groups = setOf(OnCreate::class))
		val ctx = TestValidationContext(groups = setOf(OnDefault::class), skipGroupChecks = true)
		val error = RequiredValidator.runValidation(null, constraint, ctx)
		assertThat(error?.code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		assertThat(error?.path).isEqualTo("field")
	}
	
	@Test
	@DisplayName("non-intersecting groups skip validation")
	fun groupMismatchSkips() {
		val constraint = required(groups = setOf(OnCreate::class))
		val ctx = TestValidationContext(groups = setOf(OnDefault::class), skipGroupChecks = false)
		assertThat(RequiredValidator.runValidation(null, constraint, ctx)).isNull()
	}
	
	@Test
	@DisplayName("intersecting groups run validation")
	fun groupMatchRuns() {
		val constraint = required(groups = setOf(OnDefault::class, OnCreate::class))
		val ctx = TestValidationContext(groups = setOf(OnDefault::class))
		assertThat(RequiredValidator.runValidation(null, constraint, ctx)?.code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
	}
	
	@Test
	@DisplayName("empty context groups with non-empty constraint groups skip")
	fun emptyContextGroupsSkip() {
		val constraint = required(groups = setOf(OnDefault::class))
		val ctx = TestValidationContext(groups = emptySet())
		assertThat(RequiredValidator.runValidation(null, constraint, ctx)).isNull()
	}
	
	@Test
	@DisplayName("custom constraint message wins over validator default")
	fun customMessage() {
		val constraint = required(message = "please provide")
		val error = RequiredValidator.runValidation(null, constraint, TestValidationContext())
		assertThat(error?.message).isEqualTo("please provide")
	}
	
	@Test
	@DisplayName("default validateNull skips null for value constraints")
	fun valueConstraintsSkipNull() {
		val error = AssertValidator.runValidation(
			value = null,
			constraint = AssertConstraint(
				value = false,
				message = "",
				groups = setOf(OnDefault::class),
			),
			context = TestValidationContext(),
		)
		assertThat(error).isNull()
	}
	
	private fun required(
		message: String = "",
		groups: Set<kotlin.reflect.KClass<*>> = setOf(OnDefault::class),
	) = RequiredConstraint(
		mode = Required.Mode.STRICT,
		message = message,
		groups = groups,
	)
}
