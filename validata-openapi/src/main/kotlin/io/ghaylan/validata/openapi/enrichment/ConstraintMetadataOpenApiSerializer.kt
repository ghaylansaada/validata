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

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.PropertyRef
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.ref.PropertyRefScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Serializes [ConstraintMetadata] into OpenAPI-friendly maps for
 * [ConstraintExtensionKeys.CONSTRAINTS] array entries.
 *
 * Uses public Kotlin properties on the concrete metadata class (KSP-generated data classes).
 * Omits `message` and `groups`. Omits empty args (null, blank string, empty collection/map).
 * Each entry is prefixed with [ConstraintExtensionKeys.CONSTRAINT_KIND].
 * `@PropertyRef` args are rewritten to wire [PropertySpec.externalName]
 * when an owner schema is provided.
 * [CompositionConstraint] publishes `composition=OR` and nested `members` (leaf entries).
 * Output is deterministic: LinkedHashMap insertion order follows property name order after the kind.
 *
 * Reflective property descriptors are cached per metadata class.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintMetadataOpenApiSerializer {
	
	/**
	 * Property names never published under `x-validata-constraints` (not request values).
	 */
	private val OMITTED_PROPERTIES: Set<String> = setOf("message", "groups")
	
	/**
	 * Cached reflective accessors per concrete metadata [KClass].
	 */
	private val descriptorsByClass = ConcurrentHashMap<KClass<*>, List<PropDescriptor>>()
	
	/**
	 * Short annotation name for [ConstraintExtensionKeys.CONSTRAINT_KIND]
	 * (`SizeConstraint` → `Size`, `CompareConstraint` → `Compare`).
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @return annotation short name without the `Constraint` suffix	 
	 */
	fun typeName(metadata: ConstraintMetadata): String {
		val simple = metadata::class.simpleName ?: metadata::class.java.simpleName
		
		return if (simple.endsWith("Constraint") && simple.length > "Constraint".length) {
			simple.removeSuffix("Constraint")
		} else simple
	}
	
	/**
	 * One [ConstraintExtensionKeys.CONSTRAINTS] array entry for [metadata].
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @param siblingSchema schema for [PropertyRefScope.SIBLING] refs (enclosing DTO); may be `null`
	 * @param elementSchema schema for [PropertyRefScope.ELEMENT] refs (list element DTO); may be `null`
	 * @return map with [ConstraintExtensionKeys.CONSTRAINT_KIND] plus non-empty args	 
	 */
	fun toConstraintEntry(
		metadata: ConstraintMetadata,
		siblingSchema: ObjectSchema? = null,
		elementSchema: ObjectSchema? = null,
	): Map<String, Any?> {
		if (metadata is CompositionConstraint) {
			return compositionEntry(metadata, siblingSchema, elementSchema)
		}
		val out = LinkedHashMap<String, Any?>()
		out[ConstraintExtensionKeys.CONSTRAINT_KIND] = typeName(metadata)
		for (descriptor in descriptors(metadata::class)) {
			val raw = descriptor.read(metadata)
			val serialized = if (descriptor.propertyRef != null) {
				serializePropertyRefValue(raw, descriptor.propertyRef.scope, siblingSchema, elementSchema)
			}
			else {
				serializeValue(raw)
			}
			if (isEmptyArg(serialized)) continue
			out[descriptor.name] = serialized
		}
		return out
	}
	
	/**
	 * Publishes an OR composition as `{ "_constraint": "Composition", "composition": "OR", "members": […] }`.
	 *
	 * @param metadata composition site
	 * @param siblingSchema sibling `@PropertyRef` rewrite root
	 * @param elementSchema element `@PropertyRef` rewrite root
	 * @return OpenAPI-friendly entry without raw [CompiledConstraint] `toString` payloads	 
	 */
	private fun compositionEntry(
		metadata: CompositionConstraint,
		siblingSchema: ObjectSchema?,
		elementSchema: ObjectSchema?,
	): Map<String, Any?> {
		val members = metadata.children.mapNotNull { child ->
			val childMeta = child.metadata as? ConstraintMetadata
				?: return@mapNotNull null
			toConstraintEntry(childMeta, siblingSchema, elementSchema)
		}
		val out = LinkedHashMap<String, Any?>()
		out[ConstraintExtensionKeys.CONSTRAINT_KIND] = "Composition"
		out["composition"] = "OR"
		if (members.isNotEmpty()) out["members"] = members
		return out
	}
	
	/**
	 * Arg map for [metadata] without the kind key (tests / callers that only need args).
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @param siblingSchema schema for sibling `@PropertyRef` rewrite; may be `null`
	 * @param elementSchema schema for element `@PropertyRef` rewrite; may be `null`
	 * @return deterministic map of non-empty args (no kind / message / groups)	 
	 */
	fun toArgsMap(
		metadata: ConstraintMetadata,
		siblingSchema: ObjectSchema? = null,
		elementSchema: ObjectSchema? = null,
	): Map<String, Any?> = toConstraintEntry(
		metadata = metadata,
		siblingSchema = siblingSchema,
		elementSchema = elementSchema
	).filterKeys { it != ConstraintExtensionKeys.CONSTRAINT_KIND }
	
	/**
	 * Clears the reflective descriptor cache (tests).
	 */
	fun resetForTests() {
		descriptorsByClass.clear()
	}
	
	/**
	 * Returns cached property descriptors for [kClass], building them on first use.
	 */
	private fun descriptors(kClass: KClass<*>): List<PropDescriptor> =
		descriptorsByClass.getOrPut(kClass) { buildDescriptors(kClass) }
	
	/**
	 * Reflects [kClass] once into ordered [PropDescriptor]s.
	 */
	private fun buildDescriptors(kClass: KClass<*>): List<PropDescriptor> {
		val result = ArrayList<PropDescriptor>()
		for (prop in kClass.memberProperties.sortedBy { it.name }) {
			if (prop.name in OMITTED_PROPERTIES) continue
			prop.isAccessible = true
			@Suppress("UNCHECKED_CAST")
			val typed = prop as KProperty1<ConstraintMetadata, *>
			result += PropDescriptor(
				name = prop.name,
				propertyRef = findPropertyRef(kClass, typed),
				read = { metadata -> typed.get(metadata) })
		}
		return result
	}
	
	/**
	 * Resolves `@PropertyRef` from the property or matching primary-constructor parameter
	 * (KSP metadata often places the annotation on the constructor parameter).
	 *
	 * @param kClass concrete metadata class
	 * @param prop reflected member property
	 * @return `@PropertyRef` when present; otherwise `null`	 
	 */
	private fun findPropertyRef(
		kClass: KClass<*>,
		prop: KProperty1<*, *>,
	): PropertyRef? {
		prop.findAnnotation<PropertyRef>()?.let { return it }
		
		return kClass.primaryConstructor
			?.parameters
			?.firstOrNull { it.name == prop.name }
			?.findAnnotation()
	}
	
	/**
	 * Whether [value] should be omitted from the published entry.
	 *
	 * @param value serialized arg value
	 * @return `true` for null, blank string, or empty collection/map	 
	 */
	private fun isEmptyArg(value: Any?): Boolean = when (value) {
		null -> true
		is String -> value.isBlank()
		is Collection<*> -> value.isEmpty()
		is Map<*, *> -> value.isEmpty()
		else -> false
	}
	
	/**
	 * Serializes a `@PropertyRef` arg, rewriting string path segments to wire names.
	 *
	 * @param value reflected arg (usually [String] or [Collection] of strings)
	 * @param scope sibling vs element resolution root
	 * @param siblingSchema enclosing DTO schema
	 * @param elementSchema collection-element DTO schema
	 * @return OpenAPI-friendly value with wire names when resolvable	 
	 */
	private fun serializePropertyRefValue(
		value: Any?,
		scope: PropertyRefScope,
		siblingSchema: ObjectSchema?,
		elementSchema: ObjectSchema?,
	): Any? {
		
		val lookup = when (scope) {
			PropertyRefScope.SIBLING -> siblingSchema
			PropertyRefScope.ELEMENT -> elementSchema
		}
		
		return when (value) {
			null -> null
			
			is String -> OpenApiPropertyRefNames.toWire(lookup, value)
			
			is Collection<*> -> value.map { element ->
				if (element is String) OpenApiPropertyRefNames.toWire(lookup, element)
				else serializeValue(element)
			}
			
			is Array<*> -> value.map { element ->
				if (element is String) OpenApiPropertyRefNames.toWire(lookup, element)
				else serializeValue(element)
			}
			
			else -> serializeValue(value)
		}
	}
	
	/**
	 * Converts a reflected metadata property into JSON-friendly scalars, lists, or maps.
	 *
	 * Enums become names; classes become qualified names. Never serializes raw user input —
	 * only constraint **args**.
	 *
	 * @param value reflected property value, possibly nested
	 * @return OpenAPI-friendly form, or `null` when [value] is `null`	 
	 */
	private fun serializeValue(value: Any?): Any? = when (value) {
		null -> null
		is String, is Number, is Boolean -> value
		is Enum<*> -> value.name
		is KClass<*> -> value.qualifiedName ?: value.simpleName
		is Class<*> -> value.name
		is Collection<*> -> value.map { serializeValue(it) }
		is Array<*> -> value.map { serializeValue(it) }
		is IntArray -> value.toList()
		is LongArray -> value.toList()
		is DoubleArray -> value.toList()
		is FloatArray -> value.toList()
		is BooleanArray -> value.toList()
		is CharArray -> value.concatToString()
		is Map<*, *> -> value.entries.associate { (k, v) ->
			serializeValue(k)?.toString().orEmpty() to serializeValue(v)
		}
		else -> value.toString()
	}
	
	/**
	 * Cached reflective property used while serializing one metadata class.
	 *
	 * @property name property name published as the arg key
	 * @property propertyRef optional `@PropertyRef` for wire-name rewrite
	 * @property read extracts the raw value from a metadata instance
	 */
	private data class PropDescriptor(
		val name: String,
		val propertyRef: PropertyRef?,
		val read: (ConstraintMetadata) -> Any?)
}
