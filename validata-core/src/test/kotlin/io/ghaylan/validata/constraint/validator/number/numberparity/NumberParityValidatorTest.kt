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
package io.ghaylan.validata.constraint.validator.number.numberparity

import io.ghaylan.validata.constraint.annotation.NumberParity
import io.ghaylan.validata.constraint.annotation.NumberParityConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NumberParityValidator")
class NumberParityValidatorTest {

	@Test
	fun evenAndOdd() {
		val even = NumberParityConstraint(NumberParity.Value.EVEN, "", ValidatorTestSupport.defaultGroups)
		val odd = NumberParityConstraint(NumberParity.Value.ODD, "", ValidatorTestSupport.defaultGroups)
		assertValid(NumberParityValidator, 4, even)
		assertInvalid(NumberParityValidator, 3, even, ConstraintErrorCode.NUMBER_NOT_EVEN)
		assertValid(NumberParityValidator, 3, odd)
		assertInvalid(NumberParityValidator, 4, odd, ConstraintErrorCode.NUMBER_NOT_ODD)
	}
}
