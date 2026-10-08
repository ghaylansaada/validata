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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.constraint.composition.CompositionOrRunner
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.groups.OnDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [CompiledConstraints] factories.
 *
 * @author Ghaylan Saada
 */
class CompiledConstraintsTest {

	@Test
	@DisplayName("of shares the same metadata instance with ValidatorBackedRunner")
	fun ofSharesMetadata() {
		val meta = RequiredConstraint(
			mode = Required.Mode.STRICT,
			message = "",
			groups = setOf(OnDefault::class),
		)
		val compiled = CompiledConstraints.of(RequiredValidator, meta, order = 3)
		assertThat(compiled.metadata).isSameAs(meta)
		assertThat(compiled.order).isEqualTo(3)
		val runner = compiled.runner as ValidatorBackedRunner
		assertThat(runner.metadata).isSameAs(meta)
		assertThat(runner.validator).isSameAs(RequiredValidator)
	}

	@Test
	@DisplayName("or shares CompositionConstraint with CompositionOrRunner")
	fun orSharesMetadata() {
		val leaf = CompiledConstraints.of(
			RequiredValidator,
			RequiredConstraint(mode = Required.Mode.STRICT, message = "", groups = setOf(OnDefault::class)),
			order = 0,
		)
		val composition = CompositionConstraint(
			message = "either",
			groups = setOf(OnDefault::class),
			children = listOf(leaf),
		)
		val compiled = CompiledConstraints.or(composition, order = 1)
		assertThat(compiled.metadata).isSameAs(composition)
		assertThat(compiled.order).isEqualTo(1)
		val runner = compiled.runner as CompositionOrRunner
		assertThat(runner.composition).isSameAs(composition)
	}
}
