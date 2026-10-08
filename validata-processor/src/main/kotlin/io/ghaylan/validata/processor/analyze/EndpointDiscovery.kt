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
import com.google.devtools.ksp.symbol.*
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.naming.EndpointIdentifier

/**
 * Discovers effective `@Validate` handler functions for endpoint schema generation.
 *
 * ## Precedence
 * Method-level `@Validate` wins over class-level. Class-level only applies to members that
 * carry a Spring mapping annotation (same rule as runtime discovery).*
 * 
 * @author Ghaylan Saada
 */
internal object EndpointDiscovery {
	
	/**
	 * Collects candidate handlers keyed by [EndpointIdentifier], skipping [skipIdentifiers]
	 * and synthetic `$default` bridges.
	 *
	 * No side effects.
	 *
	 * @param resolver current KSP resolver
	 * @param skipIdentifiers endpoint ids to omit from the result map
	 * @return map of identifier → (function, effective `@Validate` annotation)	 
	 */
	fun discover(
		resolver: Resolver,
		skipIdentifiers: Set<String> = emptySet(),
	): Map<String, Pair<KSFunctionDeclaration, KSAnnotation>> {
		val validateRequestSymbols = resolver.getSymbolsWithAnnotation(ProcessorFqns.VALIDATE).toList()
		val functionDirect = validateRequestSymbols.filterIsInstance<KSFunctionDeclaration>()
		val classLevel = validateRequestSymbols.filterIsInstance<KSClassDeclaration>()
		val candidates = LinkedHashMap<String, Pair<KSFunctionDeclaration, KSAnnotation>>()
		
		for (fn in functionDirect) {
			if (fn.origin == Origin.SYNTHETIC) continue
			val ann = findAnnotation(fn, ProcessorFqns.VALIDATE)
				?: continue
			val id = EndpointIdentifier.of(fn)
			if (id in skipIdentifiers) continue
			candidates[id] = fn to ann
		}
		
		for (cls in classLevel) {
			val classAnn = findAnnotation(cls, ProcessorFqns.VALIDATE)
				?: continue
			for (member in cls.getAllFunctions()) {
				if (member.origin == Origin.SYNTHETIC) continue
				if (!hasSpringMapping(member)) continue
				val id = EndpointIdentifier.of(member)
				if (id in skipIdentifiers) continue
				if (candidates.containsKey(id)) continue
				val methodAnn = findAnnotation(member, ProcessorFqns.VALIDATE)
				candidates[id] = member to (methodAnn
					?: classAnn)
			}
		}
		return candidates
	}
	
	/** `true` when [fn] carries a Spring `@*Mapping` / `@RequestMapping`.
	 *
	 * No side effects.
	 *
	 * @param fn handler candidate
	 * @return `true` when any Spring mapping annotation is present	 
	 */
	fun hasSpringMapping(fn: KSFunctionDeclaration): Boolean =
		fn.annotations.any {
			AnnotationFqcn.of(it) in ProcessorFqns.SPRING_MAPPING_ANNOTATIONS
		}
	
	/** First annotation on [annotated] whose type FQCN equals [fqcn], or `null`.
	 *
	 * No side effects.
	 *
	 * @param annotated KSP node carrying annotations
	 * @param fqcn expected annotation type FQCN
	 * @return first matching annotation, or `null`	 
	 */
	fun findAnnotation(
		annotated: KSAnnotated,
		fqcn: String
	): KSAnnotation? =
		annotated.annotations.firstOrNull {
			AnnotationFqcn.isA(it, fqcn)
		}
}
