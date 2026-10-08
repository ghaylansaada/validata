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
package io.ghaylan.validata.openapi.support

import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.openapi.springdoc.ValidataModelConverter
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.schema.spi.ObjectSchemaModule

/** Test DTO whose generated schema is contributed by [OpenApiProbeObjectSchemaModule].
 * 
 * @author Ghaylan Saada
 */
class OpenApiProbeDto(
	val name: String? = null,
)

/**
 * Test-only [ObjectSchemaModule] so [ValidataModelConverter]
 * can overlay Size facets without depending on `:validata`.
 * 
 * @author Ghaylan Saada
 */
class OpenApiProbeObjectSchemaModule: ObjectSchemaModule {
	
	override fun schemas(): Map<Class<*>, ObjectSchema> {
		val size = SizeConstraint(2, 40, "", setOf(OnDefault::class))
		return mapOf(
			OpenApiProbeDto::class.java to ObjectSchema(
				type = OpenApiProbeDto::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "name",
						externalName = "name",
						shape = ScalarShape(ScalarKind.STRING),
						read = { (it as OpenApiProbeDto).name },
						constraints = listOf(
							CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 0),
						),
					),
				),
			),
		)
	}
}
