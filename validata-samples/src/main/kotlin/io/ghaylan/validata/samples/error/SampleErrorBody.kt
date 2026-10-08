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
package io.ghaylan.validata.samples.error

import io.ghaylan.validata.exception.ConstraintViolationException
import io.ghaylan.validata.model.ConstraintError

/**
 * Sample app-owned HTTP error JSON (not a Validata type).
 *
 * [ValidationExceptionHandler] maps [ConstraintViolationException] and [SampleBusinessException]
 * into this shape.
 *
 * @property status HTTP status written on the envelope
 * @property code Machine code when the failure is a catalog/business error; `null` for
 *   constraint-only 400s that only populate [errors]
 * @property message Human-readable summary. Constraint 400s use the exception message —
 *   never a request secret.
 * @property errors Field-attributed constraint failures; empty for business errors
 * 
 * @author Ghaylan Saada
 */
data class SampleErrorBody(
	val status: Int,
	val code: String? = null,
	val message: String,
	val errors: List<ConstraintError<*>> = emptyList())
