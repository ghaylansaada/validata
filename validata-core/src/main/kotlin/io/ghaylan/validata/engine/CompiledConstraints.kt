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
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.constraint.composition.CompositionOrRunner
import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Factories for [CompiledConstraint] used by KSP-generated schemas and hand-built tests.
 *
 * Ensures the same metadata instance is bound to both [CompiledConstraint.metadata] and the
 * runner — without `run { }` locals in generated source.
 *
 * @author Ghaylan Saada
 */
object CompiledConstraints {

	/**
	 * Leaf constraint: [validator] + [metadata] under [ValidatorBackedRunner].
	 *
	 * @param validator Catalog validator (usually a Kotlin `object`).
	 * @param metadata Occurrence metadata for this site.
	 * @param order Zero-based order among sibling constraints.
	 * @return Ready-to-run compiled constraint.
	 */
	fun of(
		validator: ConstraintValidator<*, *>,
		metadata: ConstraintMetadata,
		order: Int,
	): CompiledConstraint = CompiledConstraint(
		metadata = metadata,
		runner = ValidatorBackedRunner(validator, metadata),
		order = order)

	/**
	 * OR composition: [metadata] under [CompositionOrRunner].
	 *
	 * @param metadata Composition site with nested leaf children.
	 * @param order Zero-based order among sibling constraints.
	 * @return Ready-to-run compiled composition constraint.
	 */
	fun or(
		metadata: CompositionConstraint,
		order: Int,
	): CompiledConstraint = CompiledConstraint(
		metadata = metadata,
		runner = CompositionOrRunner(metadata),
		order = order)
}
