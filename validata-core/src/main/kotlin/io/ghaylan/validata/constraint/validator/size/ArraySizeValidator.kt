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
package io.ghaylan.validata.constraint.validator.size

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.runtime.ValidationContext

/**
 * `@Size` for JVM arrays (`Array<T>` and primitive arrays such as [IntArray]).
 *
 * Declares `V = `[java.lang.Cloneable] because that is the narrowest JDK type shared by every Kotlin/Java
 * array shape. Compile-time / IDE validator ranking treats array subjects as compatible with
 * `Cloneable` so `@Size` on non-array types (for example `LocalDate`) is rejected instead of
 * silently selecting a catch-all `Any` validator.
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Non-array [java.lang.Cloneable]
 * values skip silently. Bounds violations use [ConstraintErrorCode.COLLECTION_TOO_SMALL] /
 * [ConstraintErrorCode.COLLECTION_TOO_LARGE] via [SizeSupport] (attaches the failing constraint).
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 * 
 * @author Ghaylan Saada
 */
object ArraySizeValidator : ConstraintValidator<Cloneable, SizeConstraint>() {
	
	/**
	 * Error codes this validator may emit for [constraint] bounds.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Size metadata; unbounded `min`/`max` defaults omit the matching code.
	 * @return Bound codes that can fire for this configuration.
	 */
	override fun possibleErrorCodes(
		constraint: SizeConstraint,
		type: Class<*>,
	) = SizeSupport.possibleErrorCodes(
		constraint = constraint,
		underMinimumCode = ConstraintErrorCode.COLLECTION_TOO_SMALL,
		exceedsMaximumCode = ConstraintErrorCode.COLLECTION_TOO_LARGE,
	)
	
	/**
	 * Validates [value] against [constraint].
	 *
	 * Null subjects are skipped by the engine (presence is `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation carrying the failing [SizeConstraint], or `null` when valid.	 
	 */
	override fun validate(
		value: Cloneable,
		constraint: SizeConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val length = SizeSupport.arrayLength(value) ?: return null
		return SizeSupport.validateBounds(
			size = length,
			constraint = constraint,
			underMinimumCode = ConstraintErrorCode.COLLECTION_TOO_SMALL,
			exceedsMaximumCode = ConstraintErrorCode.COLLECTION_TOO_LARGE,
		)
	}
}
