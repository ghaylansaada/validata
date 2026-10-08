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
package io.ghaylan.validata.engine.fastpath

import io.ghaylan.validata.constraint.annotation.DistinctConstraint
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator
import io.ghaylan.validata.constraint.validator.size.CollectionSizeValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [ArrayContextFastPath] must skip wrappers for size-only lists and keep them for sibling-aware
 * validators such as [DistinctValidator], including Distinct nested under composition metadata.
 * 
 * @author Ghaylan Saada
 */
class ArrayContextFastPathTest {
	
	private val stringList = IterableShape(element = ScalarShape(ScalarKind.STRING))
	private val groups = setOf(OnDefault::class)
	
	@Test
	@DisplayName("size-only property constraints do not need array context")
	fun sizeOnlySkipsArrayContext() {
		val size = SizeConstraint(min = 1, max = 3, message = "", groups = groups)
		val compiled = CompiledConstraint(
			metadata = size,
			runner = ValidatorBackedRunner(CollectionSizeValidator, size),
			order = 0,
		)
		assertThat(ArrayContextFastPath.needsArrayContext(listOf(compiled), stringList)).isFalse()
	}
	
	@Test
	@DisplayName("Distinct on the property requires array context")
	fun distinctRequiresArrayContext() {
		val distinct = DistinctConstraint(by = emptySet(), message = "", groups = groups)
		val compiled = CompiledConstraint(
			metadata = distinct,
			runner = ValidatorBackedRunner(DistinctValidator, distinct),
			order = 0,
		)
		assertThat(ArrayContextFastPath.needsArrayContext(listOf(compiled), stringList)).isTrue()
	}
	
	@Test
	@DisplayName("Composition wrapping Distinct still needs array context")
	fun compositionWrappingDistinctNeedsArrayContext() {
		val distinct = DistinctConstraint(by = emptySet(), message = "", groups = groups)
		val distinctCompiled = CompiledConstraint(
			metadata = distinct,
			runner = ValidatorBackedRunner(DistinctValidator, distinct),
			order = 0,
		)
		val composition = CompositionConstraint(
			message = "",
			groups = groups,
			children = listOf(distinctCompiled),
		)
		val compiled = CompiledConstraint(
			metadata = composition,
			runner = ValidatorBackedRunner(DistinctValidator, distinct),
			order = 0,
		)
		assertThat(ArrayContextFastPath.needsArrayContext(listOf(compiled), stringList)).isTrue()
	}
	
	@Test
	@DisplayName("DistinctValidator declares requiresArrayContext")
	fun distinctFlag() {
		assertThat(DistinctValidator.requiresArrayContext).isTrue()
		assertThat(CollectionSizeValidator.requiresArrayContext).isFalse()
	}
}
