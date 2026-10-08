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
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.size.ArraySizeValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.constraint.validator.size.CollectionSizeValidator
import io.ghaylan.validata.constraint.validator.size.MapSizeValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import kotlin.reflect.KClass

/**
 * Asserts that the length or element count of the annotated value falls within an inclusive range,
 * e.g. password length or the number of items in a list.
 *
 * Applies to the **direct** annotated type only:
 * - [CharSequence] length
 * - [Collection] / [Map] sizes
 * - JVM arrays (`Array`, `IntArray`, …)
 *
 * A field typed `LocalDate` (or other non-sizeable types) is rejected at compile time — there is
 * no catch-all `Any` size validator. `null` values are skipped; combine with [Required] when the
 * field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Size(min = 8, max = 64)
 * val password: String
 *
 * @field:Size(min = 1, max = 10)
 * val tags: List<String>
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.TEXT_TOO_SHORT]
 * - [ConstraintErrorCode.TEXT_TOO_LONG]
 * - [ConstraintErrorCode.COLLECTION_TOO_SMALL]
 * - [ConstraintErrorCode.COLLECTION_TOO_LARGE]
 * - [ConstraintErrorCode.OBJECT_TOO_SMALL]
 * - [ConstraintErrorCode.OBJECT_TOO_LARGE]
 *
 * @property min The minimum allowable element or character count (inclusive). Defaults to `0`.
 * @property max The maximum allowable element or character count (inclusive). Defaults to [Int.MAX_VALUE].
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [
	CharSequenceSizeValidator::class,
	CollectionSizeValidator::class,
	MapSizeValidator::class,
	ArraySizeValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Size(
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val min: Int = 0,
	
	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val max: Int = Int.MAX_VALUE,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)