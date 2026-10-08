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
 * A constraint bound to a runner and ready to execute, in declaration order.
 *
 * Stored as a [List]: KSP emits literal construction; the engine iterates without re-sorting.
 *
 * @property metadata Typed configuration for this occurrence ([ConstraintConfig])
 * @property runner Executable that interprets [metadata] against a value
 * @property order Zero-based position among constraints of the same owner; producers sort `@Required` first
 * 
 * @author Ghaylan Saada
 */
data class CompiledConstraint(
	val metadata: ConstraintConfig,
	val runner: ConstraintRunner,
	val order: Int)
