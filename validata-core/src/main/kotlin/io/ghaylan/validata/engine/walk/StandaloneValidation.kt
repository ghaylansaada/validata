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
import io.ghaylan.validata.engine.ValidationOptions
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.engine.fastpath.SchemaGroupFastPath
import io.ghaylan.validata.engine.support.ConstraintErrorDeduper
import io.ghaylan.validata.engine.support.DeferredErrorList
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContextValue
import kotlin.reflect.KClass

/**
 * Standalone object validation extracted from [ValidatorEngine].
 *
 * Resolves the generated object schema, walks it, and returns deduplicated errors.*
 * 
 * @author Ghaylan Saada
 */
internal object StandaloneValidation {
	
	/**
	 * Validates [params] against its generated object schema using [options].
	 *
	 * @param engine Host engine providing registry, walkers, and group resolution.
	 * @param params Root object to validate.
	 * @param options Per-run flags and active groups.
	 * @param resolveGroups Stable group-set resolver from the engine companion.
	 * @return Deduplicated, naturally ordered constraint errors (empty when valid).	 
	 */
	fun validate(
		engine: ValidatorEngine,
		params: Any,
		options: ValidationOptions,
		resolveGroups: (Array<KClass<*>>) -> Set<KClass<*>>,
	): List<ConstraintError<*>> {
		val schema = engine.validationRegistry.resolveObjectSchemaByClass(params.javaClass)
		val errors = DeferredErrorList()
		val activeGroups = resolveGroups(options.groups)
		
		val context = ValidationCursor.root(
			oneErrorPerParam = options.oneErrorPerParam,
			failFast = options.failFast,
			groups = activeGroups,
			skipGroupChecks = SchemaGroupFastPath.canSkipGroupChecks(activeGroups, schema),
			shape = schema.selfRef,
			containerObject = ValidationContextValue(
				value = params,
				objectSchema = schema,
				shape = schema.selfRef))
		
		ObjectSchemaWalker.walk(
			engine = engine,
			param = params,
			schema = schema,
			context = context,
			errors = errors,
			preserveArrayContext = false)
		
		return ConstraintErrorDeduper.deduplicate(errors.snapshot())
	}
}
