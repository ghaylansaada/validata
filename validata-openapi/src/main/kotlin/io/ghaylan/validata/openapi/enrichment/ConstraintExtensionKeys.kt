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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.openapi.docs.ConstraintDocumentation

/**
 * Vendor-extension keys published on each property / parameter schema for Validata IR.
 * 
 * @author Ghaylan Saada
 */
object ConstraintExtensionKeys {
	
	/**
	 * Array of constraint entry maps for this property/param.
	 *
	 * Each entry starts with [CONSTRAINT_KIND] (annotation short name), then non-empty args
	 * (**no** `message` / `groups`). Every Validata constraint on the property/shape is listed,
	 * including those that also map to native OpenAPI facets (`Size`, numeric `Min`, …).
	 */
	const val CONSTRAINTS: String = "x-validata-constraints"
	
	/**
	 * Discriminant key inside each [CONSTRAINTS] entry — annotation short name (`Size`, `Max`, …).
	 *
	 * Underscore-prefixed so it cannot collide with a future constraint arg named `type` / `name`.
	 */
	const val CONSTRAINT_KIND: String = "_constraint"
	
	/**
	 * Sorted distinct `{ code, message }` entries for this property/param:
	 * `VALUE_TYPE_MISMATCH` ∪ validators’ `possibleErrorCodes` ∪ `@ApiError` ∪ `VALUE_MISSING` when required.
	 */
	const val ERRORS: String = "x-validata-errors"
	
	/**
	 * Standard constraint types that had **no** [ConstraintDocumentation] owner and no OpenAPI mapper.
	 *
	 * Value is a **sorted distinct list** of metadata class names. Owned constraints that decline
	 * native facets (e.g. temporal `@Min`) are **not** listed here.
	 */
	const val UNMAPPED: String = "x-validata-unmapped"
}
