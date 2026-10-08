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

package io.ghaylan.validata.intellij.discovery.cache


/**
 * One cache row for `@Constraint(validatedBy = …)` analysis keyed by annotation FQCN.
 *
 * Stored in [ConstraintDiscoveryCache]. Same stamp semantics as [PropertyRefDiscoveryHostEntry].
 *
 * @property stamp PSI modification count when [result] was produced.
 * @property result Outcome of reading `validatedBy` for that annotation FQCN.
 * 
 * @author Ghaylan Saada
 */
internal data class ValidatedByDiscoveryEntry(
	val stamp: Long,
	val result: ConstraintValidatedByResult,
)
