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
package io.ghaylan.validata.processor.model

import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind

/**
 * One constraint occurrence bound to a property or a type-use shape node.
 *
 * ## What gets generated
 * Leaf sites: [metadataConstructorCall] + [validatorExpression] become a [CompiledConstraint]
 * via `CompiledConstraints.of`. OR composition sites set [compositionChildren] and emit
 * `CompiledConstraints.or(CompositionConstraint(…), …)` instead.
 *
 * ## Refs for compile-time checks
 * [siblingRefs] / [elementRefs] are **not** emitted into generated code; they exist so
 * PropertyReferenceVerifier can fail the build on typos / type mismatches before the code is written.
 *
 * @property metadataConstructorCall Fully rendered Kotlin call, e.g. `…RequiredConstraint(…)`.
 *   Unused when [compositionChildren] is non-null.
 * @property validatorExpression Fully qualified validator reference (Kotlin `object` or class name).
 *   Unused when [compositionChildren] is non-null.
 * @property order Zero-based execution order among siblings (`@Required` sorted first by the builder)
 * @property siblingRefs Paths from metadata `@PropertyRef` sibling fields (single-segment only)
 * @property elementRefs Paths from metadata `@PropertyRef(scope = ELEMENT)` fields (single-segment)
 * @property compatibilityKind Scalar type-check rule from sibling `@PropertyRef.compatibility`
 *   (element refs are existence-only today)
 * @property annotationSimpleName Short annotation name for diagnostics (e.g. `GreaterThan`)
 * @property compositionChildren When non-null, this site is an OR composition of these leaf models
 * @property compositionMessageExpr Kotlin expression for the outer composition `message`
 * @property compositionGroupsExpr Kotlin expression for the outer composition `groups`
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintModel(
	val metadataConstructorCall: String,
	val validatorExpression: String,
	val order: Int,
	val siblingRefs: List<String> = emptyList(),
	val elementRefs: List<String> = emptyList(),
	val compatibilityKind: PropertyRefCompatibilityKind = PropertyRefCompatibilityKind.NONE,
	val annotationSimpleName: String = "",
	val compositionChildren: List<ConstraintModel>? = null,
	val compositionMessageExpr: String = "\"\"",
	val compositionGroupsExpr: String = "setOf(io.ghaylan.validata.groups.OnDefault::class)",
)
