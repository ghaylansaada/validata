/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.constraint.annotation

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.PropertyRef
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget

/**
 * Test stub mirroring library `@RequiredWhen` with nested [Condition] and gate-typed
 * `value` / `values` markers.
 * 
 * @author Ghaylan Saada
 */
@Constraint(validatedBy = [RequiredWhenValidator::class])
annotation class RequiredWhen(
	@PropertyRef
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val ref: String,
	val condition: Condition,
	@ConstraintArg(ConstraintArgKind.TYPED_LITERAL)
	val value: String = "",
	@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
	@ConstraintArg(
		ConstraintArgKind.NOT_BLANK,
		ConstraintArgKind.TYPED_LITERAL,
		target = ConstraintArgTarget.ELEMENT,
	)
	val values: Array<String> = [],
) {
	
	enum class Condition { MISSING,
		PRESENT,
		EQ,
		NE,
		`IN`,
		NIN,
		GT,
		LT,
		GTE,
		LTE,
	}
}

data class RequiredWhenConstraint(
	@PropertyRef
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val ref: String,
	@ConstraintArg(ConstraintArgKind.TYPED_LITERAL)
	val value: String = "",
	@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
	@ConstraintArg(
		ConstraintArgKind.NOT_BLANK,
		ConstraintArgKind.TYPED_LITERAL,
		target = ConstraintArgTarget.ELEMENT,
	)
	val values: Set<String> = emptySet(),
)

object RequiredWhenValidator: ConstraintValidator<Any, RequiredWhenConstraint>()
