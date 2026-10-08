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
package io.ghaylan.validata.constraint.annotation

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.number.numberparity.NumberParityValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import kotlin.reflect.KClass

/**
 * Requires a [Number] to be an even or odd whole integer, per [value].
 *
 * The subject must first be a finite whole number: `NaN` and infinities fail as unparseable, and
 * a fractional value (`4.5`, `2.50` with a non-zero scale) fails as a non-integer before parity
 * is considered. `2.0` is accepted as even. `null` values are skipped; combine with [Required]
 * when the field must also be present.
 *
 * ### Examples
 *
 * ```kotlin
 * // 2, 4, 2.0 pass; 3 and 2.5 fail
 * @field:NumberParity(NumberParity.Value.EVEN)
 * val seatsPerRow: Int
 *
 * @field:NumberParity(NumberParity.Value.ODD)
 * val teamSize: Int
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_PARSING_FAILED]
 * - [ConstraintErrorCode.NUMBER_NOT_INTEGER]
 * - [ConstraintErrorCode.NUMBER_NOT_EVEN]
 * - [ConstraintErrorCode.NUMBER_NOT_ODD]
 *
 * @property value Required parity of the annotated number.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [NumberParityValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class NumberParity(
	val value: Value,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	
	/**
	 * Required parity of the annotated whole number.
	 */
	enum class Value {
		
		/**
		 * Divisible by two.
		 */
		EVEN,
		
		/**
		 * Not divisible by two.
		 */
		ODD,
	}
}
