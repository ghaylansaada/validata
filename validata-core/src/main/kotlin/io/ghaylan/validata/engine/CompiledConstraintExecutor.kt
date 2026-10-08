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
import io.ghaylan.validata.engine.support.ValidationLimitGuards
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Compiled-constraint execution extracted from [ValidatorEngine].
 *
 * Public engine API is unchanged — [ValidatorEngine.validateCompiled] delegates here.*
 * 
 * @author Ghaylan Saada
 */
internal object CompiledConstraintExecutor {

	/**
	 * Runs compiled constraints for the current field, honoring fail-fast flags.
	 *
	 * Mutates [errors] when a constraint fails; may return early under [ValidationContext.oneErrorPerParam]
	 * or [ValidationContext.failFast].
	 *
	 * @param engine Host engine for abort checks against [ValidatorEngine.limits].
	 * @param value Current field value.
	 * @param constraints Compiled constraints on the property or type-use.
	 * @param context Walk context exposed to validators.
	 * @param errors Mutable error accumulator.
	 */
	fun validate(
		engine: ValidatorEngine,
		value: Any?,
		constraints: List<CompiledConstraint>,
		context: ValidationContext,
		errors: MutableList<ConstraintError<*>>,
	) {
		if (constraints.isEmpty()) return

		// Path-scoped: property constraints + type-use constraints share the same wire path.
		if (context.oneErrorPerParam && context.hasFailedPath()) return

		// Per-param oneErrorPerParam *or* whole-request failFast: stop after the first
		// violation on this param so the outer walk can abort.
		if (context.oneErrorPerParam || context.failFast) {
			for (constraint in constraints) {
				val error = execute(constraint, value, context) ?: continue
				errors += error
				if (context.oneErrorPerParam) context.markPathFailed()
				return
			}
		} else {
			for (constraint in constraints) {
				execute(constraint, value, context)?.let(errors::add)
				if (ValidationLimitGuards.shouldAbortWalk(engine.limits, errors, context.failFast)) return
			}
		}
	}

	/**
	 * Executes one compiled constraint via its [ContextAwareConstraintRunner].
	 *
	 * KSP and [compileConstraints] emit [ValidatorBackedRunner] or [CompositionOrRunner]. The hot
	 * path trusts that contract instead of `require` + smart-cast on every constraint. A wrong
	 * runner type fails as [ClassCastException] — covered by a dedicated unit test. No direct
	 * mutation of engine state; the runner may enrich the returned [ConstraintError].
	 *
	 * @param constraint Compiled constraint binding (metadata + runner).
	 * @param value Current field value.
	 * @param context Walk context exposed to the validator.
	 * @return Violation from the runner, or `null` when valid / skipped.
	 * @throws ClassCastException When [constraint]'s runner is not a [ContextAwareConstraintRunner].
	 */
	private fun execute(
		constraint: CompiledConstraint,
		value: Any?,
		context: ValidationContext,
	): ConstraintError<*>? = (constraint.runner as ContextAwareConstraintRunner).execute(value, context)
}
