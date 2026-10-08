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
package io.ghaylan.validata.openapi.springdoc

/**
 * Filters annotation lists before springdoc applies Bean Validation → schema mapping.
 *
 * springdoc matches Bean Validation annotations by **simple name** then casts to
 * `jakarta.validation.*`. Validata ships its own `@Size` / `@Min` / `@Max` (and peers) in the
 * constraint-annotation package, which collide and throw [ClassCastException].
 * OpenAPI enrichment must ignore those; Validata IR mappers document them instead.*
 * 
 * @author Ghaylan Saada
 */
internal object JakartaValidationAnnotationFilter {

	/**
	 * Keeps only Jakarta / Hibernate Validator annotations from [annotations].
	 *
	 * @param annotations annotations springdoc collected for a parameter or request body; may be null
	 * @return a new mutable list of kept annotations, or `null` when [annotations] is null
	 */
	fun onlyJakartaOrHibernate(annotations: List<Annotation>?): MutableList<Annotation>? {
		if (annotations == null) return null
		// Single-pass filterTo — avoids filter()'s intermediate List plus a second toMutableList() copy.
		return annotations.filterTo(ArrayList(annotations.size), ::isJakartaOrHibernateValidation)
	}

	/**
	 * Whether [annotation] is a Jakarta Validation or Hibernate Validator annotation.
	 *
	 * @param annotation candidate annotation (may be a JDK proxy)
	 * @return `true` when the annotation type lives under Jakarta Validation or Hibernate Validator
	 */
	fun isJakartaOrHibernateValidation(annotation: Annotation): Boolean {
		// Accessing .annotationClass.java resolves the actual annotation interface,
        // avoiding the generic proxy class (jdk.proxy*) and avoiding java.lang.annotation.Annotation.
		val pkg = annotation.annotationClass.java.packageName
		return pkg.startsWith("jakarta.validation") || pkg.startsWith("org.hibernate.validator")
	}
}
