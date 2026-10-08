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
package io.ghaylan.validata.constraint.validator.number.numbersign

import io.ghaylan.validata.constraint.annotation.NumberSign
import io.ghaylan.validata.constraint.annotation.NumberSignConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NumberSignValidator")
class NumberSignValidatorTest {

	@Test
	fun positiveRejectsNegative() {
		val c = NumberSignConstraint(NumberSign.Sign.POSITIVE, false, "", ValidatorTestSupport.defaultGroups)
		assertValid(NumberSignValidator, 1, c)
		assertInvalid(NumberSignValidator, -1, c, ConstraintErrorCode.NUMBER_NOT_POSITIVE)
	}

	@Test
	fun negativeRejectsPositive() {
		val c = NumberSignConstraint(NumberSign.Sign.NEGATIVE, false, "", ValidatorTestSupport.defaultGroups)
		assertValid(NumberSignValidator, -1, c)
		assertInvalid(NumberSignValidator, 1, c, ConstraintErrorCode.NUMBER_NOT_NEGATIVE)
	}
}
