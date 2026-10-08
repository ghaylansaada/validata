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
package io.ghaylan.validata.processor.analyze

/**
 * Effective `@Validate` flags copied into endpoint IR for runtime policy.
 *
 * Defaults match the annotation defaults (`oneErrorPerParam = true`, `OnDefault` group).
 *
 * @property oneErrorPerParam when `true`, at most one constraint error per parameter per pass
 * @property failFast when `true`, stop validating further constraints after the first failure
 * @property groupsFqcn fully qualified validation group class names (never empty — defaults to
 *   `OnDefault`)
 * 
 * @author Ghaylan Saada
 */
internal data class ValidateFlags(
	val oneErrorPerParam: Boolean,
	val failFast: Boolean,
	val groupsFqcn: List<String>,
)
