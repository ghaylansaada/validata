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
package io.ghaylan.validata.engine

import io.ghaylan.validata.engine.ValidatorEngine.Companion.DEFAULT_GROUPS
import io.ghaylan.validata.engine.support.ConstraintErrorDeduper
import io.ghaylan.validata.engine.support.ValidationLimitGuards
import io.ghaylan.validata.engine.walk.RequestValidationOrchestrator
import io.ghaylan.validata.engine.walk.ShapeCascade
import io.ghaylan.validata.engine.walk.StandaloneValidation
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.TypeShape
import kotlin.reflect.KClass

/**
 * Stateless schema-walk façade for body and flat query/header/path sections.
 *
 * Walks pre-compiled IR graphs (never rebuilt per request). Safe as a singleton. Empty returned
 * lists mean valid; non-empty lists are deduplicated and ordered by natural path.
 *
 * Prefer [ValidationOptions] for standalone call sites.
 *
 * @property validationRegistry Cache of endpoint schemas, generated object schemas, and validators.
 * @property limits Per-run ceilings so hostile input cannot amplify into unbounded work.*
 * 
 * @author Ghaylan Saada
 */
open class ValidatorEngine(
	val validationRegistry: ValidationRegistry,
	val limits: ValidationLimits = ValidationLimits()
) {
	
	companion object {
		
		/** Shared active-group set for the common `groups = [OnDefault]` call (Phase 3.1).		 */
		private val DEFAULT_GROUPS: Set<KClass<*>> = setOf(OnDefault::class)
		
		/**
		 * Returns a stable [Set] for [groups], reusing [DEFAULT_GROUPS] when possible.
		 *
		 * @param groups Active validation groups from the public API.
		 * @return Shared [DEFAULT_GROUPS], empty set, or a fresh set from [groups].		 
		 */
		internal fun resolveGroups(groups: Array<KClass<*>>): Set<KClass<*>> = when {
			groups.size == 1 && groups[0] == OnDefault::class -> DEFAULT_GROUPS
			groups.isEmpty() -> emptySet()
			else -> groups.toSet()
		}
	}
	
	/** @see ValidationLimitGuards.rejectIfTooDeep     */
	internal fun rejectIfTooDeep(
		context: ValidationContext,
		errors: MutableList<ConstraintError<*>>,
	): Boolean = ValidationLimitGuards.rejectIfTooDeep(
		limits = limits,
		context = context,
		errors = errors)
	
	/** @see ValidationLimitGuards.rejectIfTooLarge     */
	internal fun rejectIfTooLarge(
		size: Int,
		context: ValidationContext,
		errors: MutableList<ConstraintError<*>>,
	): Boolean = ValidationLimitGuards.rejectIfTooLarge(
		limits = limits,
		size = size,
		context = context,
		errors = errors)
	
	/** @see ValidationLimitGuards.shouldAbortWalk     */
	internal fun shouldAbortWalk(
		errors: List<ConstraintError<*>>,
		failFast: Boolean
	): Boolean = ValidationLimitGuards.shouldAbortWalk(
		limits = limits,
		errors = errors,
		failFast = failFast)
	
	/**
	 * Validates a standalone object against its generated schema.
	 *
	 * @param params Root object to validate.
	 * @param options Per-run flags and active groups (defaults via [ValidationOptions]).
	 * @return Deduplicated constraint errors (empty when valid).	 
	 */
	fun validate(
		params: Any,
		options: ValidationOptions = ValidationOptions(),
	): List<ConstraintError<*>> = StandaloneValidation.validate(
		engine = this,
		params = params,
		options = options,
		resolveGroups = ::resolveGroups)
	
	/**
	 * Validates an HTTP request by endpoint [id] (looks up schema then delegates).
	 *
	 * @throws IllegalStateException When no schema is registered for [id].	 
	 */
	fun validateRequest(
		id: String,
		body: Any?,
		params: Map<String, Any?>?,
		headers: Map<String, Any?>?,
		pathVariables: Map<String, Any?>?,
	): List<ConstraintError<*>> {
		val schema = validationRegistry.getSchemaByRequest(id)
			?: error("No validation schema found for request $id.")
		
		return validateRequest(
			schema = schema,
			body = body,
			params = params,
			headers = headers,
			pathVariables = pathVariables)
	}
	
	/**
	 * Validates an HTTP request against an already-resolved [EndpointSchema].	 
	 */
	fun validateRequest(
		schema: EndpointSchema,
		body: Any?,
		params: Map<String, Any?>?,
		headers: Map<String, Any?>?,
		pathVariables: Map<String, Any?>?,
	): List<ConstraintError<*>> {
		
		val errors = RequestValidationOrchestrator.validate(
			engine = this,
			schema = schema,
			body = body,
			params = params,
			headers = headers,
			pathVariables = pathVariables)
		
		return ConstraintErrorDeduper.deduplicate(errors)
	}
	
	/** @see ShapeCascade.cascade     */
	internal fun cascadeIntoShape(
		value: Any?,
		shape: TypeShape,
		context: ValidationCursor,
		errors: MutableList<ConstraintError<*>>,
		preserveArrayContextForNestedObject: Boolean,
	) {
		ShapeCascade.cascade(
			engine = this,
			value = value,
			shape = shape,
			context = context,
			errors = errors,
			preserveArrayContextForNestedObject = preserveArrayContextForNestedObject)
	}
	
	/** @see CompiledConstraintExecutor.validate     */
	internal fun validateCompiled(
		value: Any?,
		constraints: List<CompiledConstraint>,
		context: ValidationContext,
		errors: MutableList<ConstraintError<*>>,
	) {
		CompiledConstraintExecutor.validate(
			engine = this,
			value = value,
			constraints = constraints,
			context = context,
			errors = errors)
	}
}
