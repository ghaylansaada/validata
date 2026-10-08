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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import io.ghaylan.validata.schema.shape.*
import kotlin.reflect.KClass

/**
 * Collects OpenAPI `x-validata-errors` entries for a property schema level or nested shape level.
 *
 * Each entry is `{ "code", "message" }`.*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyErrorCodeCollector {
	
	/**
	 * Sorted distinct `{code, message}` maps for [prop] under [activeGroups], including nested
	 * shape constraints (flat union — prefer [collectForLevel] when writing nested schemas).
	 *
	 * @param prop Validata property IR
	 * @param activeGroups endpoint groups; empty means all constraints active
	 * @return immutable list sorted by `code`	 
	 */
	fun collect(
		prop: PropertySpec,
		activeGroups: Set<KClass<*>> = emptySet()
	): List<Map<String, String>> {
		val byCode = linkedMapOf<String, String>()
		putTypeMismatch(byCode)
		var hasRequired = false
		val subjectType = TypeShapeJavaTypes.resolve(prop.shape)
		ConstraintErrorCodeWalk.forEachActive(
			type = subjectType,
			constraints = prop.constraints,
			activeGroups = activeGroups,
			onRequired = { hasRequired = true },
			onDefinition = { def -> byCode.putIfAbsent(def.code, def.message) })
		considerShapeFlat(prop.shape, activeGroups, byCode) { hasRequired = true }
		if (hasRequired) putValueNull(byCode)
		mergeApiErrors(prop.errorDocs, byCode)
		return toSortedEntries(byCode)
	}
	
	/**
	 * Errors for one schema level: [constraints] (+ optional structural / `@ApiError` extras).
	 *
	 * @param constraints compiled constraints at this schema level
	 * @param activeGroups endpoint groups; empty means all constraints active
	 * @param includeTypeMismatch when `true`, always adds [ConstraintErrorCode.VALUE_TYPE_MISMATCH]
	 * @param errorDocs `@ApiError` docs to merge (annotation wins on code collision); typically
	 *   only on the property root
	 * @param type runtime subject class for [io.ghaylan.validata.constraint.ConstraintValidator.possibleErrorCodes]
	 * @return immutable list sorted by `code`	 
	 */
	fun collectForLevel(
		constraints: List<CompiledConstraint>,
		activeGroups: Set<KClass<*>> = emptySet(),
		includeTypeMismatch: Boolean = false,
		errorDocs: List<SchemaErrorDoc> = emptyList(),
		type: Class<*> = Any::class.java,
	): List<Map<String, String>> {
		val byCode = linkedMapOf<String, String>()
		if (includeTypeMismatch) putTypeMismatch(byCode)
		var hasRequired = false
		ConstraintErrorCodeWalk.forEachActive(
			type = type,
			constraints = constraints,
			activeGroups = activeGroups,
			onRequired = { hasRequired = true },
			onDefinition = { def -> byCode.putIfAbsent(def.code, def.message) })
		if (hasRequired) putValueNull(byCode)
		mergeApiErrors(docs = errorDocs, byCode = byCode)
		return toSortedEntries(byCode)
	}
	
	/**
	 * Walks [shape] flat (all nesting) into [byCode] — used by [collect] only.
	 */
	private fun considerShapeFlat(
		shape: TypeShape,
		activeGroups: Set<KClass<*>>,
		byCode: MutableMap<String, String>,
		onRequired: () -> Unit,
	) {
		ConstraintErrorCodeWalk.forEachActive(
			type = TypeShapeJavaTypes.resolve(shape),
			constraints = shape.constraints,
			activeGroups = activeGroups,
			onRequired = onRequired,
			onDefinition = { def -> byCode.putIfAbsent(def.code, def.message) })
		
		when (shape) {
			is ScalarShape,
			is DynamicShape,
			is ObjectRefShape -> Unit
			is IterableShape -> considerShapeFlat(shape.element, activeGroups, byCode, onRequired)
			is MapShape -> {
				considerShapeFlat(shape.key, activeGroups, byCode, onRequired)
				considerShapeFlat(shape.value, activeGroups, byCode, onRequired)
			}
		}
	}
	
	private fun putTypeMismatch(byCode: MutableMap<String, String>) {
		val typeMismatch = ConstraintErrorCode.VALUE_TYPE_MISMATCH
		byCode[typeMismatch.code] = typeMismatch.message
	}
	
	private fun putValueNull(byCode: MutableMap<String, String>) {
		val nullCode = ConstraintErrorCode.VALUE_MISSING
		byCode.putIfAbsent(nullCode.code, nullCode.message)
	}
	
	private fun mergeApiErrors(
		docs: List<SchemaErrorDoc>,
		byCode: MutableMap<String, String>
	) {
		for (doc in docs) {
			val (code, message) = SchemaErrorDocResolver.resolve(doc)
			byCode[code] = message
		}
	}
	
	private fun toSortedEntries(
		byCode: Map<String, String>
	): List<Map<String, String>> {
		return byCode.entries
			.sortedBy { it.key }
			.map { (code, message) ->
				linkedMapOf("code" to code, "message" to message)
			}
	}
}
