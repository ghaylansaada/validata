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
package io.ghaylan.validata.schema.constraint

/**
 * Minimal failure payload produced by a [ConstraintRunner].
 *
 * Smaller than the public wire error (`ConstraintError`) in validata-core: no path and no HTTP
 * section (the engine stamps those on `ConstraintError`).
 *
 * @property code Stable machine-readable code (typically an enum name such as `VALUE_MISSING`), or `null` when unset
 * @property message Human-readable detail; blank/`null` falls back to [ConstraintConfig.message] or a code default
 * @property metadata Constraint metadata or structured args that produced the failure, or `null` when unused
 *
 * @author Ghaylan Saada
 */
data class ConstraintFailure(
	val code: String? = null,
	val message: String? = null,
	val metadata: Any? = null,
)
