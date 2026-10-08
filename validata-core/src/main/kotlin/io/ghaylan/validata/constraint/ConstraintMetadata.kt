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

import io.ghaylan.validata.schema.constraint.ConstraintConfig
import kotlin.reflect.KClass

/**
 * Immutable configuration extracted from a constraint annotation at schema-build time.
 *
 * Subclasses mirror the properties of their corresponding annotation. KSP emits constraint
 * instances as constructor-call literals in generated schemas (object graphs and endpoint params).
 *
 * Implements [ConstraintConfig] so the same instance can sit in the schema IR without a wrapper (T-24).
 *
 * @author Ghaylan Saada
 */
abstract class ConstraintMetadata: ConstraintConfig {

	/**
	 * Optional override for the validator's default error message.
	 *
	 * Blank means use the error-code default at validation time.
	 */
	abstract override val message: String

	/**
	 * Active validation groups for this constraint instance.
	 *
	 * Empty means the constraint always runs (subject to engine group filtering).
	 */
	abstract override val groups: Set<KClass<*>>
}
