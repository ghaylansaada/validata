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

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.constraint.ConstraintConfig
import io.ghaylan.validata.schema.constraint.ConstraintFailure
import io.ghaylan.validata.schema.constraint.ConstraintRunner

/**
 * Bridges a [ConstraintValidator] into the IR's [ConstraintRunner].
 *
 * The live engine path calls [execute], which has a full [ValidationContext] for groups, paths, and
 * cross-field reads. [run] remains for IR-only call sites that have no context; it cannot enrich path
 * or location and must not be used for validators that need sibling lookups.
 *
 * @property validator Typed validator implementation (usually a Kotlin `object` from the catalog).
 * @property metadata Constraint arguments for this compiled binding.*
 * 
 * @author Ghaylan Saada
 */
class ValidatorBackedRunner(
	val validator: ConstraintValidator<*, *>,
	val metadata: ConstraintMetadata
): ConstraintRunner, ContextAwareConstraintRunner {
	
	/**
	 * Runs [validator] with a full engine context (groups, path, cross-field).
	 *
	 * Delegates to [ConstraintValidator.runValidation]; may mutate the returned [ConstraintError]
	 * (`path`, `message`) on failure. No registry or cache writes.
	 *
	 * @param value Raw property value under validation.
	 * @param context Active validation cursor.
	 * @return Enriched [ConstraintError] on violation; `null` when valid or skipped.	 
	 */
	override fun execute(
		value: Any?,
		context: ValidationContext
	): ConstraintError<*>? = validator.runValidation(
		value = value,
		constraint = metadata,
		context = context)
	
	/**
	 * IR [ConstraintRunner] hook — **not** supported for live engine validation.
	 *
	 * Always throws: validators need a [ValidationContext] for path, groups, and sibling reads.
	 * Call [execute] from [ValidatorEngine] instead.
	 *
	 * @param value Ignored.
	 * @param config Ignored.
	 * @return Never returns normally.
	 * @throws IllegalStateException Always — use [execute] with a [ValidationContext].	 
	 */
	override fun run(
		value: Any?,
		config: ConstraintConfig
	): ConstraintFailure? {
		error("ValidatorBackedRunner.run needs a ValidationContext. Use execute(value, context) from ValidatorEngine.")
	}
}
