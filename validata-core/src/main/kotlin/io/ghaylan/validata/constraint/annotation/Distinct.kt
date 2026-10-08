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
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import io.ghaylan.validata.schema.ref.PropertyRefScope
import kotlin.reflect.KClass

/**
 * Requires list or array elements to be unique — whole elements, or keyed by element fields.
 *
 * **Placement:** type-use on the **collection element** only (inside `<>`), never on the list
 * property itself. Failures are reported on indexed element paths (e.g. `contacts[1]`).
 *
 * Use on contacts, line items, tags, and similar collections. Applies to `List`/`Set`/array
 * element types that are scalars, maps, or objects. Scalars compare by value; collections with
 * fewer than 2 elements always pass. Each [by] entry must be a non-blank, single-segment field
 * name on the element type (e.g. `"email"` on `List<ContactDto>`); unknown names and dotted
 * paths such as `"address.city"` are rejected at compile time / in the IDE. Prefer generated
 * Fields constants when available. `null` elements are skipped; combine with [Required] when
 * presence is also required.
 *
 * ```kotlin
 * // No two contacts may share the same email — error path contacts[i]
 * val contacts: List<@Distinct(by = ["email"]) ContactDto>
 *
 * // Whole-element uniqueness for scalars
 * val tags: List<@Distinct String>
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.COLLECTION_DUPLICATE]
 *
 * @property by Single-segment field names on each element. Empty (default) = compare whole
 *    elements (by value for scalars, or all-field equality for maps/objects).
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@Repeatable
@MustBeDocumented
@Constraint(validatedBy = [DistinctValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Distinct(
	@PropertyRef(scope = PropertyRefScope.ELEMENT)
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val by: Array<String> = [],
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)
