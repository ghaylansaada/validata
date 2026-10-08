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
 * Internal expand mode for composed annotations (maps from
 * `ConstraintComposition.Mode` on `@ConstraintComposition`).
 * 
 * @author Ghaylan Saada
 */
internal enum class CompositionExpandMode {
	
	/**
	 * Flatten nested leaves into the property constraint list (conjunctive).
	 */
	AND,
	
	/**
	 * Emit one synthetic composition site that passes when any active leaf passes.
	 */
	OR,
}
