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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.engine.ValidationCursor
import io.ghaylan.validata.runtime.PathSegment
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Mutable snapshot slot reused across [ValidationCursor] push/pop cycles within one run.
 *
 * Path components live in the cursor's parallel arrays (not here) so success-path push/pop
 * does not allocate [PathSegment]. Slots are retained after
 * [ValidationCursor.pop] so deeper walks do not re-allocate frames.
 * 
 * @author Ghaylan Saada
 */
internal class ValidationCursorFrame {
	
	/**
	 * Field name at the time the frame was saved.
	 */
	var fieldName: String = ""
	
	/**
	 * IR shape at the time the frame was saved.
	 */
	var shape: TypeShape? = null
	
	/**
	 * Iterable element index, or [ValidationContext.NO_ELEMENT_INDEX].
	 */
	var elementIndex: Int = ValidationContext.NO_ELEMENT_INDEX
	
	/**
	 * Array wrapper visible to sibling-aware validators.
	 */
	var array: ValidationContextValue<List<Any>>? = null
	
	/**
	 * Parent object / section container wrapper.
	 */
	var containerObject: ValidationContextValue<Any>? = null
	
	/**
	 * Nesting depth at the time the frame was saved.
	 */
	var depth: Int = 0
}
