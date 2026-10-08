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
 * Executes one constraint against a value (framework-free half of [CompiledConstraint]).
 *
 * Path and HTTP-section enrichment belong on the engine wire model (`ConstraintError` in validata-core),
 * not on [ConstraintFailure].
 * */
fun interface ConstraintRunner {
	
	/**
	 * Evaluates [value] against [config].
	 *
	 * No I/O or mutation beyond reading [value] and [config].
	 *
	 * @param value property or element under validation; may be `null`
	 * @param config typed configuration for this constraint occurrence
	 * @return failure description, or `null` when the value satisfies the constraint	 
	 */
	fun run(
		value: Any?,
		config: ConstraintConfig
	): ConstraintFailure?
}
