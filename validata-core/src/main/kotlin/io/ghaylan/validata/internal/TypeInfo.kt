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

import io.ghaylan.validata.schema.shape.ScalarKind
import kotlin.reflect.KClass

/**
 * Reflective view of a Java/Kotlin type: structure, concrete class, and nested type arguments.
 *
 * Used for catalog identity keys and a few runtime scalar guards. Schema construction is
 * compile-time (KSP); this type remains the shared key shape for [ReflectionUtils.infoFromClass]
 * / [ReflectionUtils.infoFromType].
 *
 * ### Shape vs leaf kind (T-26)
 * [structure] answers "how do I traverse this?" — scalar, object, map, or array.
 * [scalarKind] answers "what leaf is this?" when [structure] is [TypeStructure.SCALAR].
 * Array element kind is never folded into [structure]; it lives on [typeArguments].
 *
 * Leaf kind codes match [ScalarKind] names (`"STRING"`, `"INTEGRAL"`, …)
 * so this type stays free of a `schema` package import (audit 4.6).
 *
 * @property rawRootType Generalized root abstraction (e.g. `List` → `Collection`).
 * @property concreteType Fully resolved Kotlin class of the runtime type.
 * @property structure Structural shape of this type.
 * @property scalarKind Leaf kind code when [structure] is [TypeStructure.SCALAR]; `null`
 *    otherwise. Values mirror `ScalarKind.name` in validata-schema.
 * @property typeArguments Nested generic / component types (recursive).
 * 
 * @author Ghaylan Saada
 */
data class TypeInfo(
	val rawRootType: KClass<*>,
	val concreteType: KClass<*>,
	val structure: TypeStructure,
	val scalarKind: String? = null,
	val typeArguments: List<TypeInfo> = emptyList(),
) {
	
	/**
	 * `true` when [structure] is [TypeStructure.OBJECT].
	 */
	val isObject: Boolean get() = structure == TypeStructure.OBJECT
	
	/**
	 * `true` when this is an array/collection whose element [TypeInfo] is an object.
	 */
	val isArrayOfObjects: Boolean get() = isArray && typeArguments.firstOrNull()?.isObject == true
	
	/**
	 * `true` when this is an array/collection whose element [TypeInfo] is a map.
	 */
	val isArrayOfMaps: Boolean get() = isArray && typeArguments.firstOrNull()?.isMap == true
	
	/**
	 * `true` when this is an array of objects, maps, or nested arrays.
	 */
	val isArrayOfNonScalar: Boolean
		get() = isArrayOfObjects || isArrayOfMaps || isArrayOfArrays
	
	/**
	 * `true` when [structure] is [TypeStructure.MAP].
	 */
	val isMap: Boolean get() = structure == TypeStructure.MAP
	
	/**
	 * `true` when [structure] is [TypeStructure.SCALAR].
	 */
	val isScalar: Boolean get() = structure == TypeStructure.SCALAR
	
	/**
	 * `true` when this is an array/collection of scalar leaves.
	 */
	val isArrayOfScalars: Boolean get() = isArray && typeArguments.firstOrNull()?.isScalar == true
	
	/**
	 * `true` when this is a multi-dimensional array/collection.
	 */
	val isArrayOfArrays: Boolean get() = isArray && typeArguments.firstOrNull()?.isArray == true
	
	/**
	 * `true` when [structure] is [TypeStructure.ARRAY].
	 */
	val isArray: Boolean get() = structure == TypeStructure.ARRAY
	
	/**
	 * Core type this structure represents: element [concreteType] for arrays, else [concreteType].
	 */
	val resolveType: KClass<*>
		get() = if (isArray) typeArguments.firstOrNull()?.concreteType ?: concreteType
		else concreteType
	
	/**
	 * Element class when this is an array; `null` otherwise.
	 */
	val arrayElemType: KClass<*>?
		get() = if (isArray) typeArguments.firstOrNull()?.concreteType else null
}
