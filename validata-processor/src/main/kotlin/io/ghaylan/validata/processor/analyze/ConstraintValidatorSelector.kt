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
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.ConstraintValidatorResolvedTypes
import io.ghaylan.validata.processor.compat.ConstraintValidatorTypeResolver
import io.ghaylan.validata.processor.compat.TypeView
import io.ghaylan.validata.processor.compat.ValidatorCompatibility

/**
 * Picks one `validatedBy` validator FQCN for a subject [KSType] using [ValidatorCompatibility].*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintValidatorSelector(
	private val typeResolver: ConstraintValidatorTypeResolver,
) {
	
	/**
	 * Scores each candidate with [ValidatorCompatibility.scoreFit] and returns the best FQCN.
	 *
	 * Ties break by FQCN so the outcome never depends on annotation-array iteration order.
	 * Misses are cached as `null` in [resolvedValidatorCache] (same contract as the former
	 * inlined [ConstraintModelBuilder] helper).
	 *
	 * [TypeView]s are cached in [typeViewCache] keyed by the FQCN of `makeNotNullable` types.
	 *
	 * Mutates [resolvedValidatorCache] and [typeViewCache] on miss.
	 *
	 * @param candidates validator FQCNs from `@Constraint(validatedBy = …)`
	 * @param valueType subject value type
	 * @param resolver current KSP resolver
	 * @param resolvedValidatorCache round cache of resolved validator types
	 * @param typeViewCache round cache of [TypeView] by type FQCN
	 * @return best-matching validator FQCN, or `null` when none fit	 
	 */
	fun select(
		candidates: List<String>,
		valueType: KSType,
		resolver: Resolver,
		resolvedValidatorCache: MutableMap<String, ConstraintValidatorResolvedTypes?>,
		typeViewCache: MutableMap<String, TypeView>,
	): String? {
		if (candidates.isEmpty()) return null
		val actual = valueType.makeNotNullable()
		val actualView = cachedTypeView(actual, typeViewCache)
		val scored = candidates.mapNotNull { fqn ->
			val resolved = ConstraintRoundSession.cachedNullable(resolvedValidatorCache, fqn) {
				val decl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(fqn))
					?: return@cachedNullable null
				typeResolver.resolve(decl)
			}
				?: return@mapNotNull null
			if (typeResolver.isUnresolvedTypeParameter(resolved.valueType)) return@mapNotNull null
			val validatorV = resolved.valueType.makeNotNullable()
			val validatorView = cachedTypeView(validatorV, typeViewCache)
			val rank = ValidatorCompatibility.scoreFit(actualView, validatorView)
				?: return@mapNotNull null
			rank to fqn
		}
		return pickBest(scored)
	}
	
	companion object {
		
		/**
		 * Lowest rank wins; ties break by FQCN for stable selection.
		 *
		 * No side effects.
		 *
		 * @param scored pairs of (compatibility rank, validator FQCN)
		 * @return winning validator FQCN, or `null` when [scored] is empty		 
		 */
		fun pickBest(scored: List<Pair<Int, String>>): String? =
			scored.minWithOrNull(compareBy({ it.first }, { it.second }))?.second
		
		/**
		 * Returns a cached [TypeView] for [type], keyed by its declaration FQCN.
		 *
		 * Mutates [cache] on miss.
		 *
		 * @param type KSP type to view
		 * @param cache round-scoped type view cache
		 * @return [TypeView] for validator compatibility scoring		 
		 */
		private fun cachedTypeView(
			type: KSType,
			cache: MutableMap<String, TypeView>,
		): TypeView {
			val key = type.declaration.qualifiedName?.asString()
				?: return TypeView.from(type)
			return cache.getOrPut(key) { TypeView.from(type) }
		}
	}
}
