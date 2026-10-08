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
package io.ghaylan.validata.constraint.validator.number.coordinate

import io.ghaylan.validata.constraint.annotation.Coordinate
import io.ghaylan.validata.constraint.annotation.CoordinateConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CoordinateValidator")
class CoordinateValidatorTest {

	@Test
	fun latitudeInRange() {
		val c = CoordinateConstraint(Coordinate.Axis.LATITUDE, "", ValidatorTestSupport.defaultGroups)
		assertValid(CoordinateValidator, 45.0, c)
		assertInvalid(CoordinateValidator, 91.0, c, ConstraintErrorCode.NUMBER_OUT_OF_RANGE)
	}

	@Test
	fun longitudeInRange() {
		val c = CoordinateConstraint(Coordinate.Axis.LONGITUDE, "", ValidatorTestSupport.defaultGroups)
		assertValid(CoordinateValidator, 120.0, c)
		assertInvalid(CoordinateValidator, -181.0, c, ConstraintErrorCode.NUMBER_OUT_OF_RANGE)
	}
}
