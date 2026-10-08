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
package io.ghaylan.validata.golden

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.DistinctConstraint
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * L4 golden: [DistinctConstraint] as type-use on list elements (indexed error paths).
 *
 * @author Ghaylan Saada
 */
class DistinctListValidationTest {
	
	data class TagList(val tags: List<String>?)
	
	private val groups = setOf(OnDefault::class)
	
	private fun <M: ConstraintMetadata> compiled(
		meta: M,
		validator: ConstraintValidator<*, M>,
	) = CompiledConstraint(
		metadata = meta,
		runner = ValidatorBackedRunner(validator, meta),
		order = 0,
	)
	
	private fun schema(): ObjectSchema {
		val distinct = compiled(
			DistinctConstraint(
				by = emptySet(),
				message = "",
				groups = groups,
			),
			DistinctValidator,
		)
		return ObjectSchema(
			type = TagList::class.java,
			properties = listOf(
				PropertySpec(
					"tags",
					"tags",
					IterableShape(
						element = ScalarShape(
							kind = ScalarKind.STRING,
							constraints = listOf(distinct),
						),
					),
					{ (it as TagList).tags },
				),
			),
		)
	}
	
	@Nested
	@DisplayName("Distinct on List<@Distinct String>")
	inner class DistinctList {
		
		@Test
		@DisplayName("unique tags yield no errors")
		fun uniqueTags() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-distinct-valid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = TagList(tags = listOf("alpha", "beta")),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isEmpty()
		}
		
		@Test
		@DisplayName("duplicate tags fail with COLLECTION_DUPLICATE on indexed paths")
		fun duplicateTags() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-distinct-invalid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = TagList(tags = listOf("alpha", "alpha")),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isNotEmpty()
			assertThat(errors.map { it.path }).contains("tags[0]", "tags[1]")
			assertThat(errors).allMatch { it.code == ConstraintErrorCode.COLLECTION_DUPLICATE }
			assertThat(errors).allMatch { it.message == "Item is duplicated in the collection." }
		}
	}
}
