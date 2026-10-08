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
package io.ghaylan.validata.samples.matrix

import io.ghaylan.validata.constraint.annotation.Distinct
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.schema.Validatable

/**
 * Nested element of [AllConstraintsRequest.items] for [Distinct] uniqueness-by-property
 * (`by = ["code"]`).
 *
 * @property code Unique SKU within the parent list; required
 * @property label Human-readable item name; required
 * 
 * @author Ghaylan Saada
 */
@Validatable
data class AllConstraintsLineItem(
	@field:Required
	val code: String?,
	
	@field:Required
	val label: String?)
