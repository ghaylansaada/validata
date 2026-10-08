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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.ConstraintMeta

/**
 * Resolves `@Constraint(validatedBy = …)` meta for a usage- or meta-annotation type.
 *
 * Metadata FQCN is always `{annotationPackage}.{AnnotationSimpleName}Constraint`.
 * Results are cached by annotation-type FQCN for the round.*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintMetaResolver {
	
	/**
	 * Resolves `@Constraint(validatedBy = …)` meta for a usage- or meta-annotation type.
	 *
	 * Mutates [cache] on miss (including negative `null` hits).
	 *
	 * @param ann a usage site such as `@Required` on a field
	 * @param cache round-scoped FQCN → meta
	 * @return parsed meta, or `null` if [ann] is not a constraint annotation	 
	 */
	fun resolve(
		ann: KSAnnotation,
		cache: MutableMap<String, ConstraintMeta?>,
	): ConstraintMeta? {
		val decl = ann.annotationType.resolve().declaration as? KSClassDeclaration
			?: return null
		val typeFqcn = decl.qualifiedName?.asString()
			?: return null
		if (cache.containsKey(typeFqcn)) return cache[typeFqcn]
		val constraintAnn = decl.annotations.firstOrNull {
			AnnotationFqcn.isA(it, ProcessorFqns.CONSTRAINT)
		}
		val meta = if (constraintAnn == null) {
			null
		}
		else {
			val validators = mutableListOf<String>()
			for (arg in constraintAnn.arguments) {
				when (arg.name?.asString()) {
					AnnotationAttrs.Constraint.VALIDATED_BY, null -> {
						@Suppress("UNCHECKED_CAST")
						val arr = arg.value as? List<KSType>
						arr?.forEach { type ->
							type.declaration.qualifiedName?.asString()?.takeIf { it.isNotBlank() }?.let { validators += it }
						}
					}
				}
			}
			ConstraintMeta(MetadataFqcnResolver.conventionFqcn(decl), validators)
		}
		cache[typeFqcn] = meta
		return meta
	}
}
