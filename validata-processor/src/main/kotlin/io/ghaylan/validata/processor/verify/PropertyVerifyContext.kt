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
package io.ghaylan.validata.processor.verify

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.ghaylan.validata.processor.model.SchemaModel

/**
 * Shared collaborators for one [PropertyReferenceVerifier.verify] walk over a schema.
 *
 * Avoids threading long parameter lists through nested constraint / shape helpers.
 *
 * @property error emits a KSP diagnostic for the current verification site
 * @property schemaPathChecks sibling-path and subject scalar-kind checks
 * @property elementRefChecks element-path existence checks for collection constraints
 * @property schemasByQualifiedName all schemas built in this round for nested object walks*
 * 
 * @author Ghaylan Saada
 */
internal class PropertyVerifyContext(
	val error: (String) -> Unit,
	val schemaPathChecks: PropertySchemaPathChecks,
	val elementRefChecks: PropertyElementRefChecks,
	val schemasByQualifiedName: Map<String, SchemaModel>,
) {
	
	companion object {
		
		/**
		 * Builds a context wired to [logger] and [site] for one schema verification pass.
		 *
		 * Side effects: none.
		 *
		 * @param logger KSP diagnostics sink.
		 * @param site Optional symbol attached to emitted errors.
		 * @param maxShapeNestingDepth Cap for nested iterable/map shape walks.
		 * @param schemasByQualifiedName All schemas built in this round.
		 * @return Context shared by nested verify helpers.		 
		 */
		fun create(
			logger: KSPLogger,
			site: KSNode?,
			maxShapeNestingDepth: Int,
			schemasByQualifiedName: Map<String, SchemaModel>,
		): PropertyVerifyContext {
			val error: (String) -> Unit = { logger.error(it, site) }
			val schemaPathChecks = PropertySchemaPathChecks(error)
			val elementRefChecks = PropertyElementRefChecks(
				error = error,
				maxShapeNestingDepth = maxShapeNestingDepth,
				pathChecks = schemaPathChecks,
			)
			return PropertyVerifyContext(
				error = error,
				schemaPathChecks = schemaPathChecks,
				elementRefChecks = elementRefChecks,
				schemasByQualifiedName = schemasByQualifiedName,
			)
		}
	}
}
