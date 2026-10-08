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
package io.ghaylan.validata.samples.constraint

import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Sample [ConstraintDocumentation] for [OddYearsConstraint].
 *
 * No native OpenAPI facets — still published as an `OddYears` entry under `x-validata-constraints` by
 * the OpenAPI enricher. Returning [ConstraintDocHints.EMPTY] avoids `x-validata-unmapped`.*
 * 
 * @author Ghaylan Saada
 */
class OddYearsConstraintDocumentation: ConstraintDocumentation {
	
	/**
	 * @param metadata constraint metadata from the schema IR
	 * @return `true` when [metadata] is [OddYearsConstraint]	 
	 */
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata is OddYearsConstraint
	
	/**
	 * @param metadata must be [OddYearsConstraint] when [supports] was true
	 * @param shape unused; OddYears has no native JSON Schema facet
	 * @return [ConstraintDocHints.EMPTY] so the enricher does not mark the property unmapped	 
	 */
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints = ConstraintDocHints.EMPTY
}
