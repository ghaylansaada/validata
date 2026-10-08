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
 * Pure cascade decision for object-like property / parameter types.
 *
 * Extracted from [ShapeModelBuilder] so warn-vs-error and `@NoCascade` outcomes are unit-testable
 * without a KSP [com.google.devtools.ksp.symbol.KSType].*
 * 
 * @author Ghaylan Saada
 */
internal object CascadeDecision {
	
	/**
	 * Decides how to model an object-like nested type from [facts].
	 *
	 * No side effects.
	 *
	 * @param facts cascade inputs for one object-like subject type
	 * @return [CascadeOutcome] chosen for shape emission and diagnostics	 
	 */
	fun decide(facts: CascadeFacts): CascadeOutcome = when {
		facts.noCascade -> CascadeOutcome.SCALAR_OTHER
		facts.markedValidatable -> CascadeOutcome.OBJECT_REF
		facts.sameCompilation || facts.strictCrossModuleCascade -> CascadeOutcome.ERROR_DYNAMIC
		else -> CascadeOutcome.WARN_OBJECT_REF
	}
}
