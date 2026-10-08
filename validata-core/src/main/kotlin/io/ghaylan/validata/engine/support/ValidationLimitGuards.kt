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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Depth / size / error-budget guards extracted from [ValidatorEngine].
 *
 * Stateless helpers; [ValidatorEngine.limits] is passed per call so engines remain shareable
 * singletons. These failures are engine limits (not annotation constraints), so
 * [ConstraintError.metadata] stays `null` and the message carries the useful bound.
 *
 * @author Ghaylan Saada
 */
internal object ValidationLimitGuards {

	/**
	 * Emits a depth-limit error and returns `true` when [ValidationContext.depth] exceeds
	 * [ValidationLimits.maxDepth].
	 *
	 * Mutates [errors] when the depth ceiling is exceeded.
	 *
	 * @param limits Per-run ceilings from the host engine.
	 * @param context Current walk cursor.
	 * @param errors Mutable error accumulator.
	 * @return `true` when the walk must stop descending at this node.
	 */
	fun rejectIfTooDeep(
		limits: ValidationLimits,
		context: ValidationContext,
		errors: MutableList<ConstraintError<*>>,
	): Boolean {
		if (context.depth <= limits.maxDepth) return false

		errors += ConstraintError(
			path = context.fieldPath,
			code = ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED,
			message = "Structure exceeds the permitted nesting depth of ${limits.maxDepth}.",
		)

		return true
	}

	/**
	 * Emits a collection-size error and returns `true` when [size] exceeds
	 * [ValidationLimits.maxElementsPerContainer].
	 *
	 * Mutates [errors] when the size ceiling is exceeded.
	 *
	 * @param limits Per-run ceilings from the host engine.
	 * @param size Observed container size.
	 * @param context Current walk cursor.
	 * @param errors Mutable error accumulator.
	 * @return `true` when the walk must not iterate the container.
	 */
	fun rejectIfTooLarge(
		limits: ValidationLimits,
		size: Int,
		context: ValidationContext,
		errors: MutableList<ConstraintError<*>>,
	): Boolean {
		if (size <= limits.maxElementsPerContainer) return false

		errors += ConstraintError(
			path = context.fieldPath,
			code = ConstraintErrorCode.COLLECTION_TOO_LARGE,
			message = "Collection has $size items; at most ${limits.maxElementsPerContainer} are permitted.",
		)

		return true
	}

	/**
	 * Whether the walk should stop collecting more violations.
	 *
	 * Honors [failFast] (whole-request abort after the first error) and [ValidationLimits.maxErrors].
	 * No mutation.
	 *
	 * @param limits Per-run ceilings from the host engine.
	 * @param errors Errors collected so far.
	 * @param failFast Whole-request fail-fast flag for this run.
	 * @return `true` when callers should stop walking.
	 */
	fun shouldAbortWalk(
		limits: ValidationLimits,
		errors: List<ConstraintError<*>>,
		failFast: Boolean,
	): Boolean = (failFast && errors.isNotEmpty()) || errors.size >= limits.maxErrors
}
