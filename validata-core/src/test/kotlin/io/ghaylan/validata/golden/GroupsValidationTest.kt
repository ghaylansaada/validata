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
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * L4 golden: endpoint [EndpointSchema.groups] filters constraints.
 *
 * Groups are supplied via [EngineTestSupport.bodyRequestSchema]; [ValidatorEngine.validateRequest]
 * reads them from the schema (not a separate parameter).
 * 
 * @author Ghaylan Saada
 */
class GroupsValidationTest {
	
	data class GroupedBody(
		val createOnly: String?,
		val defaultOnly: String?,
	)
	
	private fun <M: ConstraintMetadata> compiled(
		meta: M,
		validator: ConstraintValidator<*, M>,
	) = CompiledConstraint(
		metadata = meta,
		runner = ValidatorBackedRunner(validator, meta),
		order = 0,
	)
	
	private fun schema(): ObjectSchema {
		val onCreate = compiled(
			RequiredConstraint(Required.Mode.STRICT, "", setOf(OnCreate::class)),
			RequiredValidator,
		)
		val onDefault = compiled(
			RequiredConstraint(Required.Mode.STRICT, "", setOf(OnDefault::class)),
			RequiredValidator,
		)
		return ObjectSchema(
			type = GroupedBody::class.java,
			properties = listOf(
				PropertySpec(
					"createOnly",
					"createOnly",
					ScalarShape(ScalarKind.STRING),
					{ (it as GroupedBody).createOnly },
					constraints = listOf(onCreate),
				),
				PropertySpec(
					"defaultOnly",
					"defaultOnly",
					ScalarShape(ScalarKind.STRING),
					{ (it as GroupedBody).defaultOnly },
					constraints = listOf(onDefault),
				),
			),
		)
	}
	
	@Nested
	@DisplayName("OnCreate vs OnDefault")
	inner class ActiveGroups {
		
		@Test
		@DisplayName("OnCreate endpoint validates only createOnly field")
		fun onCreateFiresCreateOnly() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				groups = setOf(OnCreate::class),
				id = "golden-groups-create",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = GroupedBody(createOnly = null, defaultOnly = null),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).hasSize(1)
			assertThat(errors.single().path).isEqualTo("createOnly")
			assertThat(errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("OnDefault endpoint validates only defaultOnly field")
		fun onDefaultFiresDefaultOnly() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				groups = setOf(OnDefault::class),
				id = "golden-groups-default",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = GroupedBody(createOnly = null, defaultOnly = null),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).hasSize(1)
			assertThat(errors.single().path).isEqualTo("defaultOnly")
			assertThat(errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("matching active group yields no errors")
		fun validWhenGroupMatches() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				groups = setOf(OnCreate::class),
				id = "golden-groups-valid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = GroupedBody(createOnly = "new", defaultOnly = null),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isEmpty()
		}
	}
}
