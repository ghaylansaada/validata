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

import io.ghaylan.validata.internal.ReflectionUtils.IS_SCALAR_CACHE_SOFT_MAX
import io.ghaylan.validata.internal.ReflectionUtils.isScalarByClass
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Runtime type classification helpers for catalog identity and scalar guards.
 *
 * Schema construction no longer uses these APIs — KSP emits TypeInfo-shaped catalog keys and
 * `ObjectSchema` graphs at compile time. What remains is value-shape classification ([isScalar],
 * [isCollection], …) and [infoFromClass]/[infoFromType] for registry / test helpers.
 *
 * Leaf-package / host-leaf registration lives in [LeafTypeRegistry]. [TypeInfo] construction lives
 * in [TypeInfoFactory]; structural / scalar-kind / DTO-likeness live in [StructureClassifier].
 *
 * Public façade for catalog identity helpers: KSP-generated constraint catalogs and hosts call
 * [infoFromClass] / [infoFromType].*
 * 
 * @author Ghaylan Saada
 */
object ReflectionUtils {
	
	/**
	 * Fallback [TypeInfo] for erased / unknown wildcards.
	 */
	private val wildcardType = TypeInfo(
		rawRootType = Any::class,
		concreteType = Any::class,
		structure = TypeStructure.ANY)
	
	/**
	 * Soft upper bound on [isScalarByClass] entries.
	 *
	 * Soft (not hard): when at capacity, new classes are classified without being inserted so the
	 * map cannot grow without bound under adversarial / dynamic class loads.	 
	 */
	private const val IS_SCALAR_CACHE_SOFT_MAX: Int = 512
	
	/**
	 * Bounded [Class] → scalar? memo for the [isScalar] / [isObjectLike] hot path.
	 *
	 * See [IS_SCALAR_CACHE_SOFT_MAX]. Concurrent; safe for shared [ReflectionUtils] use.	 
	 */
	private val isScalarByClass = ConcurrentHashMap<Class<*>, Boolean>(64)
	
	/**
	 * Registers [type] as a terminal value so schema builders never traverse its properties.
	 *
	 * @param type Class to treat as a leaf.	 
	 */
	fun registerLeafType(type: Class<*>) {
		LeafTypeRegistry.register(type)
	}
	
	/**
	 * Returns whether [type] must be treated as a terminal value rather than a traversable DTO.
	 *
	 * @param type Class under test.
	 * @return `true` when registered or under a leaf-package namespace.	 
	 */
	fun isLeafType(type: Class<*>): Boolean = LeafTypeRegistry.isLeaf(type)
	
	/**
	 * Builds [TypeInfo] from a raw Java [Type] (e.g. field or method return type).
	 *
	 * @param type Java type to analyze.
	 * @return Structured [TypeInfo] representation.	 
	 */
	fun infoFromType(type: Type): TypeInfo = TypeInfoFactory.buildTypeInfo(type)
	
	/**
	 * Builds [TypeInfo] from a raw Java [Class], using its generic superclass when useful.
	 *
	 * @param clazz Class to analyze.
	 * @return Structured [TypeInfo] representation.	 
	 */
	fun infoFromClass(clazz: Class<*>): TypeInfo = TypeInfoFactory.buildTypeInfo(clazz)
	
	/**
	 * Returns whether [type] is assignable from [Map].
	 *
	 * @param type Java class to check.
	 * @return `true` if [type] is a map or subclass of a map.	 
	 */
	fun isMapLike(type: Class<*>): Boolean = Map::class.java.isAssignableFrom(type)
	
	/**
	 * Returns whether [type] is a structured data object (DTO-like).
	 *
	 * Delegates to [StructureClassifier.isObjectLike].
	 *
	 * @param type Class to inspect.
	 * @return `true` when [type] is likely a traversable data object.	 
	 */
	fun isObjectLike(type: Class<*>): Boolean = StructureClassifier.isObjectLike(type)
	
	/**
	 * Returns whether [type] is an array or a [Collection].
	 *
	 * @param type Class to inspect.
	 * @return `true` when [type] is array-like.	 
	 */
	fun isCollectionLike(type: Class<*>): Boolean = type.isArray || Collection::class.java.isAssignableFrom(type)
	
	/**
	 * Returns whether [value] is any array or collection type (including primitive arrays).
	 *
	 * @param value Runtime value to classify.
	 * @return `true` when [value]'s class is collection-like.	 
	 */
	fun isCollection(value: Any): Boolean = isCollectionLike(value.javaClass)
	
	/**
	 * Returns whether [type] represents a [Collection] (not necessarily a [List]).
	 *
	 * @param type Reflective type info.
	 * @return `true` when [TypeInfo.concreteType] is a collection.	 
	 */
	fun isTypeInfoCollectionLike(type: TypeInfo): Boolean = Collection::class.java.isAssignableFrom(type.concreteType.java)
	
	/**
	 * Returns whether [type] represents a [Map].
	 *
	 * @param type Reflective type info.
	 * @return `true` when [TypeInfo.concreteType] is a map.	 
	 */
	fun isTypeInfoMapLike(type: TypeInfo): Boolean = Map::class.java.isAssignableFrom(type.concreteType.java)
	
	/**
	 * Returns whether [type] is an erased wildcard (`Any` / unknown type argument).
	 *
	 * @param type Reflective type info.
	 * @return `true` when [type] is the wildcard sentinel or [Any].	 
	 */
	fun isWildcard(type: TypeInfo): Boolean = type.concreteType == Any::class || type == wildcardType
	
	/**
	 * Returns whether [a] and [b] are the same type or a primitive/boxed pair.
	 *
	 * @param a First class.
	 * @param b Second class.
	 * @return `true` on identity or boxed match via schema [KnownTypes.primitiveOrBoxedMatch].	 
	 */
	fun primitiveOrBoxedMatch(
		a: KClass<*>,
		b: KClass<*>
	): Boolean = StructureClassifier.primitiveOrBoxedMatch(a, b)
	
	/**
	 * Returns whether [type] is assignable to [Number].
	 *
	 * @param type Class under test.
	 * @return `true` when [type] is numeric.	 
	 */
	fun isNumericType(type: KClass<*>): Boolean = Number::class.java.isAssignableFrom(type.java)
	
	/**
	 * Returns whether [type] is numeric and implements [Comparable].
	 *
	 * @param type Class under test.
	 * @return `true` when both numeric and comparable.	 
	 */
	fun isComparableNumeric(type: KClass<*>): Boolean = isNumericType(type) && Comparable::class.java.isAssignableFrom(type.java)
	
	/**
	 * Returns whether [value] is a scalar (string, char, number, temporal, boolean, enum, or Date).
	 *
	 * Uses [isScalarByClass] so repeated [isObjectLike] work is paid once per class
	 * (soft-capped; see [IS_SCALAR_CACHE_SOFT_MAX]).
	 *
	 * @param value Runtime value to classify.
	 * @return `true` when not a collection and not [isObjectLike].	 
	 */
	fun isScalar(value: Any): Boolean {
		val clazz = value.javaClass
		if (isCollectionLike(clazz)) return false
		isScalarByClass[clazz]?.let { return it }
		val scalar = !isObjectLike(clazz)
		if (isScalarByClass.size < IS_SCALAR_CACHE_SOFT_MAX) {
			isScalarByClass.putIfAbsent(clazz, scalar)
		}
		return scalar
	}
}
