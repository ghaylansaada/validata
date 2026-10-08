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

/**
 * Synthetic holder for stacked [ConstraintArg] markers on one parameter.
 *
 * Authors do **not** write this annotation. Mark the parameter with one or more `@ConstraintArg(…)`
 * declarations (see [ConstraintArg]); Kotlin emits a [ConstraintArgs] container under the hood via
 * `@JvmRepeatable` so the JVM sees a single repeatable group.
 *
 * Rules live only on each nested [ConstraintArg] (`kinds`, `target`, `message`). This type carries
 * no extra semantics — it exists so the platform can represent several [ConstraintArg] on the same
 * host.
 *
 * @property value The [ConstraintArg] markers applied to the same annotation parameter.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConstraintArgs(vararg val value: ConstraintArg)