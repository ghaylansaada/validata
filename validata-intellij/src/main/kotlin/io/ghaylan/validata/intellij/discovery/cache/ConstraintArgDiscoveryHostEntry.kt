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

import io.ghaylan.validata.intellij.model.ConstraintArgMetadataHost

/**
 * One cache row for `@ConstraintArg` host discovery keyed by annotation FQCN.
 *
 * Stored in [ConstraintDiscoveryCache]. The [stamp] is the project’s PSI modification count
 * when [hosts] was computed. A later lookup with a different stamp treats the entry as stale.
 *
 * IntelliJ-only memoization — not shared with KSP.
 *
 * @property stamp PSI modification count when [hosts] was produced.
 * @property hosts Discovered `@ConstraintArg` hosts for that annotation FQCN (may be empty).
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintArgDiscoveryHostEntry(
	val stamp: Long,
	val hosts: List<ConstraintArgMetadataHost>,
)
