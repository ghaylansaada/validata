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
import io.ghaylan.validata.engine.fastpath.ArrayContextFastPath
import io.ghaylan.validata.internal.CollectionUtils
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.shape.*

/**
 * Iterable element walker extracted from [ValidatorEngine].
 *
 * Empty iterables skip the element loop; container-level constraints on the iterable itself still
 * run. Public engine API is unchanged — this is an internal size-split of the schema walk.*
 * 
 * @author Ghaylan Saada
 */
internal object IterableShapeWalker {

	/**
	 * Traverses an iterable under [shape], mutating [context] (push/pop per element) and appending
	 * violations to [errors].
	 *
	 * @param engine Host engine providing depth/size guards, compiled constraints, and nested cascade.
	 * @param params Iterable / array / collection value, or `null`.
	 * @param shape IR iterable shape (element type + container constraints).
	 * @param context Mutable walk cursor.
	 * @param errors Mutable error accumulator.
	 */
	fun walk(
		engine: ValidatorEngine,
		params: Any?,
		shape: IterableShape,
		context: ValidationCursor,
		errors: MutableList<ConstraintError<*>>,
	) {
		if (engine.rejectIfTooDeep(context, errors)) return

		engine.validateCompiled(
			value = params,
			constraints = shape.constraints,
			context = context,
			errors = errors)

		// Reuse the property-frame array list when present (same null-preserving instance built at
		// push). Otherwise build once for both the element walk and Distinct siblings.
		val priorArray = context.array
		val elements: List<Any?>
		val arrayValueCtx: ValidationContextValue<List<Any>>?
		if (priorArray != null && priorArray.shape === shape) {
			elements = priorArray.value ?: emptyList()
			arrayValueCtx = priorArray
		}
		else {
			elements = CollectionUtils.toIndexedElements(params)
			val needsArrayContext = ArrayContextFastPath.needsArrayContext(emptyList(), shape)
			arrayValueCtx = if (needsArrayContext) {
				@Suppress("UNCHECKED_CAST")
				ValidationContextValue(
					value = elements as List<Any>,
					objectSchema = (shape.element as? ObjectRefShape)?.ref?.value,
					shape = shape)
			}
			else null
		}

		if (engine.rejectIfTooLarge(size = elements.size, context = context, errors = errors)) return

		val element = shape.element

		elements.forEachIndexed { idx, elementValue ->

			if (engine.shouldAbortWalk(errors = errors, failFast = context.failFast)) return

			context.pushIndex(
				index = idx,
				elementShape = element,
				arrayContext = arrayValueCtx)
			
			try {
				when (element) {
					is IterableShape -> walk(
						engine = engine,
						params = elementValue,
						shape = element,
						context = context,
						errors = errors)

					is MapShape -> {
						engine.validateCompiled(
							value = elementValue,
							constraints = element.constraints,
							context = context,
							errors = errors)
						
						MapShapeWalker.walk(
							engine = engine,
							map = elementValue,
							shape = element,
							context = context,
							errors = errors)
					}

					is ObjectRefShape -> {
						engine.validateCompiled(
							value = elementValue,
							constraints = element.constraints,
							context = context,
							errors = errors)
						
						ObjectSchemaWalker.walk(
							engine = engine,
							param = elementValue,
							schema = element.ref.value,
							context = context,
							errors = errors,
							preserveArrayContext = true)
					}

					is DynamicShape -> {
						engine.validateCompiled(
							value = elementValue,
							constraints = element.constraints,
							context = context,
							errors = errors)
						
						if (elementValue != null) {
							engine.validationRegistry.tryResolveObjectSchema(elementValue.javaClass)?.let { resolved ->
								ObjectSchemaWalker.walk(
									engine = engine,
									param = elementValue,
									schema = resolved,
									context = context,
									errors = errors,
									preserveArrayContext = true)
							}
						}
					}

					is ScalarShape -> engine.validateCompiled(
						value = elementValue,
						constraints = element.constraints,
						context = context,
						errors = errors)
				}
			}
			finally {
				context.pop()
			}
		}
	}
}
