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
package io.ghaylan.validata.schema.support.metadata

import io.ghaylan.validata.schema.constraint.ConstraintConfig
import io.ghaylan.validata.schema.constraint.ConstraintFailure
import io.ghaylan.validata.schema.constraint.ConstraintRunner

/**
 * Test [ConstraintRunner] that fails only when `value` is `null`.
 *
 * Exercises the IR runner contract without pulling in the Spring `ConstraintValidator` hierarchy.*
 * 
 * @author Ghaylan Saada
 */
object RequiredRunner : ConstraintRunner {

	/**
	 * @return [ConstraintFailure] with code `VALUE_MISSING` when [value] is null; otherwise `null`
	 */
	override fun run(value: Any?, config: ConstraintConfig): ConstraintFailure? {
		// config is unused: this stub always uses a fixed code for assertion stability.
		return if (value == null) ConstraintFailure(code = "VALUE_MISSING") else null
	}
}
