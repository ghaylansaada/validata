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
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.ConstraintValidatorResolvedTypes
import io.ghaylan.validata.processor.compat.TypeView
import io.ghaylan.validata.processor.model.ConstraintMeta
import io.ghaylan.validata.processor.verify.ConstraintArgHost
import io.ghaylan.validata.processor.verify.PropertyRefHost

/**
 * Round-scoped resolver + caches for [ConstraintModelBuilder].
 *
 * Replaced on each `beginRound`; must not retain KS symbols past the round that created them.
 *
 * @property resolver current KSP resolver for the active round*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintRoundSession(
	val resolver: Resolver,
) {
	
	/**
	 * Validator FQCN → resolved `ConstraintValidator<V, C>` type args, including negative hits.
	 */
	val resolvedValidatorCache = HashMap<String, ConstraintValidatorResolvedTypes?>()
	
	/**
	 * Annotation-type FQCN → parsed `@Constraint` meta, including negative hits.
	 */
	val constraintMetaCache = HashMap<String, ConstraintMeta?>()
	
	/**
	 * Generated metadata FQCN → metadata class declaration, including negative hits.
	 */
	val metadataDeclCache = HashMap<String, KSClassDeclaration?>()
	
	/**
	 * Annotation FQCN → extracted `@PropertyRef` hosts for one usage shape.
	 */
	val propertyRefHostCache = HashMap<String, List<PropertyRefHost>>()
	
	/**
	 * Annotation FQCN → extracted `@ConstraintArg` hosts for one usage shape.
	 */
	val constraintArgHostCache = HashMap<String, List<ConstraintArgHost>>()
	
	/**
	 * Metadata FQCN → (param name → is Set-like).
	 */
	val metadataParamSetCache = HashMap<String, Map<String, Boolean>>()
	
	/**
	 * FQCN of `makeNotNullable` type → [TypeView].
	 */
	val typeViewCache = HashMap<String, TypeView>()
	
	/**
	 * Owner FQCN → property name → type (RequiredWhen gate index, once per owner per round).
	 */
	val ownerPropertyTypeCache = HashMap<String, Map<String, KSType>>()
	
	companion object {
		
		/**
		 * [MutableMap.getOrPut] does not cache `null` values; round caches need that contract.
		 *
		 * Mutates [cache] on miss.
		 *
		 * @param cache round-scoped map that may store `null` results
		 * @param key lookup key
		 * @param compute supplier invoked only on first miss for [key]
		 * @return cached or freshly computed value, which may be `null`		 
		 */
		inline fun <K, V> cachedNullable(
			cache: MutableMap<K, V?>,
			key: K,
			compute: () -> V?,
		): V? {
			if (cache.containsKey(key)) return cache[key]
			val value = compute()
			cache[key] = value
			return value
		}
	}
}
