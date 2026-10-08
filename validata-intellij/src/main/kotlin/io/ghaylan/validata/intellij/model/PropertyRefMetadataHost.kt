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

import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope

/**
 * One metadata / annotation constructor parameter marked `@PropertyRef`.
 *
 * IDE twin of processor `PropertyRefHost` (`validata-processor`). Produced by
 * `PropertyRefAttributeDiscovery` when walking annotation primary-constructor parameters
 * marked `@PropertyRef`, or the same markers on the generated `{Name}Constraint` class.
 * Cached per annotation FQCN via `ConstraintDiscoveryCache`.
 *
 * Call sites that need the usage-site annotation FQCN plus matching argument name wrap this into
 * [PropertyRefHostAttribute] via `PropertyRefAnnotationMatcher`. This type itself is
 * declaration-side only — it does not know which annotation class was annotated.
 *
 * When several `@PropertyRef` markers collapse onto the same [parameterName], discovery merges
 * scopes (ELEMENT wins) and compatibility kinds (prefer non-[PropertyRefCompatibilityKind.NONE],
 * with [PropertyRefCompatibilityKind.COMPARABLE_FAMILY] winning over
 * [PropertyRefCompatibilityKind.SAME_SCALAR_KIND] when both appear).
 *
 * Not a PsiReference. Not a `@ConstraintArg` host — see [ConstraintArgMetadataHost].
 *
 * @property parameterName Must match the usage-site annotation argument name (`property`, `by`, …)
 *   so matchers can bind a string literal’s `KtValueArgument` to this host.
 * @property scope Sibling vs collection-element path resolution
 *   ([PropertyRefScope.SIBLING] vs [PropertyRefScope.ELEMENT]).
 * @property compatibilityKind Scalar rule for sibling paths (and subject-family gates). Element
 *   paths still carry the declared kind; type-check interpreters may ignore it for element scope
 *   today (same as KSP).
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyRefMetadataHost(
	val parameterName: String,
	val scope: PropertyRefScope,
	val compatibilityKind: PropertyRefCompatibilityKind
)
