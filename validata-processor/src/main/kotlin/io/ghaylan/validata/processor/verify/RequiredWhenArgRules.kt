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
package io.ghaylan.validata.processor.verify

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.analyze.ExternalPropertyNames
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming

/**
 * Gate-aware helpers for `@RequiredWhen` compile-time argument checks.
 *
 * `value` / `values` are typed against the sibling named by `ref`, and only when `condition`
 * uses those arguments.*
 * 
 * @author Ghaylan Saada
 */
internal object RequiredWhenArgRules {
	
	/**
	 * Condition entry names that require a single `value` argument.
	 */
	private val VALUE_CONDITIONS = setOf("EQ", "NE", "GT", "LT", "GTE", "LTE")
	
	/**
	 * Condition entry names that require a `values` argument.
	 */
	private val VALUES_CONDITIONS = setOf("IN", "NIN")
	
	/**
	 * Whether [ann] is Validata `@RequiredWhen` (FQCN match).
	 *
	 * Side effects: none.
	 *
	 * @param ann Candidate annotation use-site.
	 * @return `true` when this is `@RequiredWhen`.	 
	 */
	fun isRequiredWhen(ann: KSAnnotation): Boolean =
		AnnotationFqcn.isA(ann, ProcessorFqns.REQUIRED_WHEN)
	
	/**
	 * Whether [parameterName] is unused under the active `condition` and should be skipped.
	 *
	 * Side effects: none.
	 *
	 * @param args Annotation arguments by name.
	 * @param parameterName Metadata / annotation parameter under check.
	 * @return `true` when the parameter is irrelevant for the active condition.	 
	 */
	fun shouldSkipParameter(
		args: Map<String, Any?>,
		parameterName: String
	): Boolean {
		val condition = conditionName(args[AnnotationAttrs.RequiredWhen.CONDITION])
			?: return false
		return when (parameterName) {
			AnnotationAttrs.ConstraintPayload.VALUE -> condition !in VALUE_CONDITIONS
			AnnotationAttrs.ConstraintPayload.VALUES -> condition !in VALUES_CONDITIONS
			else -> false
		}
	}
	
	/**
	 * Builds a declared-name / wire-name → [KSType] index for [owner]'s properties.
	 *
	 * Indexes the Kotlin name and the resolved external name (`@JsonProperty` or [naming]).
	 *
	 * Side effects: none.
	 *
	 * @param owner Declaring type that owns sibling properties.
	 * @param naming Wire naming when `@JsonProperty` is absent.
	 * @return Map accepting both declared and Jackson external names.	 
	 */
	fun ownerPropertyTypeIndex(
		owner: KSClassDeclaration,
		naming: JacksonPropertyNaming = JacksonPropertyNaming.IDENTITY,
	): Map<String, KSType> {
		val map = HashMap<String, KSType>()
		for (property in owner.getAllProperties()) {
			val type = property.type.resolve()
			val declared = property.simpleName.asString()
			map[declared] = type
			val external = ExternalPropertyNames.resolve(property, naming)
			if (external != declared) {
				map[external] = type
			}
		}
		return map
	}
	
	/**
	 * Resolves the gate sibling type named by `ref`.
	 *
	 * Looks up [siblingParamTypes] by declared or transport name, then [ownerPropertyTypes] or
	 * [owner] properties by declared name or external wire name.
	 *
	 * Side effects: none.
	 *
	 * @param owner Declaring type that owns sibling properties; may be null.
	 * @param siblingParamTypes Optional endpoint sibling name → type map; may be null.
	 * @param args Annotation arguments by name.
	 * @param ownerPropertyTypes Optional precomputed owner property name → type map; when null and
	 *   [owner] is non-null, properties are scanned linearly.
	 * @param naming Wire naming when scanning [owner] without a precomputed index.
	 * @return Gate type from [siblingParamTypes] or [owner], or `null` when unresolved.	 
	 */
	fun gateType(
		owner: KSClassDeclaration?,
		siblingParamTypes: Map<String, KSType>?,
		args: Map<String, Any?>,
		ownerPropertyTypes: Map<String, KSType>? = null,
		naming: JacksonPropertyNaming = JacksonPropertyNaming.IDENTITY,
	): KSType? {
		val gateName = args[AnnotationAttrs.ConstraintPayload.REF] as? String
			?: return null
		if (gateName.isBlank()) return null
		siblingParamTypes?.get(gateName)?.let { return it }
		ownerPropertyTypes?.get(gateName)?.let { return it }
		if (owner == null || ownerPropertyTypes != null) return null
		val prop = owner.getAllProperties().firstOrNull { property ->
			property.simpleName.asString() == gateName ||
				ExternalPropertyNames.resolve(property, naming) == gateName
		} ?: return null
		return prop.type.resolve()
	}
	
	/**
	 * Extracts the condition enum entry simple name from a KSP argument.
	 *
	 * Side effects: none.
	 *
	 * @param raw Condition argument value; may be null.
	 * @return Entry name (e.g. `EQ`), or `null`.	 
	 */
	private fun conditionName(raw: Any?): String? =
		when (raw) {
			null -> null
			is String -> raw.substringAfterLast('.')
			is KSType -> raw.declaration.simpleName.asString()
			else -> raw.toString().substringAfterLast('.')
		}
}
