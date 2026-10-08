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

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation

/**
 * Resolves annotation type FQCNs with optional per-round caching via [ProcessorRoundCache].
 *
 * Prefer this over repeating `ann.annotationType.resolve().declaration.qualifiedName` in hot loops.*
 * 
 * @author Ghaylan Saada
 */
internal object AnnotationFqcn {
	
	/**
	 * Fully qualified name of [ann]'s annotation type, or `null` when unresolved.
	 *
	 * Side effects: may populate [ProcessorRoundCache].
	 *
	 * @param ann Annotation instance to resolve.
	 * @return Annotation type FQCN, or `null` when KSP cannot resolve it.	 
	 */
	fun of(ann: KSAnnotation): String? =
		ProcessorRoundCache.annotationFqcn(ann)
	
	/**
	 * Whether [ann]'s type FQCN equals [expected].
	 *
	 * Side effects: may populate [ProcessorRoundCache].
	 *
	 * @param ann Annotation instance to test.
	 * @param expected Expected annotation type FQCN.
	 * @return `true` when [ann] is an instance of [expected].	 
	 */
	fun isA(
		ann: KSAnnotation,
		expected: String
	): Boolean =
		ProcessorRoundCache.annotationIs(ann, expected)
}

/**
 * Whether this symbol carries an annotation whose type FQCN equals [fqcn].
 *
 * Side effects: may populate [ProcessorRoundCache] while scanning [annotations].
 *
 * @param fqcn Annotation type FQCN to match.
 * @return `true` when any attached annotation resolves to [fqcn].
 * */
internal fun KSAnnotated.has(fqcn: String): Boolean =
	annotations.any { AnnotationFqcn.isA(it, fqcn) }
