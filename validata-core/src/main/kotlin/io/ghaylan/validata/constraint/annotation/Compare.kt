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
import io.ghaylan.validata.constraint.PropertyRef
import io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import kotlin.reflect.KClass

/**
 * Cross-field ordering or equality: the annotated value must satisfy [operation] against the
 * sibling named by [ref].
 *
 * Both sides must be [Comparable] and of the same comparable family — both numeric, both
 * temporal, or the same scalar kind for text-like leaves (`String`, `Char`, enums, `UUID`).
 * The processor and the IDE plugin reject a mismatch at compile time; at runtime a sibling whose
 * concrete type differs from the subject's yields
 * [ConstraintErrorCode.COMPARISON_NOT_ORDERABLE].
 *
 * `null` subjects are skipped (presence is [Required]). A `null` sibling fails [Operation.EQ] and
 * passes every other operation, so an optional reference does not block the request.
 *
 * Repeatable: several [Compare] on one field all have to hold.
 *
 * ### Examples
 *
 * ```kotlin
 * // Confirmation must match the password field
 * @field:Compare(ref = "password", operation = Compare.Operation.EQ)
 * val passwordConfirmation: String
 *
 * // End date must be strictly after the start date
 * @field:Compare(ref = "startsOn", operation = Compare.Operation.GT)
 * val endsOn: LocalDate
 *
 * // Discount may not exceed the total
 * @field:Compare(ref = "total", operation = Compare.Operation.LTE)
 * val discount: BigDecimal
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.COMPARISON_NOT_ORDERABLE]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_EQUAL]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN]
 * - [ConstraintErrorCode.COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL]
 *
 * @property ref Sibling field or parameter name to compare against. Only single-segment sibling
 *    names are supported; prefer generated Fields constants.
 * @property operation Required ordering the annotated value must satisfy against [ref].
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@Repeatable
@MustBeDocumented
@Constraint(validatedBy = [CompareValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Compare(
	@PropertyRef(compatibility = PropertyRefCompatibilityKind.COMPARABLE_FAMILY)
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val ref: String,
	
	val operation: Operation,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	
	/**
	 * Ordering the annotated value must satisfy against the referenced sibling.
	 *
	 * The annotated value is always the left operand: `GT` reads “annotated greater than [ref]”.
	 */
	enum class Operation {
		
		/**
		 * Annotated value must be strictly greater than the sibling.
		 */
		GT,
		
		/**
		 * Annotated value must be greater than or equal to the sibling.
		 */
		GTE,
		
		/**
		 * Annotated value must be strictly less than the sibling.
		 */
		LT,
		
		/**
		 * Annotated value must be less than or equal to the sibling.
		 */
		LTE,
		
		/**
		 * Annotated value must equal the sibling. A `null` sibling never equals.
		 */
		EQ,
		
		/**
		 * Annotated value must differ from the sibling. A `null` sibling passes.
		 */
		NE,
	}
}
