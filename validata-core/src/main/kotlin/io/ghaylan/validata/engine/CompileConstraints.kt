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
package io.ghaylan.validata.engine

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Turns a metadata→validator map into an ordered IR constraint list.
 *
 * Exists as a top-level factory (not a method on [ValidatorBackedRunner]) so call sites that only
 * need compilation — tests, hand-written schemas — do not drag the runner type into their imports
 * as an implicit companion. Order follows map iteration; callers that need a stable order must
 * pass a [LinkedHashMap] or sort beforehand.
 *
 * Allocates new [CompiledConstraint] / [ValidatorBackedRunner] instances; no shared-state mutation.
 *
 * @param constraints Metadata instances paired with their validator implementations.
 * @return [CompiledConstraint] list with sequential [CompiledConstraint.order] indices.
 * */
fun compileConstraints(
	constraints: Map<ConstraintMetadata, ConstraintValidator<*, *>>,
): List<CompiledConstraint> {
	return constraints.entries.mapIndexed { index, (metadata, validator) ->
		CompiledConstraint(
			order = index,
			metadata = metadata,
			runner = ValidatorBackedRunner(validator, metadata))
	}
}
