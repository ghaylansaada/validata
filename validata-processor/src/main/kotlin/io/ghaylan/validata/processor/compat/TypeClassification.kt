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
package io.ghaylan.validata.processor.compat

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.analyze.ShapeModelBuilder
import io.ghaylan.validata.processor.model.DynamicShapeModel
import io.ghaylan.validata.processor.model.IterableShapeModel
import io.ghaylan.validata.processor.model.MapShapeModel
import io.ghaylan.validata.processor.model.ScalarShapeModel
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.ScalarKinds
import io.ghaylan.validata.schema.types.TypeTables
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Classifies KSP types into buckets [ShapeModelBuilder] understands.
 *
 * FQCN leaf / scalar policy comes from schema [ScalarKinds] / [KnownTypes]; this object adds
 * KSP supertype walks for concrete JDK collections/maps (`HashMap`, `ArrayList`, …).*
 * 
 * @author Ghaylan Saada
 */
internal object TypeClassification {
	
	/**
	 * Supertype FQCNs that mean map-shaped for cascade and shape emission.
	 */
	private val MAP_SUPERTYPES = TypeTables.MAP_FQCNS
	
	/**
	 * Supertype FQCNs that mean “collection-shaped” (list / set / collection — not map/array).
	 */
	private val COLLECTION_SUPERTYPES = TypeTables.COLLECTION_FQCNS + setOf(
		TypeNames.LIST_JAVA, TypeNames.SET_JAVA, TypeNames.COLLECTION_JAVA)
	
	/**
	 * Whether [type] should become a [MapShapeModel].
	 *
	 * Recognizes Kotlin `Map` / `MutableMap`, `java.util.Map*`, and concrete maps that implement
	 * those interfaces (`HashMap`, `LinkedHashMap`, …).
	 *
	 * Side effects: none.
	 *
	 * @param type Resolved property or type-argument type.
	 * @return `true` when [type] is a map.	 
	 */
	fun isMap(type: KSType): Boolean {
		val qualifiedName = type.declaration.qualifiedName?.asString()
			?: return false
		val isKnownMapType = qualifiedName in MAP_SUPERTYPES
		val isJavaMapType = qualifiedName.startsWith(TypeNames.MAP_JAVA)
		val inheritsMap = hasSupertype(type, MAP_SUPERTYPES)
		
		return isKnownMapType || isJavaMapType || inheritsMap
	}
	
	/**
	 * Whether [type] should become an [IterableShapeModel]
	 * (list / set / collection — not map/array).
	 *
	 * Arrays are handled by [isArray] so element typing can differ. Concrete JDK lists/sets
	 * (`ArrayList`, `HashSet`, …) are recognized via their collection supertypes.
	 *
	 * Side effects: none.
	 *
	 * @param type Resolved property or type-argument type.
	 * @return `true` when [type] is an iterable collection.	 
	 */
	fun isIterable(type: KSType): Boolean {
		val isExcluded = isArray(type) || isMap(type)
		val isIterable = isIterableAssumingNotMap(type)
		return !isExcluded && isIterable
	}
	
	/**
	 * Like [isIterable] but skips the [isMap] check — caller already knows [type] is not a map
	 * (e.g. after a failed map branch in shape building).
	 *
	 * Side effects: none.
	 *
	 * @param type Resolved property or type-argument type.
	 * @return `true` when [type] is an iterable collection and not an array.	 
	 */
	fun isIterableAssumingNotMap(type: KSType): Boolean {
		if (isArray(type)) return false
		val qualifiedName = type.declaration.qualifiedName?.asString()
			?: return false
		val isCollectionType = qualifiedName in COLLECTION_SUPERTYPES
		val isJavaList = qualifiedName.startsWith(TypeNames.LIST_JAVA)
		val isJavaSet = qualifiedName.startsWith(TypeNames.SET_JAVA)
		val isJavaCollection = qualifiedName.startsWith(TypeNames.COLLECTION_JAVA)
		val inheritsCollection = hasSupertype(type, COLLECTION_SUPERTYPES)
		
		return isCollectionType || isJavaList || isJavaSet || isJavaCollection || inheritsCollection
	}
	
	/**
	 * Whether [type] is a Kotlin or JVM array (`kotlin.Array`, `IntArray`, …).
	 *
	 * Only `kotlin.Array` and Kotlin primitive-array types (`IntArray`, …) — not arbitrary
	 * FQCNs that happen to end with `Array` (e.g. a user DTO `PhotoArray`).
	 *
	 * Arrays are emitted as iterable shapes so the engine walks elements like lists.
	 *
	 * Side effects: none.
	 *
	 * @param type Resolved property or type-argument type.
	 * @return `true` when [type] is an array.	 
	 */
	fun isArray(type: KSType): Boolean =
		isArrayFqcn(type.declaration.qualifiedName?.asString())
	
	/**
	 * FQCN variant of [isArray] for tests and callers that already have a qualified name.
	 *
	 * Side effects: none.
	 *
	 * @param q Fully qualified type name, or `null`.
	 * @return `true` when [q] is `kotlin.Array` or a Kotlin primitive array.	 
	 */
	fun isArrayFqcn(q: String?): Boolean =
		KnownTypes.isArrayFqcn(q)
	
	/**
	 * Assignable-supertype FQCNs for [type] (excludes self), depth-capped and round-cached.
	 *
	 * Shared by map/iterable classification and [TypeView] ranking so hierarchy walks are paid once
	 * per [KSType] instance in a processor round.
	 *
	 * Side effects: may populate [ProcessorRoundCache] when a round is active.
	 *
	 * @param type Type whose assignable supertypes to collect.
	 * @return Supertype FQCN set excluding [type] itself.	 
	 */
	fun assignableSupertypes(type: KSType): Set<String> {
		val key = type.makeNotNullable()
		ProcessorRoundCache.getSupertypes(key)?.let { return it }
		val out = LinkedHashSet<String>(8)
		collectSupertypes(key, out, depth = 0)
		ProcessorRoundCache.putSupertypes(key, out)
		return out
	}
	
	/**
	 * Whether [type]'s hierarchy includes any FQCN in [targets] (depth-capped, cycle-safe).
	 *
	 * Side effects: may populate the round type-hierarchy cache.
	 *
	 * @param type Type whose hierarchy to inspect.
	 * @param targets Candidate supertype FQCNs.
	 * @return `true` when any [targets] entry appears in [type]'s cached supers.	 
	 */
	private fun hasSupertype(
		type: KSType,
		targets: Set<String>
	): Boolean {
		val supers = assignableSupertypes(type)
		return targets.any { it in supers }
	}
	
	/**
	 * Walks [type]'s declared supertypes up to depth 8, collecting FQCNs into [out].
	 *
	 * Side effects: mutates [out].
	 *
	 * @param type Current type node in the hierarchy walk.
	 * @param out Accumulator for discovered supertype FQCNs.
	 * @param depth Current recursion depth (stops above 8).	 
	 */
	private fun collectSupertypes(
		type: KSType,
		out: MutableSet<String>,
		depth: Int
	) {
		if (depth > 8) return
		val decl = type.declaration as? KSClassDeclaration
			?: return
		for (superType in decl.superTypes) {
			val resolved = superType.resolve().makeNotNullable()
			val name = resolved.declaration.qualifiedName?.asString()
				?: continue
			if (out.add(name)) {
				collectSupertypes(resolved, out, depth + 1)
			}
		}
	}
	
	/**
	 * Whether [qName] is a scalar leaf (stdlib / JDK), including all `java.time.*` types.
	 *
	 * Scalars become [ScalarShapeModel]; they are never cascade targets.
	 *
	 * Side effects: none.
	 *
	 * @param qName Fully qualified type name, e.g. `kotlin.String`.
	 * @return `true` when [qName] is a scalar leaf.	 
	 */
	fun isScalar(qName: String): Boolean =
		ScalarKinds.isScalarLeaf(qName)
	
	/**
	 * Whether [qName] is a JDK / Kotlin platform type that must not be treated as a user DTO.
	 *
	 * Used before cascade checks: when `true`, emit
	 * [DynamicShapeModel] (or a scalar) instead of
	 * requiring `@Validatable`. `kotlin.Any` is not a platform leaf here so unmarked `Any` stays dynamic.
	 *
	 * Side effects: none.
	 *
	 * @param qName Fully qualified type name, or `null` when KSP could not resolve the type.
	 * @return `true` if cascade / object-ref logic must not apply.	 
	 */
	fun isPlatformLeaf(qName: String?): Boolean =
		ScalarKinds.isPlatformLeaf(qName)
	
	/**
	 * Maps a leaf FQCN to a [ScalarKind] for [ScalarShapeModel]
	 * and generated `ScalarKind.…` emission.
	 *
	 * Delegates to schema [ScalarKinds] (single source of truth with IntelliJ / runtime).
	 *
	 * Side effects: none.
	 *
	 * @param qName Fully qualified leaf type name.
	 * @return Matching [ScalarKind], or [ScalarKind.OTHER] when the leaf is not a known scalar.	 
	 */
	fun scalarKind(qName: String): ScalarKind =
		ScalarKinds.of(qName)
}
