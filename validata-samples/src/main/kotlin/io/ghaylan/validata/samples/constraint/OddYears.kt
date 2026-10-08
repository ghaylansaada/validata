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

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.groups.OnDefault
import kotlin.reflect.KClass

/**
 * Sample custom constraint: the annotated number must be an odd integer.
 *
 * Option 2: this annotation + [OddYearsValidator] only; [OddYearsConstraint] is KSP-generated
 * and registered on the catalog SPI. Failures use [OddYearsError.YEAR_NOT_ODD], not a built-in
 * format code.
 *
 * ```kotlin
 * @field:Required
 * @field:OddYears
 * val oddYear: Int?
 * ```
 *
 * @property message Override for [OddYearsError.YEAR_NOT_ODD]; blank keeps the enum default
 *   (the validator still supplies a value-specific message).
 * @property groups Groups that activate this constraint. Default is [OnDefault].
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [OddYearsValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class OddYears(
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class])
