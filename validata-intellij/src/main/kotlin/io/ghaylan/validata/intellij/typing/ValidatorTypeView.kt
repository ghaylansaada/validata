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

package io.ghaylan.validata.intellij.typing

/**
 * Normalized type for validator ranking — FQCN + args, no IntelliJ resolve at unit-test time.
 *
 * Pure data; ranking helpers live on `ValidatorTypeCompatibility` in `analysis.compat`.
 *
 * @property qualifiedName FQCN or `"*"` when [isWildcard]
 * @property typeArguments nested argument views
 * @property isArray `true` for `kotlin.Array` / primitive arrays
 * @property arrayElement element view when [isArray]
 * @property isWildcard `true` for unresolved / star type arguments ([WILDCARD])
 * @property assignableSupertypes FQCNs this type is treated as assignable-to for ranking
 * 
 * @author Ghaylan Saada
 */
internal data class ValidatorTypeView(
	val qualifiedName: String,
	val typeArguments: List<ValidatorTypeView> = emptyList(),
	val isArray: Boolean = false,
	val arrayElement: ValidatorTypeView? = null,
	val isWildcard: Boolean = false,
	val assignableSupertypes: Set<String> = emptySet(),
) {
	
	companion object {
		
		val WILDCARD: ValidatorTypeView = ValidatorTypeView(qualifiedName = "*", isWildcard = true)
	}
}
