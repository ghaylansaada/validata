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

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Resolves the FQCN of generated constraint metadata (Option 2 end state).
 *
 * Convention (locked): `{annotationPackage}.{AnnotationSimpleName}Constraint`.
 * There is no `@Constraint(metadata = …)` override — authors only declare the annotation class.*
 * 
 * @author Ghaylan Saada
 */
internal object MetadataFqcnResolver {
	
	/**
	 * Resolves `{annotationPackage}.{AnnotationSimpleName}Constraint` for [annotationDecl].
	 *
	 * No side effects.
	 *
	 * @param annotationDecl `@Constraint` annotation class declaration
	 * @return generated metadata FQCN by convention	 
	 */
	fun conventionFqcn(annotationDecl: KSClassDeclaration): String {
		val pkg = annotationDecl.packageName.asString()
		val simple = annotationDecl.simpleName.asString() + "Constraint"
		return if (pkg.isBlank()) simple else "$pkg.$simple"
	}
}
