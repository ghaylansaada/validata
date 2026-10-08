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
package io.ghaylan.validata.processor.verify

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget

/**
 * One `@ConstraintArg` rule on a metadata / annotation constructor parameter.
 *
 * A parameter may contribute several hosts (repeatable markers with different [target]s).
 *
 * @property parameterName Annotation argument name (e.g. `value`, `values`, `by`).
 * @property kinds Tooling rules to apply at the usage site for [target].
 * @property target Where [kinds] apply ([ConstraintArgTarget.VALUE] or [ConstraintArgTarget.ELEMENT]).
 * @property message Optional diagnostic prefix from `@ConstraintArg(message = …)`.
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintArgHost(
	val parameterName: String,
	val kinds: Set<ConstraintArgKind>,
	val target: ConstraintArgTarget = ConstraintArgTarget.VALUE,
	val message: String = "",
)
