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
 * Inputs for [CascadeDecision.decide] — prefer named construction over positional booleans.
 *
 * @property markedValidatable `true` when the nested type carries `@Validatable`
 * @property noCascade `true` when cascade is suppressed at the usage site
 * @property sameCompilation `true` when the nested declaration is in the current compilation
 * @property strictCrossModuleCascade processor flag — errors instead of warns for unmarked
 *   cross-module cascades
 * 
 * @author Ghaylan Saada
 */
internal data class CascadeFacts(
	val markedValidatable: Boolean,
	val noCascade: Boolean,
	val sameCompilation: Boolean,
	val strictCrossModuleCascade: Boolean)