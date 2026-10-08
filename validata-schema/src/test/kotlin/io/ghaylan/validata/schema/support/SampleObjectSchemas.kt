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
package io.ghaylan.validata.schema.support

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape

/**
 * Minimal [ObjectSchema] factories for object-graph and object-SPI tests.
 *
 * Intentionally tiny — enough for ServiceLoader / merge / lookup tests without mirroring
 * production DTO graphs. Prefer hand-built schemas in path/shape suites when structure matters.*
 * 
 * @author Ghaylan Saada
 */
object SampleObjectSchemas {
	
	/**
	 * Builds a one-property [ObjectSchema].
	 *
	 * @param type runtime class recorded on the schema (defaults to [String])	 
	 */
	fun of(type: Class<*> = String::class.java): ObjectSchema =
		ObjectSchema(
			type = type,
			properties = listOf(
				PropertySpec(
					declaredName = "x",
					externalName = "x",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
				),
			),
		)
}
