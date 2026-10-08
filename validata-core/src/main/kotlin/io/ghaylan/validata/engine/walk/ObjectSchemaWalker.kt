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
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ObjectRefShape

/**
 * Object-schema property walker extracted from [ValidatorEngine].
 *
 * Public engine API is unchanged — this is an internal size-split of the schema walk.*
 * 
 * @author Ghaylan Saada
 */
internal object ObjectSchemaWalker {

	/**
	 * Evaluates properties of [schema] via each property's [ValueReader],
	 * mutating [context] (push/pop per property) and appending violations to [errors].
	 *
	 * @param engine Host engine providing depth/size guards, compiled constraints, and nested cascade.
	 * @param param Object instance under validation, or `null`.
	 * @param schema Compiled object schema for [param]'s type.
	 * @param context Mutable walk cursor for this run.
	 * @param errors Mutable error accumulator.
	 * @param preserveArrayContext When `true`, child fields that are not themselves iterables keep
	 *    [ValidationContext.array] so `@Distinct` on list-of-object properties can see sibling elements.
	 */
	fun walk(
		engine: ValidatorEngine,
		param: Any?,
		schema: ObjectSchema,
		context: ValidationCursor,
		errors: MutableList<ConstraintError<*>>,
		preserveArrayContext: Boolean,
	) {
		val effective = engine.validationRegistry.schemaForValue(schema, param)
		
		if (effective !== schema) {
			walk(
				engine = engine,
				param = param,
				schema = effective,
				context = context,
				errors = errors,
				preserveArrayContext = preserveArrayContext)
			return
		}

		if (engine.rejectIfTooDeep(context, errors)) return

		// Reuse the cursor's container when it already wraps this object (standalone / body root).
		val existing = context.containerObject
		
		val objectValueCtx = if (
			existing != null &&
			existing.value === param &&
			existing.objectSchema === schema
		) {
			existing
		} else {
			ValidationContextValue(
				value = param,
				objectSchema = schema,
				shape = schema.selfRef)
		}

		for (property in schema.properties) {

			if (engine.shouldAbortWalk(errors, context.failFast)) return

			val value = param?.let { property.read.read(it) }

			val arrayValueCtx = when (property.shape) {
				is IterableShape -> {
					val shape = property.shape as IterableShape
					if (ArrayContextFastPath.needsArrayContext(property.constraints, shape)) {
						ValidationContextValue(
							value = CollectionUtils.toArrayContextList(value),
							objectSchema = (shape.element as? ObjectRefShape)?.ref?.value,
							shape = property.shape)
					} else {
						null
					}
				}
				else -> if (preserveArrayContext) context.array else null
			}

			context.pushProperty(
				name = property.externalName,
				propertyShape = property.shape,
				container = objectValueCtx,
				arrayContext = arrayValueCtx)
			
			try {
				engine.validateCompiled(
					value = value,
					constraints = property.constraints,
					context = context,
					errors = errors)
				
				if (engine.shouldAbortWalk(errors, context.failFast)) return

				if (value == null) continue

				engine.cascadeIntoShape(
					value = value,
					shape = property.shape,
					context = context,
					errors = errors,
					preserveArrayContextForNestedObject = false)
			} finally {
				context.pop()
			}
		}
	}
}
