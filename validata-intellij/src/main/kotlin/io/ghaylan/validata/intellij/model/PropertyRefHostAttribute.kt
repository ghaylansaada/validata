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
 * One discovered property-path host on a **usage-site** constraint annotation argument.
 *
 * Built by `PropertyRefAnnotationMatcher` from `@PropertyRef` discovery
 * ([PropertyRefMetadataHost] on the annotation or generated `{Name}Constraint`) plus the
 * resolved annotation FQCN and the concrete argument parameter name under the caret /
 * reference. There is **no** bootstrap FQCN table — custom constraints light up when authors
 * mark parameters; missing markers mean no IDE path DX.
 *
 * Consumed by:
 * - `PropertyRefReferenceProvider` (scope → owner / element type resolver)
 * - Annotators / completion that need [compatibilityKind] or [parameterName] without re-running
 *   full discovery
 *
 * `PropertyRefAnnotationMatcher.matchAnnotationCompatibility` may synthesize a host whose
 * [compatibilityKind] is the primary sibling non-NONE kind even when picking a representative
 * [parameterName] from the first matching sibling host.
 *
 * Defaults [compatibilityKind] to [PropertyRefCompatibilityKind.NONE] (path existence only) when
 * callers omit it — matching `@PropertyRef`’s library default.
 *
 * Not the declaration-side host alone — that is [PropertyRefMetadataHost] (no annotation FQCN).
 * Not a `@ConstraintArg` literal host — see [ConstraintArgMetadataHost].
 *
 * @property annotationFqName Fully qualified annotation class name when known; may be a
 *   `discovered.<SimpleName>` placeholder when the declaration resolves but FQCN lookup fails.
 * @property parameterName Annotation value-parameter name bound to the string argument
 *   (`property`, `by`, …).
 * @property scope Resolve against owner (sibling) or collection element type.
 * @property compatibilityKind Scalar type check vs the annotated subject (KSP /
 *   `PropertyRefScalarCompatibility` parity). Defaults to [PropertyRefCompatibilityKind.NONE].
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyRefHostAttribute(
	val annotationFqName: String,
	val parameterName: String,
	val scope: PropertyRefScope,
	val compatibilityKind: PropertyRefCompatibilityKind = PropertyRefCompatibilityKind.NONE
)
