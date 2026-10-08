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
package io.ghaylan.validata.internal

import io.ghaylan.validata.schema.types.TypeTables
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry of terminal (non-DTO) types for schema / value-shape classification.
 *
 * Extracted from [ReflectionUtils] so leaf-package policy and host-registered wrappers live in one
 * place. Package prefixes come from schema [TypeTables.LEAF_PACKAGE_PREFIXES].*
 * 
 * @author Ghaylan Saada
 */
internal object LeafTypeRegistry {
	
	/**
	 * Package prefixes whose types are always terminal values, never traversable DTOs.
	 *
	 * Without this guard, [StructureClassifier.isObjectLike] accepts platform classes that expose
	 * instance fields (e.g. `UUID` with `mostSigBits` / `leastSigBits`); the schema builder then
	 * emits accessors that fail at request time with `InaccessibleObjectException` because
	 * `java.base` is not open to the unnamed module.	 
	 */
	private val leafPackagePrefixes = TypeTables.LEAF_PACKAGE_PREFIXES
	
	/**
	 * Host-registered terminal types (third-party value wrappers treated as scalars).
	 */
	private val additionalLeafTypes = ConcurrentHashMap.newKeySet<Class<*>>()
	
	/**
	 * Registers [type] as a terminal value so schema builders never traverse its properties.
	 *
	 * Side effect: mutates [additionalLeafTypes].
	 *
	 * @param type Class to treat as a leaf.	 
	 */
	fun register(type: Class<*>) {
		additionalLeafTypes.add(type)
	}
	
	/**
	 * Returns whether [type] must be treated as a terminal value rather than a traversable DTO.
	 *
	 * @param type Class under test.
	 * @return `true` when registered or under a [leafPackagePrefixes] namespace.	 
	 */
	fun isLeaf(type: Class<*>): Boolean {
		if (type in additionalLeafTypes) return true
		val packageName = type.`package`?.name ?: return false
		return leafPackagePrefixes.any {
			packageName == it.dropLast(1) || packageName.startsWith(it)
		}
	}
}
