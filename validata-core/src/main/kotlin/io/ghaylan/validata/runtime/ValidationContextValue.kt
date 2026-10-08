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
package io.ghaylan.validata.runtime

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Wraps a value with the IR metadata needed for cross-field and cross-element rule lookups (T-25).
 *
 * @param T Runtime value type (DTO instance, normalized list of array elements, …).
 * @property value Runtime value (DTO instance, normalized list of array elements, …).
 * @property objectSchema Schema of a structured object — the container for cross-field reads, or the
 *   *element* schema when [value] is a list of objects under Distinct.
 * @property shape Shape of this value; drives Distinct's scalar/map/object branching without `TypeInfo`.
 * 
 * @author Ghaylan Saada
 */
data class ValidationContextValue<T>(
	val value: T?,
	val objectSchema: ObjectSchema? = null,
	val shape: TypeShape? = null)