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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.constraint.spi.ConstraintCatalog
import kotlin.reflect.KClass

/**
 * Meta-annotation that binds a validation annotation to its [ConstraintValidator] implementations.
 *
 * Any custom validation annotation (e.g. `@Email`, `@OddYears`) must carry this marker. At compile
 * time, `validata-processor` discovers every class with it and emits a generated
 * `{AnnotationSimpleName}Constraint` metadata class (same package as the annotation) plus a
 * [ConstraintCatalog] ServiceLoader entry.
 *
 * ## Author contract
 * Every constraint is **two** authored declarations:
 * 1. **Annotation** — payload params with [ConstraintArg] / [PropertyRef] as needed, plus exactly
 *    one [ConstraintMessage] and one [ConstraintGroups]
 * 2. **Validator class(es)** — [ConstraintValidator] implementations listed in [validatedBy];
 *    type parameter `V` is the accepted non-null subject type (`null` is handled by
 *    [ConstraintValidator.validateNull], default skip)
 *
 * Metadata is **generated** — do not hand-write a `*Constraint` data class. KSP and the IntelliJ
 * plugin discover rules from markers on the annotation parameters.
 *
 * ### Container-Level vs. Element-Level Targeting
 * Whether a constraint applies to a collection or to its elements is decided by **where the
 * annotation is written**, not by any property of the constraint itself:
 *
 * ```kotlin
 * @field:Size(min = 1)                       // the list must hold at least one tag
 * val tags: List<@Size(min = 3) String>?     // each tag must be at least three characters
 * ```
 *
 * ### Example
 *
 * ```kotlin
 * @Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
 * @Retention(AnnotationRetention.RUNTIME)
 * @Constraint(validatedBy = [StringSizeValidator::class, CollectionSizeValidator::class])
 * annotation class Size(
 *   @ConstraintArg(ConstraintArgKind.NON_NEGATIVE) val min: Int = 0,
 *   @ConstraintArg(ConstraintArgKind.NON_NEGATIVE) val max: Int = Int.MAX_VALUE,
 *   @ConstraintMessage val message: String = "",
 *   @ConstraintGroups val groups: Array<KClass<*>> = [OnDefault::class])
 * ```
 *
 * @property validatedBy Validator implementations for this constraint. Multiple validators may
 *   cover different runtime types. **Required** — empty `validatedBy` fails KSP and IDE checks.
 * 
 * @author Ghaylan Saada
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Constraint(
	val validatedBy: Array<KClass<out ConstraintValidator<out Any, out ConstraintMetadata>>>,
)
