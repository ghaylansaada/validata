/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.model

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget

/**
 * One `@ConstraintArg` rule on a metadata / annotation constructor parameter.
 *
 * IDE twin of processor `ConstraintArgHost` (`validata-processor`). Produced by
 * `ConstraintArgAttributeDiscovery` from `@ConstraintArg` / `@ConstraintArgs` on the annotation
 * declaration or the generated `{Name}Constraint` class. Keep field shape aligned with the
 * processor host so diagnostics stay interchangeable.
 *
 * A parameter may contribute **several** hosts (repeatable markers with different [target]s), e.g.
 * `NON_EMPTY` on [ConstraintArgTarget.VALUE] plus `NOT_BLANK` + `TYPED_LITERAL` on
 * [ConstraintArgTarget.ELEMENT] for `@In.values`. Annotators merge hosts that share a target
 * before applying [kinds].
 *
 * Consumed by `ConstraintLiteralAnnotator` (blank / empty / typed-literal / numeric / regex checks)
 * and by enum / regex injectors that key off [ConstraintArgKind.TYPED_LITERAL] /
 * [ConstraintArgKind.REGEX] in [kinds].
 *
 * Defaults: [target] is [ConstraintArgTarget.VALUE]; [message] is empty (tooling then uses each
 * kind’s default diagnostic text). Empty [kinds] should not occur after successful parse — discovery
 * drops markers that yield no recognizable kind names.
 *
 * Not a `@PropertyRef` path host — see [PropertyRefMetadataHost] / [PropertyRefHostAttribute].
 * Coexistence: the same parameter may carry both; `NOT_BLANK` on a path host makes blank strings
 * errors instead of “no reference”.
 *
 * @property parameterName Must match the usage-site annotation argument name (`value`, `values`, `by`, …).
 * @property kinds Tooling rules to apply for [target]; may contain multiple [ConstraintArgKind] entries (first failure wins in the annotator).
 * @property target Where [kinds] apply — argument-as-a-whole vs each collection element. Defaults to [ConstraintArgTarget.VALUE].
 * @property message Optional diagnostic prefix from `@ConstraintArg(message = …)`; empty means use the kind’s default message in tooling.
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintArgMetadataHost(
	val parameterName: String,
	val kinds: Set<ConstraintArgKind>,
	val target: ConstraintArgTarget = ConstraintArgTarget.VALUE,
	val message: String = ""
)
