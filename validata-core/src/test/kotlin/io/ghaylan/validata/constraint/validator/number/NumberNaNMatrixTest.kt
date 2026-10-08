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
package io.ghaylan.validata.constraint.validator.number

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.validator.number.coordinate.CoordinateValidator
import io.ghaylan.validata.constraint.validator.number.max.NumberMaxValidator
import io.ghaylan.validata.constraint.validator.number.min.NumberMinValidator
import io.ghaylan.validata.constraint.validator.number.multiple.MultipleOfValidator
import io.ghaylan.validata.constraint.validator.number.numberparity.NumberParityValidator
import io.ghaylan.validata.constraint.validator.number.range.NumberRangeValidator
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Matrix: non-finite [Double] values must fail across numeric bound / parity / geo validators.
 *
 * Bound/parity validators use [ConstraintErrorCode.VALUE_PARSING_FAILED] (same as [NumberSignValidator]).
 * Geo validators reject via range check with [ConstraintErrorCode.NUMBER_OUT_OF_RANGE].
 * 
 * @author Ghaylan Saada
 */
class NumberNaNMatrixTest {
	
	private val groups = ValidatorTestSupport.defaultGroups
	
	private data class Case(
		val name: String,
		val validator: ConstraintValidator<out Number, out ConstraintMetadata>,
		val constraint: ConstraintMetadata,
		val expectedCode: ConstraintErrorDefinition = ConstraintErrorCode.VALUE_PARSING_FAILED,
	)
	
	private val cases: List<Case> = listOf(
		Case("NumberMinValidator", NumberMinValidator, MinConstraint("0", true, "", groups)),
		Case("NumberMaxValidator", NumberMaxValidator, MaxConstraint("100", true, "", groups)),
		Case(
			"NumberRangeValidator",
			NumberRangeValidator,
			RangeConstraint("0", "100", true, true, false, "", groups),
		),
		Case("MultipleOfValidator", MultipleOfValidator, MultipleOfConstraint("2", "", groups)),
		Case("NumberParityValidator", NumberParityValidator, NumberParityConstraint(NumberParity.Value.EVEN, "", groups)),
		Case(
			"CoordinateValidator latitude",
			CoordinateValidator,
			CoordinateConstraint(Coordinate.Axis.LATITUDE, "", groups),
			ConstraintErrorCode.NUMBER_OUT_OF_RANGE,
		),
		Case(
			"CoordinateValidator longitude",
			CoordinateValidator,
			CoordinateConstraint(Coordinate.Axis.LONGITUDE, "", groups),
			ConstraintErrorCode.NUMBER_OUT_OF_RANGE,
		),
	)
	private val nonFinite: List<Pair<String, Double>> = listOf(
		"NaN" to Double.NaN,
		"POSITIVE_INFINITY" to Double.POSITIVE_INFINITY,
	)
	
	@TestFactory
	@DisplayName("non-finite Doubles are rejected")
	fun nonFiniteMatrix(): List<DynamicTest> = cases.flatMap { case ->
		nonFinite.map { (label, value) ->
			@Suppress("UNCHECKED_CAST")
			val validator = case.validator as ConstraintValidator<Number, ConstraintMetadata>
			dynamicTest("${case.name} rejects $label with ${case.expectedCode}") {
				assertInvalid(
					validator,
					value,
					case.constraint,
					case.expectedCode,
				)
			}
		}
	}
}
