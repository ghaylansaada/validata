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
package io.ghaylan.validata.engine

import io.ghaylan.validata.constraint.composition.CompositionOrRunner
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Engine hot-path runner that needs a [ValidationContext] (groups, path, sibling reads).
 *
 * [ValidatorBackedRunner] and [CompositionOrRunner] implement this.
 * [ValidatorEngine] casts compiled runners to this type; unknown runners fail as [ClassCastException].
 *
 * Lives in `engine` (not `schema.runtime`) so the package DAG has no `schema → runtime` edge
 * (ADR 0001 / audit A.1).
 * */
fun interface ContextAwareConstraintRunner {
	
	/**
	 * Evaluates [value] under [context].
	 *
	 * @param value Raw property value under validation; may be `null`.
	 * @param context Active validation cursor.
	 * @return Enriched violation, or `null` when valid / skipped.	 
	 */
	fun execute(
		value: Any?,
		context: ValidationContext
	): ConstraintError<*>?
}
