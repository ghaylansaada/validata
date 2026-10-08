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
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.ScalarKinds
import java.lang.reflect.Modifier
import java.time.temporal.Temporal
import kotlin.reflect.KClass

/**
 * Structural classification for runtime reflection walks.
 *
 * Leaf / scalar / boxed / array **membership** comes from schema [KnownTypes] / [ScalarKinds]
 * (FQCN catalogs). This object only inspects live [Class] / [KClass] shapes
 * ([isObjectLike], assignability).
 *
 * @author Ghaylan Saada
 */
internal object StructureClassifier {

	/**
	 * Returns whether [a] and [b] are the same type or a primitive/boxed pair (schema catalogs).
	 */
	fun primitiveOrBoxedMatch(a: KClass<*>, b: KClass<*>): Boolean =
		KnownTypes.primitiveOrBoxedMatch(typeFqcn(a), typeFqcn(b))

	/**
	 * Returns whether [type] is a structured data object (DTO-like).
	 *
	 * Rejects primitives, collections, enums, interfaces, synthetics, and [LeafTypeRegistry]
	 * platforms. Accepts Kotlin data classes, Java records, and concrete classes with real
	 * instance fields.
	 */
	fun isObjectLike(type: Class<*>): Boolean {
		if (type == Unit::class.java ||
			type == Any::class.java ||
			type == Void::class.java ||
			type.isEnum || type.isArray ||
			type.isPrimitive ||
			type.isInterface ||
			type.isSynthetic ||
			Modifier.isAbstract(type.modifiers) ||
			Map::class.java.isAssignableFrom(type) ||
			Collection::class.java.isAssignableFrom(type) ||
			CharSequence::class.java.isAssignableFrom(type) ||
			Number::class.java.isAssignableFrom(type) ||
			Temporal::class.java.isAssignableFrom(type) ||
			LeafTypeRegistry.isLeaf(type)
		) {
			return false
		}

		if (runCatching { type.isRecord }.getOrDefault(false)) return true
		if (runCatching { type.kotlin.isData }.getOrDefault(false)) return true
		if (type.isAnonymousClass || type.isLocalClass) return false

		var current: Class<*>? = type
		while (current != null && current != Any::class.java) {
			val hasRealFields = current.declaredFields.any {
				!it.isSynthetic && !Modifier.isStatic(it.modifiers)
			}
			if (hasRealFields) return true
			current = current.superclass
		}
		return false
	}

	/**
	 * Infers structural shape only — never the leaf kind of array elements (T-26).
	 */
	fun determineStructure(type: KClass<*>): TypeStructure {
		return when {
			type == Nothing::class || type == Void::class -> TypeStructure.NOTHING
			Map::class.java.isAssignableFrom(type.java) -> TypeStructure.MAP
			type.java.isArray ||
				Collection::class.java.isAssignableFrom(type.java) ||
				KnownTypes.isArrayFqcn(typeFqcn(type)) -> TypeStructure.ARRAY
			isKnownScalar(type) -> TypeStructure.SCALAR
			isObjectLike(type.java) -> TypeStructure.OBJECT
			else -> TypeStructure.ANY
		}
	}

	/**
	 * Returns whether [type] is a platform leaf that must not fall through to [TypeStructure.ANY]
	 * or [TypeStructure.OBJECT].
	 */
	fun isKnownScalar(type: KClass<*>): Boolean {
		if (type.java.isEnum) return true
		val fqcn = typeFqcn(type)
		if (ScalarKinds.isScalarLeaf(fqcn)) return true
		// JVM primitive spelling (`int`) when qualifiedName is absent / erased.
		return ScalarKinds.isScalarLeaf(type.java.name)
	}

	/**
	 * Maps a scalar concrete type onto a [ScalarKind] via schema [ScalarKinds].
	 */
	fun determineScalarKind(type: KClass<*>): String {
		val fqcn = typeFqcn(type)
		val fromCatalog = ScalarKinds.of(fqcn)
		if (fromCatalog != ScalarKind.OTHER) return fromCatalog.name
		if (type.java.isEnum) return ScalarKind.ENUM.name
		return ScalarKinds.of(type.java.name).name
	}

	/** Prefer Kotlin FQCN (`kotlin.Int`); `Class.getName` is `int` for primitives. */
	internal fun typeFqcn(type: KClass<*>): String =
		type.qualifiedName ?: type.java.name
}
