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
package io.ghaylan.validata.constraint

/**
 * Marks a **composed** annotation and selects how nested `@Constraint` meta-annotations combine.
 *
 * Place this on a non-`@Constraint` annotation that carries leaf constraints as meta-annotations:
 *
 * ```kotlin
 * @Email
 * @Phone
 * @ConstraintComposition(ConstraintComposition.Mode.OR)
 * @Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
 * annotation class EmailOrPhone
 * ```
 *
 * The outer type must **not** declare `@Constraint`. [Mode.AND] (default) preserves
 * flatten-to-AND expand. [Mode.OR] compiles to one synthetic composition constraint
 * that succeeds when any leaf passes.
 *
 * @property value Combination mode for nested leaf constraints. Defaults to [Mode.AND].
 * 
 * @author Ghaylan Saada
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ConstraintComposition(
	val value: Mode = Mode.AND,
) {
	
	/**
	 * How nested `@Constraint` meta-annotations on a composed annotation combine.
	 *
	 * Absent marker (or [AND]) keeps Validata’s historical flatten-to-AND expand.
	 * [OR] emits one synthetic composition constraint at runtime.
	 */
	enum class Mode {
		
		/**
		 * Every nested leaf constraint must pass (default / historical expand).
		 */
		AND,
		
		/**
		 * At least one nested leaf constraint must pass.
		 */
		OR,
	}
}
