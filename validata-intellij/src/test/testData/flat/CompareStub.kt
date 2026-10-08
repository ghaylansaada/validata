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
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind

/**
 * Test stub mirroring library `@Compare` with `@PropertyRef` on [ref].
 */
@Constraint(validatedBy = [CompareValidator::class])
annotation class Compare(
	@PropertyRef(compatibility = PropertyRefCompatibilityKind.COMPARABLE_FAMILY)
	@ConstraintArg(ConstraintArgKind.NOT_BLANK)
	val ref: String,
	val operation: Operation,
	val message: String = "",
) {
	enum class Operation {
		GT,
		GTE,
		LT,
		LTE,
		EQ,
		NE,
	}
}

data class CompareConstraint(
	@PropertyRef(compatibility = PropertyRefCompatibilityKind.COMPARABLE_FAMILY)
	val ref: String,
	val operation: Operation,
	val message: String = "",
)

object CompareValidator : ConstraintValidator<Comparable<*>, CompareConstraint>()
