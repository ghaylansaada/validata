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

import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget

/**
 * Marks a [ConstraintMetadata] / `@Constraint` annotation parameter whose value is a constraint
 * **argument** that tooling must check at compile time and in the IDE.
 *
 * Analogous to [PropertyRef] (which marks path hosts). Together they let authors declare contracts
 * on metadata without teaching `validata-processor` / `validata-intellij` each annotation by name.
 *
 * ## Repeatable + target
 *
 * Stack several markers on one parameter. Each pairs [kinds] with a single [target]:
 *
 * ```kotlin
 * @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
 * @ConstraintArg(
 *   ConstraintArgKind.NOT_BLANK,
 *   ConstraintArgKind.TYPED_LITERAL,
 *   target = ConstraintArgTarget.ELEMENT)
 * val values: Array<String>
 * ```
 *
 * Both `@Repeatable` and `@JvmRepeatable(ConstraintArgs::class)` are required: `@Repeatable` lets
 * you stack the annotation in Kotlin; `@JvmRepeatable` binds the synthetic holder to [ConstraintArgs]
 * (what KSP and the IntelliJ plugin expand when they see the container). Authors still only write
 * `@ConstraintArg`, never [ConstraintArgs].
 *
 * Default [target] is [ConstraintArgTarget.VALUE] (scalars and collection-as-a-whole).
 *
 * ## Coexistence with [PropertyRef]
 *
 * A parameter may carry both. When a [ConstraintArgKind.NOT_BLANK] rule is present (typically with
 * [ConstraintArgTarget.ELEMENT] for path arrays), blank strings are **errors**. Optional path hosts
 * without `NOT_BLANK` (e.g. optional blank refs) keep blank-means-absent behavior.
 * [RequiredWhen.ref] carries `NOT_BLANK` so the sibling name is mandatory.
 *
 * ## Module
 *
 * Declared in **`validata-core`**. Kind / target enums live in **`validata-schema`**.
 *
 * @property kinds One or more [ConstraintArgKind] rules for this [target].
 * @property target Where to apply [kinds] on the argument ([ConstraintArgTarget.VALUE] or
 *   [ConstraintArgTarget.ELEMENT]).
 * @property message Optional diagnostic prefix; empty uses the kind’s default message in tooling.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@JvmRepeatable(ConstraintArgs::class)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConstraintArg(
	vararg val kinds: ConstraintArgKind,
	val target: ConstraintArgTarget = ConstraintArgTarget.VALUE,
	val message: String = "")
