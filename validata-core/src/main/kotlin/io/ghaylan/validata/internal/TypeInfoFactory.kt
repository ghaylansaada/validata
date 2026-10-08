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

import io.ghaylan.validata.internal.TypeInfoFactory.extractRootAbstraction
import io.ghaylan.validata.schema.types.KnownTypes
import java.lang.reflect.*
import kotlin.reflect.KClass

/**
 * [TypeInfo] construction extracted from [ReflectionUtils].
 *
 * Recursively walks Java [Type] / [Class] graphs (including generics and arrays) into the
 * [TypeInfo] tree used for catalog identity and registry helpers.*
 * 
 * @author Ghaylan Saada
 */
internal object TypeInfoFactory {
	
	/**
	 * Shared fallback type information for unresolved or unsupported types.
	 */
	private val wildcardType = TypeInfo(
		rawRootType = Any::class,
		concreteType = Any::class,
		structure = TypeStructure.ANY)
	
	
	/**
	 * Recursively builds [TypeInfo] from [type], using [visited] to break cyclic generics.
	 *
	 * @param type Type to inspect.
	 * @param visited Mutable set of already-seen types (cycle prevention).
	 * @return Fully resolved [TypeInfo] tree.	 
	 */
	fun buildTypeInfo(
		type: Type,
		visited: MutableSet<Type> = mutableSetOf(),
	): TypeInfo {
		if (!visited.add(type)) {
			return wildcardType
		}
		
		return when (type) {
			is ParameterizedType -> {
				val raw = (type.rawType as? Class<*>)?.kotlin ?: Any::class
				val nestedTypes = type.actualTypeArguments.map { buildTypeInfo(it, visited) }
				typeInfo(
					rawRootType = extractRootAbstraction(raw),
					concreteType = raw,
					typeArguments = nestedTypes)
			}
			
			is GenericArrayType -> {
				val componentType = buildTypeInfo(
					type = type.genericComponentType,
					visited = visited)
				typeInfo(
					rawRootType = Array::class,
					concreteType = getPrimitiveArrayClass(componentType.concreteType),
					typeArguments = listOf(componentType))
			}
			
			is Class<*> -> {
				val kClass = type.kotlin
				
				if (type.isEnum) {
					return typeInfo(
						rawRootType = kClass,
						concreteType = kClass,
						typeArguments = emptyList())
				}
				
				when {
					type.isArray -> {
						val componentType = buildTypeInfo(type.componentType, visited)
						typeInfo(
							rawRootType = Array::class,
							concreteType = getPrimitiveArrayClass(componentType.concreteType),
							typeArguments = listOf(componentType))
					}
					
					StructureClassifier.isObjectLike(type) -> {
						typeInfo(
							rawRootType = extractRootAbstraction(type.kotlin),
							concreteType = type.kotlin,
							typeArguments = emptyList())
					}
					
					else -> {
						val genericSuper = type.genericSuperclass
						if (genericSuper is ParameterizedType) {
							return buildTypeInfo(type = genericSuper, visited = visited)
						}
						val plain = type.kotlin
						typeInfo(
							rawRootType = extractRootAbstraction(plain),
							concreteType = plain,
							typeArguments = emptyList())
					}
				}
			}
			
			is WildcardType -> {
				buildTypeInfo(
					type = type.upperBounds.firstOrNull() ?: Any::class.java,
					visited = visited)
			}
			
			is TypeVariable<*> -> {
				buildTypeInfo(
					type = type.bounds.firstOrNull() ?: Any::class.java,
					visited = visited)
			}
			
			else -> {
				wildcardType
			}
		}
	}
	
	/**
	 * Builds a [TypeInfo] with [TypeStructure] and optional leaf kind code from [concreteType] /
	 * [typeArguments] — never from a combined shape+element enum.
	 *
	 * @param rawRootType Root abstraction ([extractRootAbstraction] result).
	 * @param concreteType Fully resolved Kotlin class.
	 * @param typeArguments Nested generic / component types.
	 * @return New [TypeInfo] with derived [TypeInfo.structure] / [TypeInfo.scalarKind].	 
	 */
	private fun typeInfo(
		rawRootType: KClass<*>,
		concreteType: KClass<*>,
		typeArguments: List<TypeInfo> = emptyList(),
	): TypeInfo {
		val structure = StructureClassifier.determineStructure(concreteType)
		
		val scalarKind = if (structure == TypeStructure.SCALAR) {
			StructureClassifier.determineScalarKind(concreteType)
		}
		else null
		
		return TypeInfo(
			rawRootType = rawRootType,
			concreteType = concreteType,
			structure = structure,
			scalarKind = scalarKind,
			typeArguments = typeArguments)
	}
	
	/**
	 * Returns the Kotlin primitive array class for [component], or `Array<Any>` when not primitive.
	 *
	 * @param component Element type, or `null`.
	 * @return Matching primitive array [KClass], or [Array].	 
	 */
	private fun getPrimitiveArrayClass(component: KClass<*>?): KClass<*> {
		return when (component) {
			Float::class -> FloatArray::class
			Double::class -> DoubleArray::class
			Int::class -> IntArray::class
			Long::class -> LongArray::class
			Short::class -> ShortArray::class
			Boolean::class -> BooleanArray::class
			Char::class -> CharArray::class
			Byte::class -> ByteArray::class
			else -> Array<Any>::class
		}
	}
	
	/**
	 * Returns the root abstraction of [type]: [Array], [Collection], [Map], or [type] itself.
	 *
	 * @param type Raw Kotlin type.
	 * @return Root type abstraction for [TypeInfo.rawRootType].	 
	 */
	private fun extractRootAbstraction(type: KClass<*>): KClass<*> {
		return when {
			type.java.isArray || KnownTypes.isArrayFqcn(StructureClassifier.typeFqcn(type)) -> Array::class
			Collection::class.java.isAssignableFrom(type.java) -> Collection::class
			Map::class.java.isAssignableFrom(type.java) -> Map::class
			else -> type
		}
	}
}
