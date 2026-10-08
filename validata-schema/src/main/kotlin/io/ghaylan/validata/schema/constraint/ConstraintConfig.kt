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

import kotlin.reflect.KClass

/**
 * Framework-free IR config received by a [ConstraintRunner] (concrete types live in validata-core).
 *
 * Groups use [KClass] from kotlin-stdlib only — this module has no `kotlin-reflect` dependency.
 * 
 * @author Ghaylan Saada
 */
interface ConstraintConfig {
	
	/**
	 * Override for the validator's default message; blank means use the runner or message-resolver default.
	 */
	val message: String
	
	/**
	 * Active group markers; empty means the constraint always runs.
	 *
	 * Compared by class identity against the groups active on a validation run.
	 */
	val groups: Set<KClass<*>>
}
