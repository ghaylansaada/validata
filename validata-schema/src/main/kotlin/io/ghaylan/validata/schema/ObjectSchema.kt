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
package io.ghaylan.validata.schema

import io.ghaylan.validata.schema.shape.ObjectRefShape
import java.util.*

/**
 * Compiled object-graph IR for one DTO type: properties, readers, and constraints (no request-time reflection).
 *
 * Emitted by KSP or a hand-written `ObjectSchemaModule`; distinct from request `EndpointSchema`. Does not run validators.
 * Construction rejects duplicate [PropertySpec.declaredName] or [PropertySpec.externalName] values.
 * [properties] and [subtypes] are frozen snapshots — mutating the lists/maps passed to the constructor
 * after construction does not affect this instance or its indexes.
 *
 * @property type Runtime class this schema describes.
 * @property properties Properties in stable declaration order.
 * @property subtypes Concrete schemas for sealed / `@Validatable.Subtype` entries; empty when non-polymorphic.*
 * 
 * @author Ghaylan Saada
 */
class ObjectSchema(
	val type: Class<*>,
	properties: List<PropertySpec>,
	subtypes: Map<Class<*>, ObjectSchema> = emptyMap(),
) {
	
	/**
	 * Properties in stable declaration order (unmodifiable snapshot).	 
	 */
	val properties: List<PropertySpec> = Collections.unmodifiableList(ArrayList(properties))
	
	/**
	 * Concrete schemas for sealed / subtype entries (unmodifiable snapshot).	 
	 */
	val subtypes: Map<Class<*>, ObjectSchema> = if (subtypes.isEmpty()) emptyMap()
	else Collections.unmodifiableMap(LinkedHashMap(subtypes))
	
	init {
		ensureUniquePropertyNames(this.properties, PropertySpec::declaredName, "declaredName")
		ensureUniquePropertyNames(this.properties, PropertySpec::externalName, "externalName")
	}
	
	/**
	 * Lazily built [ObjectRefShape] pointing at this schema (publication-safe, once per instance).	 
	 */
	val selfRef: ObjectRefShape by lazy(LazyThreadSafetyMode.PUBLICATION) {
		ObjectRefShape(lazyOf(this))
	}
	
	/**
	 * Lazily built index of [properties] by [PropertySpec.declaredName] (1:1; uniqueness enforced in `init`).
	 */
	val byDeclaredName: Map<String, PropertySpec> by lazy(LazyThreadSafetyMode.PUBLICATION) {
		this.properties.associateBy(PropertySpec::declaredName)
	}
	
	/**
	 * Lazily built index of [properties] by [PropertySpec.externalName] (1:1; uniqueness enforced in `init`).
	 */
	val byExternalName: Map<String, PropertySpec> by lazy(LazyThreadSafetyMode.PUBLICATION) {
		this.properties.associateBy(PropertySpec::externalName)
	}
	
	/**
	 * Compact diagnostic: type name plus property and subtype counts.
	 *
	 * No side effects.
	 *
	 * @return `"ObjectSchema(…)"` string for logs	 
	 */
	override fun toString(): String = "ObjectSchema(${type.name}, props=${this.properties.size}, subtypes=${this.subtypes.size})"
	
	/**
	 * Rejects duplicate keys in [properties] for the given name selector.
	 *
	 * Mutates only local maps.
	 *
	 * @param properties schema properties in declaration order
	 * @param nameOf extracts the name under uniqueness check
	 * @param label field label in the failure message (`declaredName` / `externalName`)
	 * @throws IllegalStateException when the same name appears on more than one property	 
	 */
	private fun ensureUniquePropertyNames(
		properties: List<PropertySpec>,
		nameOf: (PropertySpec) -> String,
		label: String,
	) {
		val firstIndexByName = HashMap<String, Int>(properties.size)
		for (index in properties.indices) {
			val name = nameOf(properties[index])
			val previousIndex = firstIndexByName.putIfAbsent(name, index)
			if (previousIndex != null) {
				error("Duplicate $label '$name' on ObjectSchema for ${type.name} " +
						"(properties[$previousIndex] and properties[$index]). " +
						"Emit unique $label values from KSP or your ObjectSchemaModule — " +
						"each property must have a distinct $label.")
			}
		}
	}
}
