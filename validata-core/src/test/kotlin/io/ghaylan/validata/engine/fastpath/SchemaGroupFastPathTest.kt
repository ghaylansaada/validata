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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [SchemaGroupFastPath] skip eligibility — only when every constraint is already active.
 * 
 * @author Ghaylan Saada
 */
class SchemaGroupFastPathTest {
	
	@Test
	@DisplayName("default-only active groups skip when every constraint includes OnDefault")
	fun defaultCompatibleSkips() {
		val schema = objectSchema(
			RequiredConstraint(
				mode = Required.Mode.STRICT,
				message = "",
				groups = setOf(OnDefault::class),
			),
		)
		assertThat(SchemaGroupFastPath.canSkipGroupChecks(setOf(OnDefault::class), schema)).isTrue()
	}
	
	@Test
	@DisplayName("default-only active groups do not skip when a constraint is OnCreate-only")
	fun createOnlyDoesNotSkipForDefault() {
		val schema = objectSchema(
			RequiredConstraint(
				mode = Required.Mode.STRICT,
				message = "",
				groups = setOf(OnCreate::class),
			),
		)
		assertThat(SchemaGroupFastPath.canSkipGroupChecks(setOf(OnDefault::class), schema)).isFalse()
	}
	
	@Test
	@DisplayName("OnCreate active groups skip when every constraint intersects OnCreate")
	fun createActiveSkipsWhenCompatible() {
		val schema = objectSchema(
			RequiredConstraint(
				mode = Required.Mode.STRICT,
				message = "",
				groups = setOf(OnCreate::class),
			),
		)
		assertThat(SchemaGroupFastPath.canSkipGroupChecks(setOf(OnCreate::class), schema)).isTrue()
	}
	
	@Test
	@DisplayName("empty active groups skip only when every constraint is ungrouped")
	fun emptyActiveRequiresUngrouped() {
		val ungrouped = objectSchema(
			RequiredConstraint(
				mode = Required.Mode.STRICT,
				message = "",
				groups = emptySet(),
			),
		)
		val grouped = objectSchema(
			RequiredConstraint(
				mode = Required.Mode.STRICT,
				message = "",
				groups = setOf(OnDefault::class),
			),
		)
		assertThat(SchemaGroupFastPath.canSkipGroupChecks(emptySet(), ungrouped)).isTrue()
		assertThat(SchemaGroupFastPath.canSkipGroupChecks(emptySet(), grouped)).isFalse()
	}
	
	@Test
	@DisplayName("empty schema sequence is skip-compatible")
	fun emptySchemaSequence() {
		assertThat(SchemaGroupFastPath.canSkipGroupChecks(setOf(OnDefault::class), emptySequence())).isTrue()
	}
	
	private fun objectSchema(constraint: RequiredConstraint): ObjectSchema = ObjectSchema(
		type = Sample::class.java,
		properties = listOf(
			PropertySpec(
				declaredName = "name",
				externalName = "name",
				shape = ScalarShape(ScalarKind.STRING),
				read = ValueReader { (it as Sample).name },
				constraints = listOf(
					CompiledConstraint(
						metadata = constraint,
						runner = ValidatorBackedRunner(RequiredValidator, constraint),
						order = 0,
					),
				),
			),
		),
	)
	
	private data class Sample(val name: String?)
}
