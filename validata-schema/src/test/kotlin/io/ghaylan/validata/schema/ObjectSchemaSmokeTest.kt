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

import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ObjectRefShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.schema.support.metadata.RequiredConfig
import io.ghaylan.validata.schema.support.metadata.RequiredRunner
import io.ghaylan.validata.schema.support.dto.Node
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Module-local smoke for object-graph IR.
 *
 * Proves producers can build [ObjectSchema] / [PropertySpec] / [ObjectRefShape] graphs with only
 * `validata-schema` on the classpath. Does not run validators or assert HTTP outcomes — that
 * belongs in `validata-core` / `validata`.
 *
 * Run: `./gradlew :validata-schema:test`
 * 
 * @author Ghaylan Saada

 */
class ObjectSchemaSmokeTest {

	@Test
	@DisplayName("constructs ObjectSchema with ValueReader, property constraints, and ObjectRefShape cycles")
	fun constructsObjectGraphIr() {
		lateinit var schema: ObjectSchema
		schema = ObjectSchema(
			type = Node::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "name",
					externalName = "name",
					shape = ScalarShape(ScalarKind.STRING),
					read = { (it as Node).name },
					constraints = listOf(
						CompiledConstraint(
							metadata = RequiredConfig,
							runner = RequiredRunner,
							order = 0,
						),
					),
				),
				PropertySpec(
					declaredName = "child",
					externalName = "child",
					// Lazy self-reference: schema graph cycle without infinite build-time expansion.
					shape = ObjectRefShape(lazy { schema }),
					read = { (it as Node).child },
				),
			),
		)

		val node = Node(name = "root", child = null)
		val nameProperty = schema.byDeclaredName.getValue("name")

		assertThat(nameProperty.read.read(node)).isEqualTo("root")
		assertThat(nameProperty.constraints).hasSize(1)
		assertThat(nameProperty.constraints.single().order).isEqualTo(0)

		val nested = (schema.byDeclaredName.getValue("child").shape as ObjectRefShape).ref.value
		assertThat(nested).isSameAs(schema)
	}
}
