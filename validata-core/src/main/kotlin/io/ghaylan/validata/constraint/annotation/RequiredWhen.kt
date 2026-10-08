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
import io.ghaylan.validata.constraint.validator.required.RequiredWhenValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import kotlin.reflect.KClass

/**
 * Conditional presence: the annotated value must be present **when** a sibling [ref] matches
 * [condition].
 *
 * Uses the same type-aware [Required.Mode] rules as [Required] for gate presence
 * ([Condition.MISSING] / [Condition.PRESENT]) and for the annotated value when the gate matches.
 * Only **single-segment** sibling names are supported; prefer generated Fields constants.
 *
 * Repeatable: several [RequiredWhen] on one field combine with **OR** — required if **any** gate
 * matches.
 *
 * ### Examples
 *
 * ```kotlin
 * // Required when email is missing
 * @RequiredWhen(ref = "email", condition = RequiredWhen.Condition.MISSING)
 * val phone: String?
 *
 * // Required when contactType equals "PHONE"
 * @RequiredWhen(ref = "contactType", condition = RequiredWhen.Condition.EQ, value = "PHONE")
 * val phone: String?
 *
 * // Required when status is one of …
 * @RequiredWhen(ref = "status", condition = RequiredWhen.Condition.IN, values = ["OPEN", "PENDING"])
 * val assignee: String?
 *
 * // Required when the sibling amount exceeds a threshold
 * @RequiredWhen(ref = "amount", condition = RequiredWhen.Condition.GT, value = "1000")
 * val approverId: String?
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_MISSING]
 * - [ConstraintErrorCode.TEXT_BLANK]
 * - [ConstraintErrorCode.VALUE_EMPTY]
 *
 * @property ref Sibling field or parameter name that gates this rule (same role as
 *    [Compare.ref]).
 * @property condition How [ref]’s runtime value is interpreted as a gate ([Condition]).
 * @property value Literal compared against the gate for [Condition.EQ], [Condition.NE],
 *    [Condition.GT], [Condition.LT], [Condition.GTE], and [Condition.LTE] (string form of the
 *    gate value, same convention as [In] / [NotIn]). Ignored otherwise. Defaults to `""`.
 *    [ConstraintArgKind.TYPED_LITERAL] checks use the **gate** ([ref]) type, not the annotated
 *    field, and only for the conditions that read this argument.
 * @property values Literals for [Condition.IN] / [Condition.NIN]. Ignored otherwise. Same
 *    gate-typed checks when [condition] is IN or NIN.
 * @property mode Presence strictness for [Condition.MISSING] / [Condition.PRESENT] on the gate and
 *    for the annotated value when enforcing. Defaults to [Required.Mode.STRICT].
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@Repeatable
@MustBeDocumented
@Constraint(validatedBy = [RequiredWhenValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class RequiredWhen(
	@PropertyRef
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val ref: String,
	
	val condition: Condition,
	
	@ConstraintArg(ConstraintArgKind.TYPED_LITERAL)
	val value: String = "",
	
	@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
	val values: Array<String> = [],
	
	val mode: Required.Mode = Required.Mode.STRICT,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
) {
	
	/**
	 * Gate predicate for [RequiredWhen]: when the sibling named by [RequiredWhen.ref] matches,
	 * the annotated value must be present.
	 *
	 * [MISSING] / [PRESENT] read the gate through [Required.Mode]. Every other entry compares the
	 * gate’s string form (`toString()`) against [RequiredWhen.value] or [RequiredWhen.values]; a
	 * `null` gate never matches those. The ordering entries ([GT], [LT], [GTE], [LTE]) compare
	 * numerically when both sides parse as decimals and fall back to lexicographic order
	 * otherwise.
	 */
	enum class Condition {
		
		/**
		 * Required when the gate is missing (per [RequiredWhen.mode]).
		 */
		MISSING,
		
		/**
		 * Required when the gate is present (per [RequiredWhen.mode]).
		 */
		PRESENT,
		
		/**
		 * Required when the gate’s string form equals [RequiredWhen.value]. `null` never equals.
		 */
		EQ,
		
		/**
		 * Required when the gate is non-null and its string form differs from
		 * [RequiredWhen.value].
		 */
		NE,
		
		/**
		 * Required when the gate’s string form is in [RequiredWhen.values]. `null` never matches.
		 */
		IN,
		
		/**
		 * Required when the gate is non-null and its string form is not in [RequiredWhen.values].
		 */
		NIN,
		
		/**
		 * Required when the gate orders strictly after [RequiredWhen.value]. `null` never matches.
		 */
		GT,
		
		/**
		 * Required when the gate orders strictly before [RequiredWhen.value]. `null` never
		 * matches.
		 */
		LT,
		
		/**
		 * Required when the gate orders at or after [RequiredWhen.value]. `null` never matches.
		 */
		GTE,
		
		/**
		 * Required when the gate orders at or before [RequiredWhen.value]. `null` never matches.
		 */
		LTE,
	}
}
