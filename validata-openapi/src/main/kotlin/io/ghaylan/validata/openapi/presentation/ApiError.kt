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
package io.ghaylan.validata.openapi.presentation

import io.ghaylan.validata.model.ConstraintErrorDefinition
import kotlin.reflect.KClass

/**
 * Docs-only declaration of one possible machine-readable error code for OpenAPI.
 *
 * Use this for codes **not** emitted by Validata validators (manual checks, business
 * exceptions). Does **not** change runtime validation or exception handling.
 *
 * Place on the DTO field or handler parameter that can fail when a wire path exists.
 *
 * [catalog] must be an `enum class` that implements [ConstraintErrorDefinition]
 * (enforced by `validata-processor` and `validata-intellij`). [code] must equal one of
 * that enum’s constant names. When [message] is blank, OpenAPI / KSP fill it from the
 * matching catalog entry’s [ConstraintErrorDefinition.message]; a non-blank [message]
 * overrides that default for documentation only.
 *
 * Repeatable via Kotlin `@Repeatable` — stack several `@ApiError` on the same host; there is no
 * container annotation to write or discover.
 *
 * ### Example
 *
 * ```kotlin
 * enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
 *     EMAIL_TAKEN("Email already registered");
 *     override val code: String get() = name
 * }
 *
 * data class CreateUserRequest(
 *     @field:ApiError(code = "EMAIL_TAKEN", catalog = UserErrors::class)
 *     val email: String?,
 * )
 * ```
 *
 * @property code Machine code selector (enum constant name, e.g. `EMAIL_TAKEN`). Must not be blank.
 * @property message Optional OpenAPI override; blank → use the catalog entry’s message.
 * @property catalog Enum catalog implementing [ConstraintErrorDefinition].
 * 
 * @author Ghaylan Saada
 */
@Repeatable
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
annotation class ApiError(
	val code: String,
	val message: String = "",
	val catalog: KClass<out ConstraintErrorDefinition>)
