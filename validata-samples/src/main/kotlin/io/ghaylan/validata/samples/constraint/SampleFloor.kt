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
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import kotlin.reflect.KClass

/**
 * Sample custom inclusive numeric floor — Option 2: annotation + [SampleFloorValidator];
 * [SampleFloorConstraint] is KSP-generated.
 *
 * ```kotlin
 * @field:Required
 * @field:SampleFloor("3")
 * val minAge: Int?
 * ```
 *
 * @property value Minimum bound as a decimal string (same literal rules as `@Min`). Must be
 *   non-blank; KSP rejects an empty string.
 * @property message Override when the floor is violated; blank uses
 *   [ConstraintErrorCode.NUMBER_TOO_SMALL]’s default (do not put the raw input in a custom
 *   message for secret-like fields).
 * @property groups Groups that activate this constraint. Default is [OnDefault].
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [SampleFloorValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class SampleFloor(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
	val value: String,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class])
