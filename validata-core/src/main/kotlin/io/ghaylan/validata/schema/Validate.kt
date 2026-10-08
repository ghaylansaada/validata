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
package io.ghaylan.validata.schema

import io.ghaylan.validata.exception.ConstraintViolationException
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.groups.OnUpdate
import io.ghaylan.validata.model.ConstraintError
import kotlin.reflect.KClass

/**
 * Marks a type or handler for schema-driven validation.
 *
 * Spring-free marker. The Spring Boot host wires two uses:
 *
 * - **HTTP handlers** — on a controller method or class (method wins). Present `@RequestBody`,
 *   `@RequestParam`, `@PathVariable`, and `@RequestHeader` args are validated against pre-compiled
 *   schemas before controller logic. Violations become [ConstraintError]s and surface as
 *   [ConstraintViolationException].
 * - **Configuration** — on a `@ConfigurationProperties` class. The host validates the bound bean at
 *   startup so bad YAML / env / cloud config fails before traffic is accepted.
 *
 * Group markers such as [OnDefault], [OnCreate], and [OnUpdate] filter which constraints run;
 * custom [KClass] tokens via [groups]. Fail-fast options apply to HTTP walks (and config when the
 * host passes them through).
 *
 * @property oneErrorPerParam When `true`, stop after the first violation on each param.
 * @property failFast When `true`, stop after the first violation anywhere — remaining params and
 *    HTTP sections may be skipped. Opt-in; defaults to `false`. Distinct from [oneErrorPerParam].
 * @property groups Active validation groups; only constraints in an active group run. Defaults to
 *    `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Validate(
	val oneErrorPerParam: Boolean = true,
	val failFast: Boolean = false,
	val groups: Array<KClass<*>> = [OnDefault::class])
