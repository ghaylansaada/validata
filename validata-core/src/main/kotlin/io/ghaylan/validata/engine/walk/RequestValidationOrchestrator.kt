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
import io.ghaylan.validata.engine.fastpath.SchemaGroupFastPath
import io.ghaylan.validata.engine.support.DeferredErrorList
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.DynamicShape
import io.ghaylan.validata.schema.shape.MapShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape

/**
 * HTTP request-section orchestration extracted from [ValidatorEngine].
 *
 * Walks body then flat query/header/path sections against a pre-compiled [EndpointSchema].
 * Public engine API is unchanged — [ValidatorEngine.validateRequest] delegates here.*
 * 
 * @author Ghaylan Saada
 */
internal object RequestValidationOrchestrator {

	/**
	 * Reused IR shape for flat query/header/path section containers (Phase 3.3).
	 *
	 * These sections are always `Map<String, Any?>` wrappers; allocating a fresh [MapShape]
	 * per section per request was pure waste.
	 */
	private val FLAT_SECTION_MAP_SHAPE: MapShape = MapShape(
		key = ScalarShape(ScalarKind.STRING),
		value = DynamicShape())

	/**
	 * Validates an HTTP request against an already-resolved [EndpointSchema].
	 *
	 * Allocates a fresh [ValidationCursor] per run; mutates that cursor while walking body and
	 * flat sections. Returns raw accumulated errors — the engine deduplicates before returning
	 * to callers.
	 *
	 * @param engine Host engine providing walkers, abort checks, and registry lookups.
	 * @param schema Pre-resolved endpoint schema (same instance as in the registry).
	 * @param body Resolved `@RequestBody` instance, or `null`.
	 * @param params Query-parameter map, or `null` when the section is unused.
	 * @param headers Header map, or `null` when the section is unused.
	 * @param pathVariables Path-variable map, or `null` when the section is unused.
	 * @return Collected constraint errors (may contain duplicates; engine deduplicates).
	 */
	fun validate(
		engine: ValidatorEngine,
		schema: EndpointSchema,
		body: Any?,
		params: Map<String, Any?>?,
		headers: Map<String, Any?>?,
		pathVariables: Map<String, Any?>?,
	): List<ConstraintError<*>> {
		val errors = DeferredErrorList()
		val activeGroups = schema.groups

		val failFast = schema.failFast
		val cursor = ValidationCursor.root(
			oneErrorPerParam = schema.oneErrorPerParam,
			failFast = failFast,
			groups = activeGroups,
			skipGroupChecks = SchemaGroupFastPath.canSkipGroupChecks(
				activeGroups,
				schema.requestBody,
				schema.queryParams,
				schema.headers,
				schema.pathVariables,
			),
		)

		val bodySchema = schema.requestBody
		if (bodySchema != null &&
			(bodySchema.properties.isNotEmpty() || bodySchema.subtypes.isNotEmpty())
		) {
			// Null body still walks the schema so property-level @Required (and similar) run.
			val selected = if (body != null) {
				engine.validationRegistry.schemaForValue(bodySchema, body)
			} else {
				bodySchema
			}
			cursor.bindRoot(
				shape = selected.selfRef,
				container = ValidationContextValue(
					value = body,
					objectSchema = selected,
					shape = selected.selfRef),
			)
			ObjectSchemaWalker.walk(
				engine = engine,
				param = body,
				schema = selected,
				context = cursor,
				errors = errors,
				preserveArrayContext = false)
			cursor.bindRoot(shape = null, container = null)
		}

		if (engine.shouldAbortWalk(errors, failFast)) {
			return errors.snapshot()
		}

		schema.queryParams?.takeIf { it.properties.isNotEmpty() }?.let { querySchema ->
			validateHeadersOrParamsOrPathVariables(
				engine = engine,
				params = params,
				schema = querySchema,
				context = cursor,
				errors = errors)
		}

		if (engine.shouldAbortWalk(errors, failFast)) {
			return errors.snapshot()
		}

		schema.headers?.takeIf { it.properties.isNotEmpty() }?.let { headerSchema ->
			validateHeadersOrParamsOrPathVariables(
				engine = engine,
				params = headers,
				schema = headerSchema,
				context = cursor,
				errors = errors)
		}

		if (engine.shouldAbortWalk(errors, failFast)) {
			return errors.snapshot()
		}

		schema.pathVariables?.takeIf { it.properties.isNotEmpty() }?.let { pathSchema ->
			validateHeadersOrParamsOrPathVariables(
				engine = engine,
				params = pathVariables,
				schema = pathSchema,
				context = cursor,
				errors = errors)
		}

		return errors.snapshot()
	}

	/**
	 * Validates flat transport dictionaries (query, headers, path) using a pre-compiled [ObjectSchema].
	 *
	 * The schema was emitted by KSP with [ValueReader]s over a
	 * `Map<String, Any?>` and already-built compiled constraints — no per-request conversion.
	 * Mutates [context] (section push/pop) and appends violations to [errors].
	 *
	 * @param engine Host engine providing the object-schema walker.
	 * @param params Section map, or `null` when unused.
	 * @param schema Pre-compiled flat-section object schema.
	 * @param context Mutable walk cursor.
	 * @param errors Mutable error accumulator.
	 */
	private fun validateHeadersOrParamsOrPathVariables(
		engine: ValidatorEngine,
		params: Map<String, Any?>?,
		schema: ObjectSchema,
		context: ValidationCursor,
		errors: MutableList<ConstraintError<*>>,
	) {
		if (schema.properties.isEmpty()) return

		val containerCtx = ValidationContextValue<Any>(
			value = params,
			objectSchema = schema,
			shape = FLAT_SECTION_MAP_SHAPE)

		context.pushSectionContainer(containerCtx)
		
		try {
			ObjectSchemaWalker.walk(
				engine = engine,
				param = params,
				schema = schema,
				context = context,
				errors = errors,
				preserveArrayContext = false)
		} finally {
			context.pop()
		}
	}
}
