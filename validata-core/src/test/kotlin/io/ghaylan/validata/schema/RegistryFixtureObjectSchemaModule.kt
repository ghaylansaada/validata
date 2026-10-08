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
package io.ghaylan.validata.schema

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.schema.spi.ObjectSchemaModule

/**
 * Test-only [ObjectSchemaModule] contributing fixture DTOs for registry / standalone engine tests.
 * 
 * @author Ghaylan Saada
 */
class RegistryFixtureObjectSchemaModule: ObjectSchemaModule {
	
	override fun schemas(): Map<Class<*>, ObjectSchema> {
		val constraint = RequiredConstraint(
			mode = Required.Mode.STRICT,
			message = "",
			groups = setOf(OnDefault::class),
		)
		val compiled = CompiledConstraint(
			constraint,
			ValidatorBackedRunner(RequiredValidator, constraint),
			0,
		)
		return mapOf(
			RegistryFixtureDto::class.java to ObjectSchema(
				type = RegistryFixtureDto::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "value",
						externalName = "value",
						shape = ScalarShape(ScalarKind.STRING),
						read = { (it as RegistryFixtureDto).value },
						constraints = listOf(compiled),
					),
				),
			),
			TwoRequiredFieldsDto::class.java to ObjectSchema(
				type = TwoRequiredFieldsDto::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "left",
						externalName = "left",
						shape = ScalarShape(ScalarKind.STRING),
						read = { (it as TwoRequiredFieldsDto).left },
						constraints = listOf(compiled),
					),
					PropertySpec(
						declaredName = "right",
						externalName = "right",
						shape = ScalarShape(ScalarKind.STRING),
						read = { (it as TwoRequiredFieldsDto).right },
						constraints = listOf(compiled),
					),
				),
			),
		)
	}
}

data class RegistryFixtureDto(val value: String?)

/** Two Required string fields for standalone [ValidationOptions] failFast / collect tests.
 * 
 * @author Ghaylan Saada
 */
data class TwoRequiredFieldsDto(
	val left: String?,
	val right: String?
)
