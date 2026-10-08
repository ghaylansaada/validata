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

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition

/**
 * App-owned codes for [OddYears] — even or non-integral values are not a “number format” failure.
 *
 * [ConstraintError] requires an `enum` that implements [ConstraintErrorDefinition].
 *
 * @property message Default user-facing message when the constraint leaves `message` blank.
 * 
 * @author Ghaylan Saada
 */
enum class OddYearsError(
	override val message: String,
): ConstraintErrorDefinition {
	
	/**
	 * Value is even, non-integral, or non-finite. Message templates include the offending value
	 * (years are not secrets) and the expected fix.
	 */
	YEAR_NOT_ODD("Must be an odd integer — even and fractional values are not accepted.");
	
	override val code: String get() = name
}
