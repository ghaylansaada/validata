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
package io.ghaylan.validata.constraint.composition

import io.ghaylan.validata.constraint.ConstraintGroupMatching
import io.ghaylan.validata.engine.ContextAwareConstraintRunner
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.constraint.ConstraintConfig
import io.ghaylan.validata.schema.constraint.ConstraintFailure
import io.ghaylan.validata.schema.constraint.ConstraintRunner

/**
 * Runs nested leaf constraints with OR semantics: succeeds when any **active** child succeeds.
 *
 * Child group skips are detected via [ConstraintGroupMatching] before calling the child runner —
 * a child [ContextAwareConstraintRunner.execute] returning `null` then means success, not skip.
 * When every active child fails, reports [ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE].
 * When no child is active under the current groups, the composition is skipped (`null`).
 *
 * @property composition Nested children and outer message / groups for this site.*
 * 
 * @author Ghaylan Saada
 */
class CompositionOrRunner(
	val composition: CompositionConstraint,
): ConstraintRunner, ContextAwareConstraintRunner {
	
	/**
	 * Evaluates [value] against [composition] children under [context].
	 *
	 * @param value Raw property value; may be `null`.
	 * @param context Active validation cursor.
	 * @return Composition violation when all active children fail; `null` when valid or skipped.	 
	 */
	override fun execute(
		value: Any?,
		context: ValidationContext
	): ConstraintError<*>? {
		if (!ConstraintGroupMatching.shouldRun(composition.groups, context)) return null
		var sawActiveChild = false
		for (child in composition.children) {
			if (!ConstraintGroupMatching.shouldRun(child.metadata.groups, context)) continue
			sawActiveChild = true
			val runner = child.runner as ContextAwareConstraintRunner
			if (runner.execute(value, context) == null) return null
		}
		
		if (!sawActiveChild) return null
		
		return ConstraintError(
			code = ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE,
			path = context.fieldPath,
			message = composition.message.takeIf(String::isNotBlank)
				?: ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE.message,
		)
	}
	
	/**
	 * IR [ConstraintRunner] hook — not supported for live engine validation.
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
		error("CompositionOrRunner.run needs a ValidationContext. Use execute(value, context) from ValidatorEngine.")
	}
}
