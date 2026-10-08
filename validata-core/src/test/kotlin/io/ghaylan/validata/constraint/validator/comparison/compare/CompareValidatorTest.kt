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
package io.ghaylan.validata.constraint.validator.comparison.compare

import io.ghaylan.validata.constraint.annotation.Compare
import io.ghaylan.validata.constraint.annotation.CompareConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.SiblingContexts
import io.ghaylan.validata.support.TestValidationContext
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CompareValidator")
class CompareValidatorTest {

	private fun c(
		ref: String = "minAge",
		operation: Compare.Operation = Compare.Operation.GT,
	) = CompareConstraint(ref, operation, "", ValidatorTestSupport.defaultGroups)

	@Test
	fun skipsNull() {
		assertSkipsNull(CompareValidator, c())
	}

	@Test
	fun greaterPassesWhenAboveSibling() {
		val ctx = SiblingContexts.ages(minAge = 10, maxAge = 11, field = "maxAge")
		assertValid(CompareValidator, 11, c(ref = "minAge"), ctx)
	}

	@Test
	fun greaterFailsWhenEqual() {
		val ctx = SiblingContexts.ages(minAge = 10, maxAge = 10, field = "maxAge")
		val constraint = c(ref = "minAge")
		assertInvalid(
			CompareValidator,
			10,
			constraint,
			ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL,
			constraint,
			ctx,
		)
	}

	@Test
	fun equalRequiresMatch() {
		val ctx = SiblingContexts.passwordConfirm(password = "secret", confirm = "secret")
		val constraint = c(ref = "password", operation = Compare.Operation.EQ)
		assertValid(CompareValidator, "secret", constraint, ctx)
	}

	@Test
	fun notEqualPassesWhenDifferent() {
		val ctx = SiblingContexts.passwordConfirm(password = "secret", confirm = "other")
		val constraint = c(ref = "password", operation = Compare.Operation.NE)
		assertValid(CompareValidator, "other", constraint, ctx)
	}
}
