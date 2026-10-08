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
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.shape.*

/**
 * Nested IR cascade extracted from [ValidatorEngine].
 *
 * Public engine API is unchanged — [ValidatorEngine.cascadeIntoShape] delegates here.*
 * 
 * @author Ghaylan Saada
 */
internal object ShapeCascade {
	
	/**
	 * Cascades into nested IR after property-level constraints have run.
	 *
	 * Mutates [context] and [errors] according to the nested shape walk.
	 *
	 * @param engine Host engine providing compiled constraints and nested walkers.
	 * @param value Live property / element / map value.
	 * @param shape IR shape describing [value].
	 * @param context Mutable walk cursor.
	 * @param errors Mutable error accumulator.
	 * @param preserveArrayContextForNestedObject When `true`, nested objects keep [ValidationContext.array].
	 */
	fun cascade(
		engine: ValidatorEngine,
		value: Any?,
		shape: TypeShape,
		context: ValidationCursor,
		errors: MutableList<ConstraintError<*>>,
		preserveArrayContextForNestedObject: Boolean,
	) {
		when (shape) {
			is IterableShape -> IterableShapeWalker.walk(
				engine = engine,
				params = value,
				shape = shape,
				context = context,
				errors = errors)
			
			is MapShape -> MapShapeWalker.walk(
				engine = engine,
				map = value,
				shape = shape,
				context = context,
				errors = errors)
			
			is ObjectRefShape -> {
				engine.validateCompiled(
					value = value,
					constraints = shape.constraints,
					context = context,
					errors = errors)
				
				if (value != null) {
					ObjectSchemaWalker.walk(
						engine = engine,
						param = value,
						schema = shape.ref.value,
						context = context,
						errors = errors,
						preserveArrayContext = preserveArrayContextForNestedObject)
				}
			}
			
			is DynamicShape -> {
				engine.validateCompiled(
					value = value,
					constraints = shape.constraints,
					context = context,
					errors = errors)
				
				if (value != null) {
					engine.validationRegistry.tryResolveObjectSchema(value.javaClass)?.let { resolved ->
						ObjectSchemaWalker.walk(
							engine = engine,
							param = value,
							schema = resolved,
							context = context,
							errors = errors,
							preserveArrayContext = preserveArrayContextForNestedObject)
					}
				}
			}
			
			is ScalarShape -> engine.validateCompiled(value, shape.constraints, context, errors)
		}
	}
}
