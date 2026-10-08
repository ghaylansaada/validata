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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.model.ConstraintModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [ConstraintSourceRenderer] — shared by object and endpoint emitters.
 *
 * @author Ghaylan Saada
 */
class ConstraintSourceRendererTest {

	@Test
	@DisplayName("empty constraints render as emptyList()")
	fun emptyConstraints() {
		assertThat(ConstraintSourceRenderer.renderConstraints(emptyList())).isEqualTo("emptyList()")
	}

	@Test
	@DisplayName("non-empty constraints use CompiledConstraints.of with shared metadata")
	fun rendersCompiledConstraint() {
		val source = ConstraintSourceRenderer.renderConstraints(
			listOf(
				ConstraintModel(
					metadataConstructorCall = "RequiredConstraint(\n    message = \"\",\n)",
					validatorExpression = "RequiredValidator",
					order = 0,
				),
			),
		)
		assertThat(source).contains("CompiledConstraints.of(")
		assertThat(source).contains("RequiredValidator,")
		assertThat(source).contains("RequiredConstraint(")
		assertThat(source).contains("message = \"\",")
		assertThat(source).contains("order = 0,")
		assertThat(source).doesNotContain("run {")
		assertThat(source).doesNotContain("ValidatorBackedRunner")
	}

	@Test
	@DisplayName("OR composition uses CompiledConstraints.or")
	fun rendersOrComposition() {
		val source = ConstraintSourceRenderer.renderConstraints(
			listOf(
				ConstraintModel(
					metadataConstructorCall = "",
					validatorExpression = "",
					order = 2,
					annotationSimpleName = "EmailOrPhone",
					compositionChildren = listOf(
						ConstraintModel(
							metadataConstructorCall = "FormatOkConstraint()",
							validatorExpression = "FormatOkValidator",
							order = 0,
						),
						ConstraintModel(
							metadataConstructorCall = "TokenCheckConstraint(token = \"ok\")",
							validatorExpression = "TokenCheckValidator",
							order = 1,
						),
					),
					compositionMessageExpr = "\"either\"",
					compositionGroupsExpr = "setOf(OnDefault::class)",
				),
			),
		)
		assertThat(source).contains("CompiledConstraints.or(")
		assertThat(source).contains("CompositionConstraint(")
		assertThat(source).contains("FormatOkConstraint()")
		assertThat(source).contains("TokenCheckConstraint(token = \"ok\")")
		assertThat(source).contains("message = \"either\"")
		assertThat(source).contains("CompiledConstraints.of(")
		assertThat(source).contains("order = 2,")
		assertThat(source).doesNotContain("CompositionOrRunner")
		assertThat(source).doesNotContain("run {")
	}
}
