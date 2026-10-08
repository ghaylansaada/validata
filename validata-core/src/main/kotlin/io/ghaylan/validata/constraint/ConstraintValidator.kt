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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.PropertyPath
import io.ghaylan.validata.schema.constraint.ConstraintConfig

/**
 * Base type for every constraint validator (single-field or cross-field).
 *
 * [runValidation] is the engine entry point: it applies group filtering, routes null vs non-null
 * to [validateNull] / [validate], and on failure attaches path and message. Implement [validate]
 * only for value rules; override [validateNull] only for presence-on-null.
 *
 * ### Null policy
 *
 * `null` never reaches [validate]. The default [validateNull] returns `null` (valid / skip).
 * Presence constraints (`@Required`, `@RequiredWhen`) override [validateNull]; empty / blank
 * checks still go through [validate] because those values are non-null.
 *
 * ### Example
 *
 * ```kotlin
 * object OddYearsValidator : ConstraintValidator<Int, OddYearsConstraint>() {
 *   override fun possibleErrorCodes(constraint: OddYearsConstraint, type: Class<*>) =
 *     setOf(MyErrorCode.ODD_YEAR_REQUIRED)
 *
 *   override fun validate(
 *     value: Int,
 *     constraint: OddYearsConstraint,
 *     context: ValidationContext,
 *   ): ConstraintError<*>? {
 *     if (value % 2 != 0) return null
 *     return ConstraintError(
 *       code = MyErrorCode.ODD_YEAR_REQUIRED,
 *       message = "$value is not an odd integer.",
 *     )
 *   }
 * }
 * ```
 *
 * Register the annotation with `@Constraint(validatedBy = [OddYearsValidator::class])`; KSP emits
 * the catalog binding. Prefer a Kotlin `object` so the catalog can share one instance.
 *
 * @param Value Subject type this validator accepts (`Any` when the rule is type-agnostic).
 *   Must be non-null (`Any`); null subjects are handled by [validateNull].
 * @param Constraint Generated [ConstraintMetadata] subtype for this annotation.*
 * 
 * @author Ghaylan Saada
 */
abstract class ConstraintValidator<Value : Any, Constraint : ConstraintMetadata> {

	/**
	 * When `true`, the engine must populate [ValidationContext.array] before calling this validator
	 * (sibling-element lookups such as element-context `@Distinct`).
	 *
	 * Default `false`. Override only when the implementation reads `context.array`; a false negative
	 * silently skips uniqueness checks that need the sibling list.
	 */
	open val requiresArrayContext: Boolean get() = false

	/**
	 * Machine-readable set of error codes this validator may emit on failure.
	 *
	 * Used by OpenAPI / docs tooling to document per-endpoint `400` response codes without
	 * re-scanning annotations. Custom validators **should** override with every code their
	 * [validate] / [validateNull] implementation can return for the given [constraint]
	 * configuration and subject [type]. An empty set means "undocumented codes" for
	 * documentation consumers (they may warn; they must not fail the application).
	 *
	 * Default is empty.
	 *
	 * @param constraint Arguments for this annotation instance.
	 * @param type Runtime subject class of the property / type-use being documented (e.g.
	 *   `String::class.java`, `LocalDate::class.java`). Approximate when IR only carries a
	 *   coarse shape.
	 */
	open fun possibleErrorCodes(
		constraint: Constraint,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> = emptySet()

	/**
	 * Resolves [possibleErrorCodes] for a compiled metadata instance.
	 *
	 * OpenAPI and other tooling call this when the validator is only known as
	 * [ConstraintValidator] with star projection.
	 *
	 * @param config Constraint metadata from IR.
	 * @param type Runtime subject class for the property / type-use being documented.
	 */
	@Suppress("UNCHECKED_CAST")
	fun resolvedPossibleErrorCodes(
		config: ConstraintConfig,
		type: Class<*>,
	): Set<ConstraintErrorDefinition> =
		possibleErrorCodes(config as Constraint, type)

	/**
	 * Runs group filtering, then [validateNull] or [validate], then enriches a violation with path
	 * and message.
	 *
	 * Returns a [ConstraintError.copy] with `path` / `message` set on failure. On success (`null`
	 * from the rule) no path string is materialized and no message work runs. On failure,
	 * [ConstraintError.path] becomes [ValidationContext.fieldPath] and the message is resolved as:
	 * non-blank annotation `message`, else the validator's provisional [ConstraintError.message],
	 * else the error-code default.
	 *
	 * @param value Raw property value (may be `null`).
	 * @param constraint Metadata for this constraint instance (cast to [Constraint] after group
	 *   checks).
	 * @param context Active validation cursor (groups, path, sibling access).
	 * @return Enriched [ConstraintError] on violation; `null` when valid or skipped by groups.
	 */
	@Suppress("UNCHECKED_CAST")
	fun runValidation(
		value: Any?,
		constraint: ConstraintMetadata,
		context: ValidationContext
	): ConstraintError<*>? {

		if (!shouldValidate(constraint = constraint as Constraint, context = context)) return null

		val error = if (value == null) {
			validateNull(constraint = constraint, context = context)
		} else {
			@Suppress("UNCHECKED_CAST")
			val typed = try {
				value as Value
			} catch (ex: ClassCastException) {
				error("Constraint on '${context.fieldPath}' received value of type " +
						"${value::class.qualifiedName ?: value.javaClass.name} but validator " +
						"${this::class.simpleName} expects a different subject type " +
						"(ClassCastException: ${ex.message})")
			}
			validate(
				value = typed,
				constraint = constraint,
				context = context)
		} ?: return null

		return error.copy(
			path = context.fieldPath,
			message = constraint.message.takeIf(String::isNotBlank)
				?: error.message.takeIf { !it.isNullOrBlank() }
				?: error.code.message,
		)
	}

	/**
	 * Returns whether [constraint] should run under [context]'s active groups.
	 *
	 * Delegates to [ConstraintGroupMatching]. No I/O or mutation.
	 *
	 * @param constraint Typed metadata whose [ConstraintMetadata.groups] are checked.
	 * @param context Cursor whose [ValidationContext.groups] /
	 *   [ValidationContext.skipGroupChecks] apply.
	 * @return `true` when validation should proceed.
	 */
	private fun shouldValidate(
		constraint: Constraint,
		context: ValidationContext
	): Boolean = ConstraintGroupMatching.shouldRun(constraint.groups, context)

	/**
	 * Handles a `null` subject.
	 *
	 * Default: treat null as valid (skip). Override for presence-on-null (`@Required`,
	 * `@RequiredWhen`). Empty / blank / deep-empty checks belong in [validate] — those values are
	 * non-null and never reach this method.
	 *
	 * No path/message enrichment — return a path-free [ConstraintError]; [runValidation] attaches
	 * path and resolves the final message. Return `null` when valid.
	 *
	 * @param constraint Arguments for this annotation instance.
	 * @param context Cursor for sibling reads and path.
	 * @return Violation, or `null` if null is acceptable for this constraint.
	 */
	protected open fun validateNull(
		constraint: Constraint,
		context: ValidationContext,
	): ConstraintError<*>? = null

	/**
	 * Implements the constraint rule for a non-null [value].
	 *
	 * No path/message enrichment — return a path-free [ConstraintError] (code + optional message);
	 * [runValidation] attaches path and resolves the final message. Return `null` when valid.
	 *
	 * @param value Typed subject; never `null` ([runValidation] routes null to [validateNull]).
	 * @param constraint Arguments for this annotation instance.
	 * @param context Cursor for sibling reads and path.
	 * @return Violation, or `null` if valid.
	 */
	protected abstract fun validate(
		value: Value,
		constraint: Constraint,
		context: ValidationContext,
	): ConstraintError<*>?

	/**
	 * Resolves and extracts a sibling property value from the enclosing container.
	 *
	 * Only **single-segment** names are supported (same-object fields or flat endpoint params).
	 * Nested paths such as `"address.city"` are rejected at compile time and must not appear here.
	 * The name may be the declared Kotlin name or the `@JsonProperty` wire name.
	 *
	 * A path that cannot be resolved is a configuration error — the constraint points at a property
	 * that does not exist. Resolving it to `null` would make the referencing constraint quietly
	 * pass or fail forever.
	 *
	 * @param name Sibling field name (e.g. `"password"`), never a dotted nested path.
	 * @param context Active validation context with the parent object node.
	 * @return The raw value at the path, or `null` if there is no enclosing object / an intermediate
	 *   is null.
	 * @throws IllegalStateException if the path cannot be resolved on the container schema.
	 */
	protected fun getPropertyValue(
		name: String,
		context: ValidationContext,
	): Any? {
		
		if ('.' in name) {
			val segments = PropertyPath.split(name)
			
			require(segments.size <= 1) {
				"Constraint on '${context.fieldPath}' uses nested property reference '$name'; only same-object (or flat parameter) names are supported."
			}
		}
		
		val container = context.containerObject ?: return null
		
		val schema = container.objectSchema
			?: error("Constraint on '${context.fieldPath}' needs a structured container to resolve '$name'.")
		
		val instance = container.value ?: return null
		
		return try {
			// Hot path: sibling refs are almost always a single undeclared-dot name — skip split.
			if ('.' !in name) {
				val spec = PropertyPath.findProperty(schema, name)
					?: error("Unknown property '$name' on ${schema.type.name}. Use a declaredName or externalName that exists on that type.")
				
				spec.read.read(instance)
			} else {
				PropertyPath.read(schema, instance, name)
			}
		} catch (ex: IllegalStateException) {
			error("Constraint on '${context.fieldPath}' references property '$name', which cannot be resolved on ${schema.type.name}: ${ex.message}")
		}
	}
}
