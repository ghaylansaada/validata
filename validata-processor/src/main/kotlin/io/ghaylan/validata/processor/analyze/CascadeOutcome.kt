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

import io.ghaylan.validata.processor.model.ObjectRefShapeModel

/**
 * Outcome of cascade policy for one object-like subject type.
 * 
 * @author Ghaylan Saada
 */
internal enum class CascadeOutcome {
	
	/**
	 * Emit [ObjectRefShapeModel].
	 */
	OBJECT_REF,
	
	/**
	 * `@NoCascade` / flat param — emit scalar OTHER with the type FQCN.
	 */
	SCALAR_OTHER,
	
	/**
	 * Same-compilation unmarked (or strict cross-module) — KSP **error** + DynamicShape.
	 */
	ERROR_DYNAMIC,
	
	/**
	 * Cross-module unmarked without strict — KSP **warn** + ObjectRef (runtime lookup).
	 */
	WARN_OBJECT_REF,
}
