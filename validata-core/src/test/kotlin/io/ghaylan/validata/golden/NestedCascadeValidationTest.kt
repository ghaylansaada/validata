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
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ObjectRefShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * L4 golden: [ObjectRefShape] cascades into nested Required violations; [ScalarShape] does not.
 *
 * Mirrors compile-time `@NoCascade` (opaque nested object): only the ref shape walks child fields.
 * See [ValidatorEngine.cascadeIntoShape].
 * 
 * @author Ghaylan Saada
 */
class NestedCascadeValidationTest {
	
	data class Child(val name: String?)
	
	data class Parent(
		val cascadingChild: Child?,
		val opaqueChild: Child?,
	)
	
	private val groups = setOf(OnDefault::class)
	
	private fun <M: ConstraintMetadata> compiled(
		meta: M,
		validator: ConstraintValidator<*, M>,
	) = CompiledConstraint(
		metadata = meta,
		runner = ValidatorBackedRunner(validator, meta),
		order = 0,
	)
	
	private fun childSchema(): ObjectSchema {
		val required = compiled(
			RequiredConstraint(Required.Mode.STRICT, "", groups),
			RequiredValidator,
		)
		return ObjectSchema(
			type = Child::class.java,
			properties = listOf(
				PropertySpec(
					"name",
					"name",
					ScalarShape(ScalarKind.STRING),
					ValueReader { (it as Child).name },
					constraints = listOf(required),
				),
			),
		)
	}
	
	private fun schema(): ObjectSchema {
		val child = childSchema()
		return ObjectSchema(
			type = Parent::class.java,
			properties = listOf(
				PropertySpec(
					"cascadingChild",
					"cascadingChild",
					ObjectRefShape(lazy { child }),
					ValueReader { (it as Parent).cascadingChild },
				),
				PropertySpec(
					"opaqueChild",
					"opaqueChild",
					ScalarShape(ScalarKind.OTHER),
					ValueReader { (it as Parent).opaqueChild },
				),
			),
		)
	}
	
	@Nested
	@DisplayName("ObjectRefShape cascade vs ScalarShape leaf")
	inner class CascadeBehaviour {
		
		@Test
		@DisplayName("ObjectRefShape cascades Required into nested name path")
		fun objectRefCascades() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-cascade-ref",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = Parent(cascadingChild = Child(name = null), opaqueChild = Child(name = null)),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).hasSize(1)
			assertThat(errors.single().path).isEqualTo("cascadingChild.name")
			assertThat(errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("ScalarShape does not cascade into nested child fields")
		fun scalarShapeDoesNotCascade() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-cascade-scalar",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = Parent(cascadingChild = Child(name = "ok"), opaqueChild = Child(name = null)),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors.map { it.path.orEmpty() }).noneMatch { it.startsWith("opaqueChild.") }
		}
		
		@Test
		@DisplayName("valid nested values yield no errors")
		fun validNested() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-cascade-valid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = Parent(
					cascadingChild = Child(name = "a"),
					opaqueChild = Child(name = "b"),
				),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isEmpty()
		}
	}
}
