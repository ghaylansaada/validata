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

import io.ghaylan.validata.constraint.ConstraintComposition
import io.ghaylan.validata.constraint.ConstraintComposition.Mode
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.annotation.Email
import io.ghaylan.validata.constraint.annotation.Phone
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import kotlin.reflect.KClass

/**
 * Sample composed constraint: value must be a valid email **or** phone number.
 *
 * Outer type is **not** `@Constraint`. Nested leaves expand under
 * [ConstraintComposition] `OR` into one runtime composition site.
 *
 * ```kotlin
 * @field:Required
 * @field:EmailOrPhone
 * val contact: String?
 * ```
 *
 * @property message Override when every active leaf fails; blank uses
 *   [ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE].
 * @property groups Groups that activate the whole OR group. Default is [OnDefault].
 *
 */
@MustBeDocumented
@Email
@Phone
@ConstraintComposition(Mode.OR)
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.TYPE)
annotation class EmailOrPhone(
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class])
