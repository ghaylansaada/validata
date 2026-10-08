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
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import java.math.BigDecimal

/**
 * Fails when a non-null value is not an odd integer — proves custom-catalog registration
 * and a sample-owned [ConstraintErrorDefinition].
 *
 * Null is skipped (presence is `@Required`). Non-finite and non-integral numbers fail closed
 * rather than truncating via [Number.toLong].*
 * 
 * @author Ghaylan Saada
 */
object OddYearsValidator: ConstraintValidator<Number, OddYearsConstraint>() {
	
	/** Codes this validator may emit — [OddYearsError.YEAR_NOT_ODD] only.	 */
	override fun possibleErrorCodes(
		constraint: OddYearsConstraint,
		type: Class<*>
	): Set<ConstraintErrorDefinition> = setOf(OddYearsError.YEAR_NOT_ODD)
	
	/**
	 * @param value subject; never `null` (null is skipped by the engine)
	 * @param constraint generated metadata (no arguments besides message/groups)
	 * @param context per-call cursor — not retained
	 * @return `null` when [value] is an odd integer; otherwise [OddYearsError.YEAR_NOT_ODD]	 
	 */
	override fun validate(
		value: Number,
		constraint: OddYearsConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		if (isOddInteger(value)) return null
		return ConstraintError(
			code = OddYearsError.YEAR_NOT_ODD,
			message = OddYearsError.YEAR_NOT_ODD.message,
		)
	}
	
	/**
	 * `true` when [value] is finite and equal to an odd [Long] with no fractional part.
	 *
	 * @param value non-null numeric subject
	 * @return whether the value is an odd integer	 
	 */
	private fun isOddInteger(value: Number): Boolean {
		if (!isFinite(value)) return false
		val asLong = value.toLong()
		return asLong % 2L != 0L && when (value) {
			is Double -> value == asLong.toDouble()
			is Float -> value == asLong.toFloat()
			is BigDecimal -> value.compareTo(BigDecimal.valueOf(asLong)) == 0
			else -> true
		}
	}
	
	/**
	 * @param value non-null numeric subject
	 * @return `false` for NaN / infinities; `true` for other [Number] types	 
	 */
	private fun isFinite(value: Number): Boolean = when (value) {
		is Double -> value.isFinite()
		is Float -> value.isFinite()
		else -> true
	}
}
