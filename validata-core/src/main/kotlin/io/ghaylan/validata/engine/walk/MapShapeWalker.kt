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
package io.ghaylan.validata.engine.walk

import io.ghaylan.validata.engine.ValidationCursor
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.schema.shape.MapShape

/**
 * Map entry walker extracted from [ValidatorEngine].
 *
 * Key errors are recorded at `path.keys[key]`, value errors at `path[key]` (ADR-6 / Phase 1).
 * Public engine API is unchanged — this is an internal size-split of the schema walk.*
 * 
 * @author Ghaylan Saada
 */
internal object MapShapeWalker {
	
	/**
	 * Traverses a map under [shape], mutating [context] (push/pop per key and value) and appending
	 * violations to [errors].
	 *
	 * @param engine Host engine providing depth/size guards and nested cascade.
	 * @param map Map instance, or `null` / non-map (no entry walk).
	 * @param shape IR map shape (key/value types + container constraints).
	 * @param context Mutable walk cursor.
	 * @param errors Mutable error accumulator.	 
	 */
	fun walk(
		engine: ValidatorEngine,
		map: Any?,
		shape: MapShape,
		context: ValidationCursor,
		errors: MutableList<ConstraintError<*>>,
	) {
		if (engine.rejectIfTooDeep(context, errors)) return
		val entries = (map as? Map<*, *>) ?: return
		
		if (engine.rejectIfTooLarge(entries.size, context, errors)) return
		
		engine.validateCompiled(
			value = map,
			constraints = shape.constraints,
			context = context,
			errors = errors)
		
		entries.forEach { (key, value) ->
			if (engine.shouldAbortWalk(errors, context.failFast)) return
			val keyLabel = key?.toString() ?: "null"
			val preserveArrayForValues = context.array != null
			
			context.pushMapKeys(keyLabel = keyLabel, keyShape = shape.key)
			
			try {
				engine.cascadeIntoShape(
					value = key,
					shape = shape.key,
					context = context,
					errors = errors,
					preserveArrayContextForNestedObject = false)
			}
			finally {
				context.pop()
			}
			
			context.pushMapValue(keyLabel = keyLabel, valueShape = shape.value)
			
			try {
				engine.cascadeIntoShape(
					value = value,
					shape = shape.value,
					context = context,
					errors = errors,
					preserveArrayContextForNestedObject = preserveArrayForValues)
			}
			finally {
				context.pop()
			}
		}
	}
}
