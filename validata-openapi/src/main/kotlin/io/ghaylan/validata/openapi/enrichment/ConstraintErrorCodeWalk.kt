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

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import kotlin.reflect.KClass

/**
 * Shared IR walk that emits constraint error definitions for OpenAPI collectors.
 *
 * Handles [CompositionConstraint] (adds [ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE] and
 * recurses into children) and leaf [ValidatorBackedRunner] validators.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintErrorCodeWalk {
	
	/**
	 * Visits every active compiled constraint in [constraints].
	 *
	 * @param type runtime subject class for [ConstraintValidator.possibleErrorCodes]
	 * @param constraints compiled constraints on a property or shape
	 * @param activeGroups endpoint groups; empty means all active
	 * @param onRequired invoked when an active `@Required` is seen
	 * @param onDefinition invoked for each error definition to publish
	 * @param onEmptyValidator invoked when a leaf validator declares an empty `possibleErrorCodes`	 
	 */
	fun forEachActive(
		type: Class<*>,
		constraints: List<CompiledConstraint>,
		activeGroups: Set<KClass<*>>,
		onRequired: () -> Unit = {},
		onDefinition: (ConstraintErrorDefinition) -> Unit,
		onEmptyValidator: (ConstraintValidator<*, *>) -> Unit = {},
	) {
		for (constraint in constraints) {
			
			if (!ConstraintGroupFilter.isActive(constraint.metadata, activeGroups)) {
				continue
			}
			
			if (constraint.metadata is RequiredConstraint) {
				onRequired()
			}
			
			if (constraint.metadata is CompositionConstraint) {
				onDefinition(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE)
				forEachActive(
					type = type,
					constraints = (constraint.metadata as CompositionConstraint).children,
					activeGroups = activeGroups,
					onRequired = onRequired,
					onDefinition = onDefinition,
					onEmptyValidator = onEmptyValidator)
				continue
			}
			
			val backed = constraint.runner as? ValidatorBackedRunner ?: continue
			
			val possible = backed.validator.resolvedPossibleErrorCodes(config = constraint.metadata, type = type)
			
			if (possible.isEmpty()) {
				onEmptyValidator(backed.validator)
			}
			else for (def in possible) {
				onDefinition(def)
			}
		}
	}
}
