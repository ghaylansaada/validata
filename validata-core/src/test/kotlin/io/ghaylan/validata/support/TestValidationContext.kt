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
package io.ghaylan.validata.support

import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.runtime.AttributeBag
import io.ghaylan.validata.runtime.PathSegment
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.shape.TypeShape
import java.time.Clock
import kotlin.reflect.KClass

/**
 * Minimal [ValidationContext] for unit-testing validators without the engine cursor.
 *
 * @property path Lazy path rope for error localization.
 * @property fieldName Wire name of the field under test.
 * @property shape IR shape of the current field, if needed by the validator.
 * @property oneErrorPerParam Mirrors engine fail-fast per param (usually unused in tests).
 * @property failFast Mirrors engine fail-fast per request (usually unused in tests).
 * @property groups Active validation groups for group-filtering tests.
 * @property skipGroupChecks When `true`, simulates the engine group fast-path.
 * @property elementIndex Collection element index, or [ValidationContext.NO_ELEMENT_INDEX].
 * @property array Sibling list metadata for cross-element constraints (e.g. `@Distinct`).
 * @property containerObject Parent object metadata for sibling property reads.
 * @property depth Nesting depth from the validation root.
 * @property attributeBag Shared attribute cache for the synthetic run.
 * 
 * @author Ghaylan Saada
 */
data class TestValidationContext(
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
	override val clock: Clock = Clock.systemDefaultZone(),
): ValidationContext
