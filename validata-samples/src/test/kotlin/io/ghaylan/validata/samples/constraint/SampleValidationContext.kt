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
package io.ghaylan.validata.samples.constraint

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.runtime.AttributeBag
import io.ghaylan.validata.runtime.PathSegment
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.shape.TypeShape
import kotlin.reflect.KClass

/**
 * Minimal [ValidationContext] for sample validator unit tests (no engine cursor).
 *
 * @property path Lazy path rope for error localization
 * @property fieldName Wire name of the field under test
 * @property shape IR shape of the current field, if needed
 * @property oneErrorPerParam Unused by these validators
 * @property failFast Unused by these validators
 * @property groups Active groups for [ConstraintValidator.runValidation]
 * @property skipGroupChecks When `true`, simulates the engine group fast-path
 * @property elementIndex Collection index, or [ValidationContext.NO_ELEMENT_INDEX]
 * @property array Sibling list metadata
 * @property containerObject Parent object metadata
 * @property depth Nesting depth from the validation root
 * @property attributeBag Shared attribute cache for the synthetic run
 * 
 * @author Ghaylan Saada
 */
data class SampleValidationContext(
	override val path: PathSegment = PathSegment.Name(PathSegment.Root, "field"),
	override val fieldName: String = "field",
	override val shape: TypeShape? = null,
	override val oneErrorPerParam: Boolean = false,
	override val failFast: Boolean = false,
	override val groups: Set<KClass<*>> = setOf(OnDefault::class),
	override val skipGroupChecks: Boolean = false,
	override val elementIndex: Int = ValidationContext.NO_ELEMENT_INDEX,
	override val array: ValidationContextValue<List<Any>>? = null,
	override val containerObject: ValidationContextValue<Any>? = null,
	override val depth: Int = 0,
	override val attributeBag: AttributeBag = AttributeBag(),
): ValidationContext
