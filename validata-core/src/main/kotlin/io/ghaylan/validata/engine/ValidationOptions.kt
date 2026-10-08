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
package io.ghaylan.validata.engine

import io.ghaylan.validata.groups.OnDefault
import kotlin.reflect.KClass

/**
 * Options for standalone [ValidatorEngine.validate] runs.
 *
 * Prefer this over the boolean/group vararg overload when adding new call sites — the flag combo
 * stays documented in one place. Existing boolean overloads delegate here (non-breaking).
 *
 * @property oneErrorPerParam When `true`, at most one error per param path.
 * @property failFast When `true`, stop the whole walk after the first error.
 * @property groups Active validation groups (defaults to `[OnDefault]`).
 * 
 * @author Ghaylan Saada
 */
data class ValidationOptions(
	val oneErrorPerParam: Boolean = true,
	val failFast: Boolean = false,
	val groups: Array<KClass<*>> = arrayOf(OnDefault::class),
) {
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ValidationOptions) return false
		return oneErrorPerParam == other.oneErrorPerParam && failFast == other.failFast && groups.contentEquals(other.groups)
	}
	
	override fun hashCode(): Int {
		var result = oneErrorPerParam.hashCode()
		result = 31 * result + failFast.hashCode()
		result = 31 * result + groups.contentHashCode()
		return result
	}
}
