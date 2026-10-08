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
package io.ghaylan.validata.samples.constraint

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.ext.toBigDecimalOrNull
import io.ghaylan.validata.ext.toConstraintNumber
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Inclusive numeric floor using [SampleFloorConstraint.value] as a decimal string.
 *
 * Mirrors `@Min` number semantics. Null is skipped. An unparsable bound or subject fails closed
 * with [ConstraintErrorCode.VALUE_PARSING_FAILED] — never treated as valid.*
 * 
 * @author Ghaylan Saada
 */
object SampleFloorValidator: ConstraintValidator<Number, SampleFloorConstraint>() {
	
	/** Format failure or too-small — never a silent pass on parse errors.	 */
	override fun possibleErrorCodes(constraint: SampleFloorConstraint, type: Class<*>): Set<ConstraintErrorDefinition> = setOf(
		ConstraintErrorCode.VALUE_PARSING_FAILED,
		ConstraintErrorCode.NUMBER_TOO_SMALL)
	
	/**
	 * @param value subject; never `null` (null is skipped by the engine)
	 * @param constraint generated metadata whose [SampleFloorConstraint.value] is the floor
	 * @param context per-call cursor — not retained
	 * @return `null` when [value] is `>=` the floor; otherwise a format or too-small error	 
	 */
	override fun validate(
		value: Number,
		constraint: SampleFloorConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val min = constraint.value.toConstraintNumber()
			?: return ConstraintError(code = ConstraintErrorCode.VALUE_PARSING_FAILED)
		
		val actual = value.toBigDecimalOrNull()
			?: return ConstraintError(code = ConstraintErrorCode.VALUE_PARSING_FAILED)
		
		if (actual >= min) return null
		
		return ConstraintError(
			code = ConstraintErrorCode.NUMBER_TOO_SMALL,
			metadata = constraint,
		)
	}
}
