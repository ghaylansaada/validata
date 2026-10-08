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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.*

/**
 * Classifies Validata constraints that map to native OpenAPI / JSON Schema fields.
 *
 * Used by [ConstraintSchemaApplicator] for the unmapped path (standard + no owning documenter).
 * All constraints still appear under [ConstraintExtensionKeys.CONSTRAINTS] regardless of this
 * classification; customs that are not “standard” never go to `x-validata-unmapped`.
 *
 * Temporal membership (`@DaysOfWeek`, `@Months`, `@DaysOfMonth`) is owned by dedicated
 * documenters (allow / deny lists as `enum` / `not.enum`) but is still classified custom so
 * incomplete native coverage is not treated as a standard gap.*
 * 
 * @author Ghaylan Saada
 */
internal object StandardConstraintClassifier {
	
	/**
	 * Metadata classes that map fully to native OpenAPI / JSON Schema fields.
	 */
	private val STANDARD: Set<Class<*>> = setOf(
		RequiredConstraint::class.java,
		SizeConstraint::class.java,
		MinConstraint::class.java,
		MaxConstraint::class.java,
		RangeConstraint::class.java,
		MultipleOfConstraint::class.java,
		RegexConstraint::class.java,
		EmailConstraint::class.java,
		UrlConstraint::class.java,
		InConstraint::class.java,
		NotInConstraint::class.java)
	
	/**
	 * `true` when [metadata] is fully expressible as native OpenAPI facets.
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @return `true` for Size, Min, Max, Email, In, NotIn, and the other standard types	 
	 */
	fun isStandard(metadata: ConstraintMetadata): Boolean = metadata.javaClass in STANDARD
	
	/**
	 * `true` when [metadata] is not in the native-facet standard set.
	 *
	 * @param metadata constraint occurrence from Validata IR
	 * @return `true` when [isStandard] is false	 
	 */
	fun isCustom(metadata: ConstraintMetadata): Boolean = !isStandard(metadata)
}
