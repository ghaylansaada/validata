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
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.validator.comparison.compare.CompareValidator
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.required.RequiredWhenValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.constraint.validator.size.CollectionSizeValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.engine.support.NaturalPathOrder
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
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
 * L4 golden: RequiredWhen gate + EqualTo siblings + container/element Size on one DTO walk.
 * 
 * @author Ghaylan Saada
 */
class MultiConstraintWalkValidationTest {
	
	data class GatedSignup(
		val gate: String?,
		val payload: String?,
		val password: String?,
		val confirm: String?,
		val tags: List<String>?,
	)
	
	private val groups = setOf(OnDefault::class)
	
	private fun <M: ConstraintMetadata> compiled(
		meta: M,
		validator: ConstraintValidator<*, M>,
		order: Int,
	) = CompiledConstraint(
		metadata = meta,
		runner = ValidatorBackedRunner(validator, meta),
		order = order,
	)
	
	private fun schema(): ObjectSchema {
		val required = compiled(RequiredConstraint(Required.Mode.STRICT, "", groups), RequiredValidator, 0)
		val requiredWhen = compiled(
			RequiredWhenConstraint(
				ref = "gate",
				condition = RequiredWhen.Condition.EQ,
				value = "YES",
				values = emptySet(),
				mode = Required.Mode.STRICT,
				message = "",
				groups = groups,
			),
			RequiredWhenValidator,
			0,
		)
		val passwordSize = compiled(SizeConstraint(8, 64, "", groups), CharSequenceSizeValidator, 1)
		val equalTo = compiled(
			CompareConstraint("password", Compare.Operation.EQ, "", groups),
			CompareValidator,
			2,
		)
		val listSize = compiled(SizeConstraint(2, 5, "", groups), CollectionSizeValidator, 0)
		val tagSize = compiled(SizeConstraint(2, 20, "", groups), CharSequenceSizeValidator, 1)
		
		return ObjectSchema(
			type = GatedSignup::class.java,
			properties = listOf(
				PropertySpec(
					"gate",
					"gate",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as GatedSignup).gate },
				),
				PropertySpec(
					"payload",
					"payload",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as GatedSignup).payload },
					constraints = listOf(requiredWhen),
				),
				PropertySpec(
					"password",
					"password",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as GatedSignup).password },
					constraints = listOf(required, passwordSize),
				),
				PropertySpec(
					"confirm",
					"confirm",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as GatedSignup).confirm },
					constraints = listOf(required, equalTo),
				),
				PropertySpec(
					"tags",
					"tags",
					IterableShape(
						element = ScalarShape(ScalarKind.STRING, constraints = listOf(tagSize)),
					),
					ValueReader { (it as GatedSignup).tags },
					constraints = listOf(listSize),
				),
			),
		)
	}
	
	@Nested
	@DisplayName("RequiredWhen + EqualTo + Size walk")
	inner class Walk {
		
		@Test
		@DisplayName("valid gated signup yields no errors")
		fun validPayload() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-multi-valid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = GatedSignup(
					gate = "YES",
					payload = "detail",
					password = "password1",
					confirm = "password1",
					tags = listOf("alpha", "beta"),
				),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isEmpty()
		}
		
		@Test
		@DisplayName("invalid payload reports expected paths and codes")
		fun invalidPayload() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-multi-invalid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = GatedSignup(
					gate = "YES",
					payload = null,
					password = "short",
					confirm = "other",
					tags = listOf("a"),
				),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isNotEmpty()
			assertThat(errors.map { it.path }).contains(
				"payload",
				"password",
				"confirm",
				"tags",
				"tags[0]",
			)
			assertThat(errors.map { it.code }).contains(
				ConstraintErrorCode.VALUE_MISSING,
				ConstraintErrorCode.TEXT_TOO_SHORT,
				ConstraintErrorCode.COMPARISON_UNSATISFIED_NOT_EQUAL,
				ConstraintErrorCode.COLLECTION_TOO_SMALL,
			)
			val paths = errors.map { it.path.orEmpty() }
			assertThat(paths).isSortedAccordingTo(Comparator(NaturalPathOrder::compare))
		}
	}
}
